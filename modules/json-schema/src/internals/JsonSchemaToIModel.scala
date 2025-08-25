/* Copyright 2022 Disney Streaming
 *
 * Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://disneystreaming.github.io/TOST-1.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package smithytranslate.compiler
package internals
package json_schema

import cats.Parallel
import scala.jdk.CollectionConverters._
import cats.data._
import cats.syntax.all._
import cats.Monad
import org.typelevel.ci._
import smithytranslate.compiler.internals.Suppression
import Extractors._
import io.circe.ACursor
import io.circe.JsonObject
import cats.catsParallelForId
import smithytranslate.compiler.internals.Hint.TopLevel
import smithytranslate.compiler.json_schema.CompilationUnit

private[compiler] object JsonSchemaToIModel {

  def compile(
      compilationUnit: CompilationUnit,
      namespaceRemapper: NamespaceRemapper
  ): (Chain[ToSmithyError], IModel) = {
    type ErrorLayer[A] = Writer[Chain[ToSmithyError], A]
    type WriterLayer[A] =
      WriterT[ErrorLayer, Chain[Either[Suppression, Definition]], A]
    val (errors, (data, _)) =
      compileF[WriterLayer](compilationUnit, namespaceRemapper).run.run
    val definitions = data.collect { case Right(d) => d }
    val suppressions = data.collect { case Left(s) => s }
    (errors, IModel(definitions.toVector, suppressions.toVector))
  }

  def compileF[F[_]: Parallel: TellShape: TellError](
      compilationUnit: CompilationUnit,
      namespaceRemapper: NamespaceRemapper
  ): F[Unit] = {
    val parser = new JsonSchemaToIModel[F](compilationUnit, namespaceRemapper)
    parser.recordAll
  }

}

private class JsonSchemaToIModel[F[_]: Parallel: TellShape: TellError](
    compilationUnit: CompilationUnit,
    namespaceRemapper: NamespaceRemapper
) {

  implicit val F: Monad[F] = Parallel[F].monad

  private val CaseRef =
    new Extractors.JsonSchemaCaseRefBuilder(
      Option(compilationUnit.schema.getId()),
      compilationUnit.namespace
    ) {}

  lazy val recordAll: F[Unit] =
    refoldOne(
      Local(compilationUnit.name, compilationUnit.schema, compilationUnit.json)
        .addHints(TopLevel)
    ).void

  private def fold = new PatternFolder[F](compilationUnit.namespace).fold _

  private def refoldOne(start: Local): F[DefId] = {
    // Refolding each top value under openapi's "component/schema"
    // into a type, recording definitions using Tell as we go, during
    // the collapse (fold).
    def unfoldAndAddExts(local: Local) =
      unfold(local)

    recursion.refoldPar(unfoldAndAddExts _, fold)(start)
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Unfold
  // ///////////////////////////////////////////////////////////////////////////

  /*
   * The goal here is to match one layer of "Schema" and
   * assign it to the corresponding pattern.
   *
   * We do not we try to deference anything either, as
   * that is the responsibility of a another piece of logic that checks
   * our model implementation complies with laws.
   *
   * Errors are raised when we encounter "Schema" instances that do
   * not fit in our metamodel.
   */
  def unfold(local: Local): F[OpenApiPattern[Local]] = {
    local.schema match {
      // Primitive types
      case Extractors.CasePrimitive(hints, prim) =>
        F.pure(
          OpenApiPrimitive(
            local.context.addHints(hints, retainTopLevel = true),
            prim
          )
        )

      case Extractors.CaseEnum(hints, enumValues) =>
        F.pure(
          OpenApiEnum(
            local.context.copy(hints = hints),
            enumValues
          )
        )

      case Extractors.CaseMap(hints, s) =>
        F.pure(
          OpenApiMap[Local](
            local.context.addHints(hints),
            local.down(Segment.Arbitrary(ci"Item"), s)
          )
        )

      // O:
      //   type: object
      //   properties:
      //     <at least one property>
      case Extractors.CaseObject(hints, s) =>
        // Using the topLevelJson (stored in the Local) to recover the order of properties
        val cursor: ACursor = {
          val topLevelCursor: ACursor = local.topLevelJson.hcursor
          if (s.getLocation() == null) { topLevelCursor }
          else {
            val path = s.getSchemaLocation().split('/').dropWhile(_ == "#")
            path.foldLeft(topLevelCursor) { case (current, str) =>
              str match {
                case Extractors.int(index) =>
                  current.downN(index)
                case other =>
                  current.downField(other)

              }
            }
          }
        }
        val rawProperties =
          cursor.downField("properties").as[JsonObject].toOption
        val properties =
          rawProperties match {
            case None =>
              Option(s.getPropertySchemas())
                .map(_.asScala.toVector)
                .toVector
                .flatten
            case Some(obj) =>
              val propMap = s.getPropertySchemas()
              obj.keys.map(s => s -> propMap.get(s)).toVector
          }
        val required =
          Option(s.getRequiredProperties())
            .map(_.asScala)
            .getOrElse(List.empty)
            .toSet
        val fields =
          properties.map { case (name, schema) =>
            (name, required(name)) -> local.down(
              Segment.Derived(CIString(name)),
              schema
            )
          }
        F.pure(
          OpenApiObject(
            local.context.addHints(hints, retainTopLevel = true),
            fields
          )
        )

      case Extractors.CaseArray(hints, itemSchema) =>
        F.pure(
          OpenApiArray[Local](
            local.context.addHints(hints),
            local.down(Segment.Arbitrary(ci"Item"), itemSchema)
          )
        )

      case CaseRef(idOrError) =>
        idOrError match {
          case Left(error) => F.pure(OpenApiShortStop(local.context, error))
          case Right(ref) =>
            F.pure(
              OpenApiRef(
                local.context.removeTopLevel(),
                namespaceRemapper.remap(ref.id)
              )
            )
        }

      // Special case for `type: [X, null]`
      case Extractors.CaseOneOf(hints, Vector(some, _ @CaseNull())) =>
        unfold(local.copy(schema = some).addHints(hints :+ Hint.Nullable))

      case Extractors.CaseOneOf(hints, alternatives) =>
        val unionKind = UnionKind.Untagged
        val labeledAlts = alternatives.zipWithIndex
          .map { case (s, i) =>
            local.down(
              Name(Segment.Arbitrary(ci"oneOf"), Segment.Arbitrary(ci"alt$i")),
              s
            )
          }
          .map(Nil -> _)
        F.pure(
          OpenApiOneOf(local.context.addHints(hints), labeledAlts, unionKind)
        )

      case Extractors.CaseAllOf(hints, all) =>
        F.pure(
          OpenApiAllOf(
            local.context.addHints(hints),
            all.zipWithIndex.map { case (s, i) =>
              local.down(
                Name(Segment.Arbitrary(ci"allOf"), Segment.Arbitrary(ci"$i")),
                s
              )
            }
          ).withDescription(local)
        )

      case Extractors.CaseNull() =>
        F.pure(OpenApiNull(local.context))

      case s =>
        val error = ToSmithyError.Restriction(s"Schema not supported:\n$s")
        F.pure(OpenApiShortStop(local.context, error))
    }
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Utils
  // ///////////////////////////////////////////////////////////////////////////

  private implicit class WithDescriptionSyntax(p: OpenApiPattern[Local]) {
    def withDescription(local: Local): OpenApiPattern[Local] = {
      val maybeDesc =
        Option(local.schema.getDescription()).map(Hint.Description(_)).toList
      p.mapContext(_.addHints(maybeDesc, retainTopLevel = true))
    }
  }

  def std(name: String, hints: Hint*) =
    (
      DefId(
        Namespace(List("smithy", "api")),
        Name.stdLib(name)
      ),
      hints.toList
    )
  def smithyTranslate(name: String, hints: Hint*) =
    (
      DefId(Namespace(List("smithytranslate")), Name.stdLib(name)),
      hints.toList
    )
  def alloy(name: String, hints: Hint*) =
    (
      DefId(Namespace(List("alloy")), Name.stdLib(name)),
      hints.toList
    )
}
