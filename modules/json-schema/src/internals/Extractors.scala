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

import Primitive._
import org.everit.json.schema.Schema
import org.everit.json.schema._
import cats.data.NonEmptyChain
import smithytranslate.compiler.ToSmithyError
import smithytranslate.compiler.internals.GetExtensions
import scala.jdk.CollectionConverters._
import cats.syntax.all._
import scala.collection.compat._

private[json_schema] object Extractors {

  type Path = NonEmptyChain[String]

  object CaseObject {
    def unapply(sch: Schema): Option[(List[Hint], ObjectSchema)] = sch match {
      case o: ObjectSchema =>
        Some(getGenericHints(sch) -> o)
      case _ => None
    }
  }

  object CaseEnum {

    // returns the EnumSchema if the only two schemas in the vector are an
    // enum and a string type. This indicates that it is an enum of type
    // string.
    private def onlyEnumAndStringType(v: Vector[Schema]): Option[EnumSchema] = {
      v.toList match {
        case (first: EnumSchema) :: (_: StringSchema) :: Nil  => Some(first)
        case (_: StringSchema) :: (second: EnumSchema) :: Nil => Some(second)
        case _                                                => None
      }
    }

    private def getValues(e: EnumSchema): Vector[String] = {
      e.getPossibleValuesAsList.asScala.collect { case s: String =>
        s
      }.toVector
    }

    def unapply(sch: Schema): Option[(List[Hint], Vector[String])] = sch match {
      case e: EnumSchema =>
        val enumValues = getValues(e)
        Some(getGenericHints(sch) -> enumValues)
      case CaseAllOf(_, schemas) =>
        onlyEnumAndStringType(schemas) match {
          case None    => None
          case Some(e) => Some(getGenericHints(sch) -> getValues(e))
        }
      case _ => None
    }
  }

  object CasePrimitive {
    def unapply(sch: Schema): Option[(List[Hint], Primitive)] = {
      val desc = Option(sch.getDescription()).map(Hint.Description(_))
      val genericHints = getGenericHints(sch)
      val specific: Option[(List[Hint], Primitive)] = sch match {
        // N:
        //   type: number
        case (s: NumberSchema) =>
          val prim =
            if (s.requiresInteger()) Some(PInt)
            else Some(PDouble)

          val (typedMin, typedMax): (Double, Double) = prim match {
            case Some(PInt) => (Int.MinValue.toDouble, Int.MaxValue.toDouble)
            case _          => (Double.MinValue, Double.MaxValue)
          }

          val max = Option(s.getMaximum())
            .map(_.doubleValue())
            .map(_.min(typedMax))
            .map(BigDecimal(_))

          val min = Option(s.getMinimum())
            .map(_.doubleValue())
            .map(_.max(typedMin))
            .map(BigDecimal(_))

          val range =
            if (max.nonEmpty || min.nonEmpty) Some(Hint.Range(min, max))
            else None
          val hints: List[Hint] = List(desc, range).flatten

          prim.map(hints -> _)

        // S:
        //  type: string
        //  format: date-time
        case (_: StringSchema) & Format("date-time") =>
          Some(List.empty -> PDateTime)

        // S:
        //  type: string
        //  format: local-date
        case (_: StringSchema) & Format("local-date") =>
          Some(List.empty -> PLocalDate)

        // S:
        //  type: string
        //  format: local-time
        case (_: StringSchema) & Format("local-time") =>
          Some(List.empty -> PLocalTime)

        // S:
        //  type: string
        //  format: local-date-time
        case (_: StringSchema) & Format("local-date-time") =>
          Some(List.empty -> PLocalDateTime)

        // S:
        //  type: string
        //  format: offset-date-time
        case (_: StringSchema) & Format("offset-date-time") =>
          Some(List.empty -> POffsetDateTime)

        // S:
        //  type: string
        //  format: offset-time
        case (_: StringSchema) & Format("offset-time") =>
          Some(List.empty -> POffsetTime)

        // S:
        //  type: string
        //  format: zone-id
        case (_: StringSchema) & Format("zone-id") =>
          Some(List.empty -> PZoneId)

        // S:
        //  type: string
        //  format: zone-offset
        case (_: StringSchema) & Format("zone-offset") =>
          Some(List.empty -> PZoneOffset)

        // S:
        //  type: string
        //  format: zoned-date-time
        case (_: StringSchema) & Format("zoned-date-time") =>
          Some(List.empty -> PZonedDateTime)

        // I:
        //  type: integer
        //  format: year
        // I:
        //  type: number
        //  format: duration
        //  The json schema parser treats any integer type with a format fields as a CombinedSchema of StringSchema and NumberSchema
        //  where the StringSchema has the format set
        case (combinedSchema: CombinedSchema) => {
          val subschemas = combinedSchema.getSubschemas().asScala.toSet

          for {
            _ <- subschemas.collectFirst { case (x: NumberSchema) => Some(x) }
            format <- subschemas.collectFirst {
              case Format("year")     => PYear
              case Format("duration") => PDuration
            }
          } yield List.empty -> format
        }

        // S:
        //  type: string
        //  format: year-month
        case (_: StringSchema) & Format("year-month") =>
          Some(List.empty -> PYearMonth)

        // S:
        //  type: string
        //  format: month-day
        case (_: StringSchema) & Format("month-day") =>
          Some(List.empty -> PMonthDay)

        // S:
        //  type: string
        //  format: date
        case (_: StringSchema) & Format("date") =>
          Some(List.empty -> PDate)

        // S:
        //  type: string
        //  format: uuid
        case (_: StringSchema) & Format("uuid") =>
          Some(List.empty -> PUUID)

        // S:
        //  type: string
        case (s: StringSchema) =>
          val pattern =
            Option(s.getPattern()).map(_.pattern()).map(Hint.Pattern(_))
          val max = Option(s.getMaxLength()).map(n => n.longValue())
          val min = Option(s.getMinLength()).map(n => n.longValue())
          val length =
            if (max.nonEmpty || min.nonEmpty) Some(Hint.Length(min, max))
            else None
          val format = Format.unapply(s).flatMap {
            case "password" => Some(Hint.Sensitive)
            case _          => None
          }
          val hints: List[Hint] =
            List(desc, length, pattern, format).flatten

          Some(hints -> PString)

        // B:
        //   type: boolean
        case b: BooleanSchema =>
          val desc = Option(b.getDescription()).map(Hint.Description(_))
          val hints = desc.toList
          Some(hints -> PBoolean)

        // M:
        //   type: object
        //   additionalProperties: true
        case (obj: ObjectSchema)
            if obj.getPropertySchemas().size() == 0 &&
              obj.permitsAdditionalProperties() &&
              CaseMap.unapply(obj).isEmpty =>
          val genericHints = getGenericHints(obj)
          Some(genericHints -> PFreeForm)

        // Empty schema in JsonSchema means it can be anything (Document)
        case _: EmptySchema => Some(genericHints -> PFreeForm)

        case _ => None
      }

      specific.map(_.leftMap(_ ++ genericHints))
    }
  }

  object CaseArray {
    def unapply(sch: Schema): Option[(List[Hint], Schema)] = sch match {
      case (arr: ArraySchema) =>
        val max = Option(arr.getMaxItems()).map(n => n.longValue())
        val min = Option(arr.getMinItems()).map(n => n.longValue())
        val length =
          if (max.nonEmpty || min.nonEmpty) Some(Hint.Length(min, max))
          else None
        val unique =
          if (arr.needsUniqueItems()) Some(Hint.UniqueItems) else None
        val genericHints = getGenericHints(sch)
        val hints = List(length, unique).flatten ++ genericHints

        Option(arr.getAllItemSchema()).map(hints -> _)
      case _ => None
    }
  }

  object CaseMap {
    @annotation.nowarn(
      "msg=method getPatternProperties in class ObjectSchema is deprecated"
    )
    def unapply(sch: Schema): Option[(List[Hint], Schema)] = sch match {
      // See http://json-schema.org/draft/2020-12/json-schema-core.html#name-patternproperties
      // This is really not easy to represent in a well-typed manner, so we're just accepting
      // schemas that have a single pattern.
      case (obj: ObjectSchema) if obj.getPatternProperties.asScala.size == 1 =>
        val genericHints = getGenericHints(sch)
        Option(obj.getPatternProperties().asScala.head._2)
          .map(genericHints -> _)
      case (obj: ObjectSchema)
          if obj.getPropertySchemas().size() == 0 && obj
            .getSchemaOfAdditionalProperties() != null =>
        val genericHints = getGenericHints(sch)
        Some(genericHints -> obj.getSchemaOfAdditionalProperties())
      case _ => None
    }
  }

  object CaseNull {
    def unapply(sch: Schema): Boolean = sch match {
      case _: NullSchema => true
      case _             => false
    }
  }

  object CaseOneOf {
    def unapply(sch: Schema): Option[(List[Hint], Vector[Schema])] = {
      val genericHints = getGenericHints(sch)
      sch match {
        case combined: CombinedSchema =>
          Option(combined.getCriterion()).map(_.toString()).flatMap {
            case "oneOf" =>
              Some(genericHints -> combined.getSubschemas().asScala.toVector)
            case "anyOf" =>
              Some(genericHints -> combined.getSubschemas().asScala.toVector)
            case _ => None
          }
        case _ => None
      }
    }
  }

  object CaseAllOf {
    def unapply(sch: Schema): Option[(List[Hint], Vector[Schema])] = {
      val genericHints = getGenericHints(sch)
      sch match {
        case combined: CombinedSchema =>
          Option(combined.getCriterion()).map(_.toString()).flatMap {
            case "allOf" =>
              Some(genericHints -> combined.getSubschemas().asScala.toVector)
            case _ => None
          }
        case _ => None
      }
    }
  }

  /*
   * The most complicated thing
   */
  abstract class JsonSchemaCaseRefBuilder(id: Option[String], ns: Path)
      extends smithytranslate.compiler.internals.RefParser(ns) {
    def unapply(sch: Schema): Option[Either[ToSmithyError, DefId]] = sch match {
      case ref: ReferenceSchema =>
        // For some reason the reference seems to get prefixed by the id every now and then
        val refValue = ref.getReferenceValue()
        // Sometimes, when the id starts with `file://` it actually starts with more than two `/` chars.
        // The number of `/` chars is not always consistent between the id and the refValue.
        val fileRegex = "^file:\\/*"
        val refValueNoPrefix = refValue.replaceFirst(fileRegex, "")
        val sanitisedRefValue =
          id.map(_.replaceFirst(fileRegex, ""))
            .collectFirst {
              case idNoPrefix if (refValueNoPrefix.startsWith(idNoPrefix)) =>
                refValueNoPrefix.drop(idNoPrefix.length())
            }
            .getOrElse(refValue)

        Option(sanitisedRefValue).map(this.apply)
      case _ => None
    }

  }

  private class SchemaHasExtensions(schema: Schema) {
    def getExtensions(): java.util.Map[String, Any] =
      Option(schema.getUnprocessedProperties())
        .map { _.asScala.view.filterKeys(_.startsWith("x-")).toMap }
        .getOrElse(Map.empty[String, Any])
        .asJava
  }

  def getGenericHints(schema: Schema): List[Hint] = {
    val current = Option(schema.getLocation())
      .map(_.toString())
      .map(Hint.CurrentLocation(_))
    val target = Option(schema.getSchemaLocation())
      .map(Hint.TargetLocation(_))
    val description = Option(schema.getDescription()).map(Hint.Description(_))
    val extensions = GetExtensions.from(new SchemaHasExtensions(schema))
    val default = Option(schema.getDefaultValue)
      .map {
        case j: org.json.JSONArray    => j.toList()
        case org.json.JSONObject.NULL => null
        case other                    => other
      }
      .map(GetExtensions.anyToNode)
      .map(Hint.DefaultValue(_))
    List(description, default, current, target).flatten ++ extensions
  }

  object Format {
    def unapply(sch: Schema): Option[String] = sch match {
      case s: StringSchema => Option(s.getFormatValidator()).map(_.formatName())
      case _               => None
    }
  }

  object & {
    def unapply[A](a: A): Some[(A, A)] = Some((a, a))
  }

  object int {
    def unapply(str: String): Option[Int] = str.toIntOption
  }

}
