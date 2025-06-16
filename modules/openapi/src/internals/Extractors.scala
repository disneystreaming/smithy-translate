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
package openapi

import smithytranslate.compiler._
import smithytranslate.compiler.internals._
import io.swagger.v3.oas.models.media._
import scala.jdk.CollectionConverters._
import Primitive._

private[openapi] object CaseEnum {
  def unapply(sch: Schema[_]): Option[Vector[String]] = sch match {
    case s: StringSchema if s.getEnum() != null && !s.getEnum().isEmpty =>
      Some(s.getEnum().asScala.toVector)
    case _ => None
  }
}

private[openapi] object CaseMap {
  def unapply(sch: Schema[_]): Option[Schema[_]] = {
    sch match {
      case m: MapSchema =>
        Option(m.getAdditionalProperties()).collect { case s: Schema[_] =>
          s
        }
      case _ => None
    }
  }
}

private[openapi] object IsFreeForm {
  def unapply(sch: Schema[_]): Boolean = {
    val isObjectSchema = sch.isInstanceOf[ObjectSchema]
    val hasNoProperties = Option(sch.getProperties()).forall(_.isEmpty())
    (Option(sch.getAdditionalProperties())
      .map {
        case b: java.lang.Boolean => b.booleanValue()
        case _                    => CaseMap.unapply(sch).isEmpty
      })
      .getOrElse(isObjectSchema && hasNoProperties)
  }
}

private[openapi] object Format {
  def unapply(sch: Schema[_]): Option[String] = Option(sch.getFormat())
}

private[openapi] object NoFormat {
  def unapply(sch: Schema[_]): Boolean = Option(sch.getFormat()).isEmpty
}

private[openapi] object & {
  def unapply[A](a: A): Some[(A, A)] = Some((a, a))
}

private[openapi] object CaseAllOf {
  def unapply(sch: Schema[_]): Option[Vector[Schema[_]]] = sch match {
    case composed: ComposedSchema =>
      Option(composed.getAllOf()).map(_.asScala.toVector)
    case _ => None
  }
}

private[openapi] object CaseOneOf {
  def unapply(sch: Schema[_]): Option[Vector[Schema[_]]] = sch match {
    case composed: ComposedSchema =>
      Option(composed.getOneOf()).map(_.asScala.toVector)
    case _ => None
  }
}

private[openapi] object CaseObject {
  def unapply(sch: Schema[_]): Option[Schema[_]] = sch match {
    case o: ObjectSchema => Some(o)
    case s: Schema[_] if Option(s.getProperties()).exists(_.asScala.nonEmpty) =>
      Some(s)
    case _ => None
  }
}

private[openapi] object NonEmptySegments {
  def unapply(s: String): Option[(List[String], String)] = {
    val segments = s.split("/").toList.filterNot(_.isEmpty())
    segments.lastOption.map(last => segments.dropRight(1) -> last)
  }
}

private[openapi] object CasePrimitive {
  def unapply(sch: Schema[_]): Option[Primitive] = sch match {
    // S:
    //   type: string
    //   format: timestamp
    case (_: StringSchema) & Format("timestamp") => Some(PTimestamp)

    // S:
    //   type: string
    //   format: local-date
    case (_: StringSchema) & Format("local-date") => Some(PLocalDate)

    // S:
    //   type: string
    //   format: local-time
    case (_: StringSchema) & Format("local-time") =>  Some(PLocalTime)

    // S:
    //   type: string
    //   format: local-date-time
    case (_: StringSchema) & Format("local-date-time") => Some(PLocalDateTime)

    // S:
    //   type: string
    //   format: offset-date-time
    case (_: StringSchema) & Format("offset-date-time") => Some(POffsetDateTime)

    // S:
    //   type: string
    //   format: offset-time
    case (_: StringSchema) & Format("offset-time") => Some(POffsetTime)

    // S:
    //   type: string
    //   format: zone-id
    case (_: StringSchema) & Format("zone-id") => Some(PZoneId)

    // S:
    //   type: string
    //   format: zone-offset
    case (_: StringSchema) & Format("zone-offset") => Some(PZoneOffset)

    // S:
    //   type: string
    //   format: zoned-date-time
    case (_: StringSchema) & Format("zoned-date-time") => Some(PZonedDateTime)

    // I:
    //   type: integer
    //   format: year
    // case (_: IntegerSchema) & Format("year") => Some(PYear)

    // S:
    //   type: string
    //   format: year-month
    case (_: StringSchema) & Format("year-month") => Some(PYearMonth)

    // S:
    //   type: string
    //   format: month-day
    case (_: StringSchema) & Format("month-day") => Some(PMonthDay)

    // S:
    //   type: string
    //   format: password
    case (_: PasswordSchema) => Some(PString)

    // S:
    //   type: string
    case _: StringSchema | _: EmailSchema => Some(PString)

    // S:
    //   type: string
    //   format: byte | format: binary
    case _: ByteArraySchema | _: BinarySchema => Some(PBytes)

    // S:
    //   type: string
    //   format: uuid
    case _: UUIDSchema => Some(PUUID)

    // S:
    //   type: string
    //   format: date
    case _: DateSchema => Some(PDate)

    // S:
    //   type: string
    //   format: date-time
    case _: DateTimeSchema => Some(PDateTime)

    // N:
    //   type: number
    //   format: float
    case (_: NumberSchema) & Format("float") => Some(PFloat)

    // N:
    //   type: number
    //   format: double
    case (_: NumberSchema) & Format("double") => Some(PDouble)

    // N:
    //   type: number
    case (_: NumberSchema) & NoFormat() => Some(PDouble)

    // I:
    //   type: integer
    //   format: int16 | noformat
    case (_: IntegerSchema) & (Format("int16")) => Some(PShort)

    // I:
    //   type: integer
    //   format: int32 | noformat
    case (_: IntegerSchema) & (Format("int32") | NoFormat()) => Some(PInt)

    // I:
    //   type: integer
    //   format: int64
    case (_: IntegerSchema) & Format("int64") => Some(PLong)

    // B:
    //   type: boolean
    case _: BooleanSchema => Some(PBoolean)

    case _ => None
  }
}

/*
 * The most complicated thing
 */
private[compiler] abstract class CaseRefBuilder(ns: Path)
    extends smithytranslate.compiler.internals.RefParser(ns) {
  def unapply(sch: Schema[_]): Option[Either[ToSmithyError, DefId]] =
    Option(sch.get$ref).map(this.apply)
}
