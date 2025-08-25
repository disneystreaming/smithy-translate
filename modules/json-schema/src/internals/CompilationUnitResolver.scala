/* Copyright 2025 Disney Streaming
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

import scala.jdk.CollectionConverters._
import cats.data.NonEmptyChain
import cats.Parallel
import scala.io.Source
import cats.Monad
import io.circe.jawn
import cats.syntax.all._
import Extractors._
import org.everit.json.schema.Schema
import cats.mtl._
import cats.data.Chain
import smithytranslate.compiler.json_schema.CompilationUnit
import scala.util.Try
import smithytranslate.compiler.ToSmithyError.OpenApiParseError
import cats.data.WriterT
import scala.util.Success
import scala.util.Failure
import smithytranslate.compiler.ToSmithyError.ProcessingError
import smithytranslate.compiler.json_schema.JsonSchemaCompilerInput
import cats.data.NonEmptyList

private[compiler] object CompilationUnitResolver {

  type TellCompilationUnit[F[_]] = Tell[F, Chain[CompilationUnit]]

  /** Resolves all remote references and returns them as CompilationUnits
    *
    * @param namespace
    *   The namespace of the json schema
    * @param schemaJson
    *   the json representation of the schema
    * @return
    *   All CompilationUnits associated with the input json schema, including
    *   remote schemas
    */
  def resolve[F[_]: Parallel](
      compilerInput: JsonSchemaCompilerInput,
      allowedRemoteBaseURLs: Set[String],
      namespaceRemapper: NamespaceRemapper
  ): F[(Chain[ToSmithyError], Chain[CompilationUnit])] = {
    implicit val F: Monad[F] = Parallel[F].monad
    type ErrorLayer[A] = WriterT[F, Chain[ToSmithyError], A]
    type WriterLayer[A] = WriterT[ErrorLayer, Chain[CompilationUnit], A]

    internalResolveRemoteReferences[WriterLayer](
      compilerInput,
      allowedRemoteBaseURLs,
      namespaceRemapper
    ).run.run
      .map { case (errors, (units, _)) => (errors, units) }
  }

  private case class UnfoldStep(
      refStack: List[String],
      namespace: Path,
      schema: Schema,
      alreadyResolvedNamespace: Set[Path]
  )

  private def internalResolveRemoteReferences[F[
      _
  ]: Parallel: TellCompilationUnit: TellError](
      compilerInput: JsonSchemaCompilerInput,
      allowedRemoteBaseURLs: Set[String],
      namespaceRemapper: NamespaceRemapper
  ): F[Unit] = {
    implicit val F: Monad[F] = Parallel[F].monad

    def recordError(e: ToSmithyError): F[Unit] =
      Tell.tell(Chain.one(e))

    def recordCompilationUnit(compUnit: CompilationUnit): F[Unit] =
      Tell.tell(Chain.one(compUnit))

    def recordAndCreateUnits(
        refStack: List[String],
        namespace: Path,
        jsonString: String,
        alreadyResolved: Set[Path]
    ): F[Vector[UnfoldStep]] = {
      if (alreadyResolved.contains(namespace)) F.pure(Vector.empty)
      else {
        jawn
          .parse(jsonString)
          .leftMap(err =>
            OpenApiParseError(
              namespace,
              List(
                err.getMessage(),
                s"Failure while processing remote refs. Ref stack: ${refStack.mkString("\n")}"
              )
            )
          )
          .flatMap(json =>
            Try(JsonSchemaOps.createCompilationUnits(namespace, json)).toEither
              .leftMap(err =>
                ProcessingError(
                  s"Failure while processing remote refs. Ref stack: ${refStack.mkString("\n")}",
                  Some(err)
                )
              )
          )
          .fold(
            recordError(_).as(Vector.empty),
            compUnits =>
              compUnits
                .traverse(compUnit =>
                  recordCompilationUnit(compUnit).as(compUnit)
                )
                .map(
                  _.map(compUnit =>
                    UnfoldStep(
                      refStack,
                      compUnit.namespace,
                      compUnit.schema,
                      alreadyResolved + compUnit.namespace
                    )
                  )
                )
          )
      }
    }

    // Matches recursive schema nodes and ref nodes.
    // When a recursive node is encountered, the sub-schema(s) are returned such that the search for remote refs continues recursively.
    // When a remote ref is encountered, the remote schema is pulled, parsed, recorded for use in compilation, and then the process continues.
    // When a terminal node is encountered, an empty node is returned signaling the end of that branch of the search
    def unfold(input: UnfoldStep): F[Vector[UnfoldStep]] = {
      val UnfoldStep(refStack, namespace, schema, alreadyResolvedNamespaces) =
        input

      val CaseRef = new JsonSchemaCaseRefBuilder(
        Option(schema.getId()),
        namespace,
        namespaceRemapper
      ) {}

      schema match {

        // Remote ref found. Pull in the associated schema
        case CaseRef(Right(ParsedRef.Remote(uri, id)))
            if allowedRemoteBaseURLs.exists(uri.toString.startsWith(_)) =>
          val ns =
            NonEmptyChain.fromChainUnsafe(Chain.fromSeq(id.namespace.segments))

          Try(
            // TODO: Use some library that leverages effects to fetch the content from the URL
            Source
              .fromURL(uri.toURL())
              .getLines()
              .mkString(System.lineSeparator())
          ) match {
            case Failure(err) =>
              recordError(
                ToSmithyError.HttpError(uri, uri.toString :: refStack, err)
              )
                .as(Vector.empty)

            case Success(content) =>
              recordAndCreateUnits(
                uri.toString :: refStack,
                ns,
                content,
                alreadyResolvedNamespaces
              )
          }

        // Recursive node, return the sub-schema and its namespace
        // so we can continue searching for remote refs
        case CaseArray(_, arrSchema) =>
          F.pure(
            Vector(
              UnfoldStep(
                "#array" :: refStack,
                namespace,
                arrSchema,
                alreadyResolvedNamespaces
              )
            )
          )
        case CaseMap(_, mapSchema) =>
          F.pure(
            Vector(
              UnfoldStep(
                "#additionalProperties" :: refStack,
                namespace,
                mapSchema,
                alreadyResolvedNamespaces
              )
            )
          )
        case CaseObject(_, objSchema) =>
          F.pure(
            objSchema
              .getPropertySchemas()
              .asScala
              .toVector
              .map { case (fieldName, schema) =>
                UnfoldStep(
                  s".$fieldName" :: refStack,
                  namespace,
                  schema,
                  alreadyResolvedNamespaces
                )
              }
          )
        case CaseOneOf(_, oneOfSchemas) =>
          F.pure(
            oneOfSchemas.toVector
              .map(
                UnfoldStep(
                  "#oneOf" :: refStack,
                  namespace,
                  _,
                  alreadyResolvedNamespaces
                )
              )
          )
        case CaseAllOf(_, allOfSchemas) =>
          F.pure(
            allOfSchemas.toVector
              .map(
                UnfoldStep(
                  "#allOf" :: refStack,
                  namespace,
                  _,
                  alreadyResolvedNamespaces
                )
              )
          )

        // Nonrecursive node, terminal case. Return nothing
        case _ =>
          F.pure(Vector.empty)
      }
    }

    def getRemappedNamespaceFromPath(
        path: NonEmptyList[String]
    ): F[Option[NonEmptyList[String]]] = {
      val lastSplit = path.last.split('.')
      val newLast =
        if (lastSplit.size > 1) lastSplit.dropRight(1)
        else lastSplit

      val pathWithNoFileExtension =
        path.toList.dropRight(1) :+ newLast.mkString(".")

      val remappedPath = namespaceRemapper.remap(pathWithNoFileExtension)

      NonEmptyList.fromList(remappedPath) match {
        case None =>
          recordError(
            ProcessingError(
              s"Failed to remap namespace for file with path '${path.toList.mkString("/")}'"
            )
          ).as(None)
        case s @ Some(_) => F.pure(s)
      }
    }

    val inputs: F[Vector[UnfoldStep]] = {
      val pathAndContent =
        compilerInput match {
          case JsonSchemaCompilerInput.UnparsedSpecs(specs) =>
            specs.toVector.map { case FileContents(path, content) =>
              (path, content)
            }
          case JsonSchemaCompilerInput.ParsedSpec(path, rawJson, _) =>
            Vector((path, rawJson.noSpaces))
        }

      pathAndContent.parFoldMapA { case (path, content) =>
        getRemappedNamespaceFromPath(path).flatMap {
          case None => Vector.empty[UnfoldStep].pure[F]
          case Some(ns) =>
            recordAndCreateUnits(
              List(ns.toList.mkString("/")),
              NonEmptyChain.fromNonEmptyList(ns),
              content,
              Set.empty
            )
        }
      }
    }

    val preResolvedInputs =
      for {
        allInputs <- inputs
        preResolved = allInputs.foldMap(_.alreadyResolvedNamespace)
      } yield allInputs.map(step =>
        step.copy(alreadyResolvedNamespace = preResolved)
      )

    preResolvedInputs
      .flatMap(_.flatTraverse(recursion.unfoldPar(unfold)(_)))
      .void
  }

}
