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

import cats.Monad
import cats.Parallel
import cats.mtl._
import cats.data._
import Primitive._
import cats.syntax.all._
import org.typelevel.ci.CIString

private[compiler] final class PatternFolder[F[
    _
]: Parallel: TellShape: TellError](
    namespace: Path
) {
  implicit val F: Monad[F] = Parallel[F].monad

  def id(context: Context): DefId = {
    DefId(Namespace(namespace.toList), context.path)
  }

  def errorId(context: Context): DefId = {
    DefId(errorNamespace, context.path)
  }

  private def errorNamespace: Namespace = Namespace(List("error"))

  private def recordError(e: ToSmithyError): F[Unit] =
    Tell.tell(Chain.one(e))

  private def recordDef(definition: Definition): F[Unit] =
    Tell.tell(Chain.one(Right(definition)))

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

  // format: off
  def idFromPrimitive(primitive: Primitive): (DefId, List[Hint]) =
    primitive match {
      case PInt       => std("Integer")
      case PBoolean   => std("Boolean")
      case PString    => std("String")
      case PLong      => std("Long")
      case PByte      => std("Byte")
      case PFloat     => std("Float")
      case PDouble    => std("Double")
      case PShort     => std("Short")
      case PBytes     => std("Blob")
      case PFreeForm  => std("Document")
      case PUUID      => alloy("UUID")
      // case PYear      => alloy("Year")
      case PDate      => std("String", Hint.Timestamp(TimestampFormat.SimpleDate))
      case PDateTime  => std("Timestamp", Hint.Timestamp(TimestampFormat.DateTime))
      case PTimestamp => std("Timestamp")
      // case PLocalDate => std("String", Hint.)
      case PLocalTime =>  std("String", Hint.Timestamp(TimestampFormat.LocalTime))
      // case PLocalDateTime 
      // case POffsetTime 
      // case PZoneId 
      // case PZoneOffset
    }
    // format: on

  /** Folds one layer into a type, recording definitions into the monadic tell
    * as we go
    */
  def fold(layer: OpenApiPattern[DefId]): F[DefId] = {
    println(s"In PatternFolder.fold $layer")
    layer.context.errors.traverse(recordError) *>
      (layer match {
        case OpenApiPrimitive(context, primitive) =>
          val ntId = id(context)
          val (target, hints) = idFromPrimitive(primitive)
          val nt = Newtype(ntId, target, context.hints ++ hints)
          println(s"""processing primitive $primitive
            | $ntId => $nt 
            | context => $context
            """.stripMargin)
          recordDef(nt).as(ntId)

        case OpenApiRef(context, target) =>
          val ntId = id(context)
          val nt = Newtype(ntId, target, context.hints)
          recordDef(nt).as(ntId)

        case OpenApiEnum(context, values) =>
          val defId = id(context)
          recordDef(
            Enumeration(defId, values.filterNot(_ == null), context.hints)
          ).as(defId)

        case OpenApiNull(context) =>
          val ntId = id(context)
          val target =
            DefId(Namespace(List("smithytranslate")), Name.stdLib("Null"))
          val nt = Newtype(ntId, target, context.hints)
          recordDef(nt).as(ntId)

        case OpenApiMap(context, itemType) =>
          val defId = id(context)
          val (key, _) = idFromPrimitive(Primitive.PString)
          val definition = MapDef(defId, key, itemType, context.hints)
          recordDef(definition).as(defId)

        case OpenApiArray(context, itemType) =>
          val defId = id(context)
          recordDef(ListDef(defId, itemType, context.hints)).as(defId)

        case OpenApiSet(context, itemType) =>
          val defId = id(context)
          recordDef(SetDef(defId, itemType, context.hints)).as(defId)

        case OpenApiAllOf(context, parentTypes) =>
          val shapeId = id(context)
          F.pure(Structure(shapeId, Vector.empty, parentTypes, context.hints))
            .flatTap(recordDef)
            .as(shapeId)

        case OpenApiOneOf(context, alternatives, unionKind) =>
          val shapeId = id(context)
          alternatives
            .parTraverse { case (hints, tpe @ DefId(_, name)) =>
              val n = hints
                .collectFirst { case Hint.ContentTypeLabel(l) =>
                  StringUtil.toCamelCase(l)
                }
                .getOrElse(name.segments.last.value)
              val altId =
                MemberId(shapeId, Segment.Derived(CIString(n.toString)))
              val alt = Alternative(altId, tpe, hints)
              F.pure(alt)
            }
            .flatTap(alts =>
              recordDef(Union(shapeId, alts, unionKind, context.hints))
            )
            .as(shapeId)

        case OpenApiObject(context, items) =>
          val shapeId = id(context)
          println(s"""processing object
            | context => $context
            | items => $items
          """.stripMargin)
          items
            .map { case ((name, required), tpe) =>
              val fieldId = MemberId(shapeId, Segment.Derived(CIString(name)))
              val hints = if (required) List(Hint.Required) else List.empty
              Field(fieldId, tpe, hints)
            }
            .pure[F]
            .flatTap { fields =>
              recordDef(
                Structure(shapeId, fields, Vector.empty, context.hints)
              )
            }
            .as(shapeId)

        case OpenApiShortStop(context, error) =>
          // When an error was encountered during the unfold, we materialise it
          // as an empty shape in the `error` namespace
          val shapeId = errorId(context)
          val definition =
            Structure(
              shapeId,
              Vector.empty,
              Vector.empty,
              context.hints :+ Hint.ErrorMessage(error.getMessage)
            )
          recordError(error) *>
            recordDef(definition) *>
            F.pure(shapeId)
      })
  }
}
