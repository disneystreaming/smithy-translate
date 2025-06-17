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

  // checks to see if the current primitive is topLevel. This is because a newtype
  // definition may be required for top level primitives, and in nested instances
  // the default newtype definition should be used.
  def idFromPrimitive(primitive: Primitive, context: Context): (DefId, List[Hint]) = {
    if (context.hints.contains(Hint.TopLevel))
      topLevelIdFromPrimitive(primitive)
    else
      nestedIdFromPrimitive(primitive)
  }


  // format: off
  def topLevelIdFromPrimitive(primitive: Primitive): (DefId, List[Hint]) =
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
      case PDate      => std("String", Hint.Timestamp(TimestampFormat.SimpleDate))
      case PDateTime  => std("Timestamp", Hint.Timestamp(TimestampFormat.DateTime))
      case PTimestamp => std("Timestamp")
      case PLocalDate => std("String", Hint.Timestamp(TimestampFormat.LocalDate))
      case PLocalTime =>  std("String", Hint.Timestamp(TimestampFormat.LocalTime))
      case PLocalDateTime => std("String", Hint.Timestamp(TimestampFormat.LocalDateTime))
      case POffsetDateTime => std("Timestamp", Hint.Timestamp(TimestampFormat.OffsetDateTime), Hint.Timestamp(TimestampFormat.DateTime))
      case POffsetTime => std("String", Hint.Timestamp(TimestampFormat.OffsetTime))
      case PZoneId => std("String", Hint.Timestamp(TimestampFormat.ZoneId))
      case PZoneOffset => std("String", Hint.Timestamp(TimestampFormat.ZoneOffset))
      case PZonedDateTime => std("String", Hint.Timestamp(TimestampFormat.ZonedDateTime))
      case PYear => std("Integer", Hint.Timestamp(TimestampFormat.Year))
      case PYearMonth => std("String", Hint.Timestamp(TimestampFormat.YearMonth))
      case PMonthDay => std("String", Hint.Timestamp(TimestampFormat.MonthDay))
 
    }

  def nestedIdFromPrimitive(primitive: Primitive): (DefId, List[Hint]) =
    primitive match {
      case PLocalDate => alloy("LocalDate")
      case PLocalTime =>  alloy("LocalTime")
      case PLocalDateTime => alloy("LocalDateTime")
      case POffsetDateTime => alloy("OffsetDateTime")
      case POffsetTime => alloy("OffsetTime")
      case PZoneId => alloy("ZoneId")
      case PZoneOffset => alloy("ZoneOffset")
      case PZonedDateTime=> alloy("ZonedDateTime")
      case PYear => alloy("Year")
      case PYearMonth => alloy("YearMonth")
      case PMonthDay => alloy("MonthDay")
      case _ => topLevelIdFromPrimitive(primitive)
    }
  // format: on

  /** Folds one layer into a type, recording definitions into the monadic tell
    * as we go
    */
  def fold(layer: OpenApiPattern[DefId]): F[DefId] = {
    layer.context.errors.traverse(recordError) *>
      (layer match {
        case OpenApiPrimitive(context, primitive) =>
          val ntId = id(context)
          val (target, hints) = idFromPrimitive(primitive, context)
          val nt = Newtype(ntId, target, context.hints ++ hints)
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
          val (key, _) = idFromPrimitive(Primitive.PString, context)
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
