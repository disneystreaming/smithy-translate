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

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import scala.jdk.CollectionConverters._
import Primitive._

private[openapi] final class SchemaReader(openApi: OpenAPI) {
  private val is31 = Option(openApi.getOpenapi).exists(_.startsWith("3.1."))
  private val scalarTypes = Set("string", "integer", "number", "boolean")
  private val supportedDialects = Set(
    "https://spec.openapis.org/oas/3.1/dialect/base",
    "https://json-schema.org/draft/2020-12/schema"
  )

  private def types(schema: Schema[_]): Set[String] = {
    if (is31) Option(schema.getTypes).fold(Set.empty[String])(_.asScala.toSet)
    else Option(schema.getType).toSet
  }

  object StringEnum {
    def unapply(schema: Schema[_]): Option[Vector[String]] =
      if (schema == null) None
      else if (!is31) CaseEnum.unapply(schema)
      else if (types(schema) == Set("string")) {
        Option(schema.getEnum).flatMap { values =>
          // The parser can infer a string type for an untyped enum containing
          // null, so require string values rather than silently dropping null.
          val strings = values.asScala.collect {
            case value: String if value.nonEmpty => value
          }.toVector
          if (strings.nonEmpty && strings.size == values.size)
            Some(strings.distinct)
          else None
        }
      } else None
  }

  // TODO: Support integer enums in both versions.
  // Their enum constraints are currently ignored.
  def unapply(schema: Schema[_]): Option[Primitive] =
    if (schema == null) None
    // These schemas belong to other conversion branches.
    else if (
      schema.get$ref != null ||
      schema.getAllOf != null || schema.getOneOf != null ||
      schema.getAnyOf != null || (is31 && schema.getNot != null)
    ) None
    else primitive(schema)

  private def primitive(schema: Schema[_]): Option[Primitive] = {
    val format = Option(schema.getFormat)
    val xFormat = Option(schema.getExtensions)
      .flatMap(extensions => Option(extensions.get("x-format")))
      .map(_.toString)
    def hasFormat(value: String): Boolean =
      format.contains(value) || xFormat.contains(value)

    types(schema).toList match {
      case "string" :: Nil =>
        // Preserve existing format/x-format precedence for compatibility.
        // TODO: Define a consistent, documented policy for conflicting
        // format and x-format values.
        format match {
          case Some("uuid")               => Some(PUUID)
          case Some("date")               => Some(PDate)
          case Some("date-time")          => Some(PDateTime)
          case Some("email" | "password") => Some(PString)
          case Some("byte" | "binary")    => if (is31) None else Some(PBytes)
          case Some("timestamp")          => Some(PTimestamp)
          case _                          =>
            // TODO: Revisit the PString fallback for unknown formats in the next
            // breaking release - numeric types return None instead as well.
            List(
              "local-date" -> PLocalDate,
              "local-time" -> PLocalTime,
              "local-date-time" -> PLocalDateTime,
              "offset-date-time" -> POffsetDateTime,
              "offset-time" -> POffsetTime,
              "zone-id" -> PZoneId,
              "zone-offset" -> PZoneOffset,
              "zoned-date-time" -> PZonedDateTime,
              "year-month" -> PYearMonth,
              "month-day" -> PMonthDay
            ).collectFirst {
              case (name, primitive) if hasFormat(name) =>
                primitive
            }.orElse(Some(PString))
        }
      case "integer" :: Nil =>
        if (hasFormat("year")) Some(PYear)
        else
          format match {
            case Some("int16")        => Some(PShort)
            case Some("int32") | None => Some(PInt)
            case Some("int64")        => Some(PLong)
            case _                    => None
          }
      case "number" :: Nil =>
        if (hasFormat("duration")) Some(PDuration)
        else
          format match {
            case Some("float")         => Some(PFloat)
            case Some("double") | None => Some(PDouble)
            case _                     => None
          }
      case "boolean" :: Nil => Some(PBoolean)
      case _                => None
    }
  }

  /** Check whether a schema is supported.
    */
  def restriction(schema: Schema[_]): Option[ToSmithyError.Restriction] = {
    // TODO: Support converting 3.0 scalar constraints such as `not` and
    // `multipleOf`, or report them as errors.
    // For now, primitive conversion ignores these constraints.
    if (!is31) None
    else if (schema == null)
      Some(ToSmithyError.Restriction("Schema not supported:\nnull"))
    // OpenAPI 3.1 handling
    else {
      val declaredTypes = types(schema)
      val isScalar =
        declaredTypes.size == 1 && declaredTypes.exists(scalarTypes)
      val unsupported = unsupportedKeywords(schema)
      val refSiblings = unsupported ++ present(
        "type" -> schema.getTypes,
        "enum" -> schema.getEnum,
        "format" -> schema.getFormat,
        "minimum" -> schema.getMinimum,
        "maximum" -> schema.getMaximum,
        "minLength" -> schema.getMinLength,
        "maxLength" -> schema.getMaxLength,
        "pattern" -> schema.getPattern,
        "description" -> schema.getDescription,
        "example" -> schema.getExample,
        "externalDocs" -> schema.getExternalDocs
      )
      val contextKeywords = present(
        s"$$schema" -> schema.get$schema,
        "$id" -> schema.get$id
      ) ++ Option(openApi.getJsonSchemaDialect)
        .filterNot(supportedDialects)
        .map(_ => "jsonSchemaDialect")
        .toList
      val keywords =
        if (contextKeywords.nonEmpty) contextKeywords
        else if (declaredTypes.size > 1 || declaredTypes.contains("null"))
          List("type (null and multiple types)")
        else if (schema.get$ref != null && refSiblings.nonEmpty)
          List("$ref siblings")
        else if (isScalar) {
          val diagnosticFormat =
            if (primitive(schema).isEmpty) List("format") else Nil
          val enumRestrictions =
            if (declaredTypes == Set("string") && schema.getEnum != null) {
              if (StringEnum.unapply(schema).isEmpty)
                List(
                  "enum (expected one or more nonempty string values)"
                )
              else if (!primitive(schema).contains(PString))
                List("enum with format or x-format")
              else Nil
            } else Nil
          unsupported ++ diagnosticFormat ++ enumRestrictions
        } else Nil

      if (keywords.isEmpty) None
      else
        Some(
          ToSmithyError.Restriction(
            s"Unsupported OpenAPI 3.1 schema keywords: ${keywords.mkString(", ")}."
          )
        )
    }
  }

  private def present(fields: (String, Any)*): List[String] =
    fields.collect { case (name, value) if value != null => name }.toList

  private def unsupportedKeywords(schema: Schema[_]): List[String] = {
    val fields = present(
      "const" -> schema.getConst,
      "multipleOf" -> schema.getMultipleOf,
      "exclusiveMinimum" -> schema.getExclusiveMinimumValue,
      "exclusiveMaximum" -> schema.getExclusiveMaximumValue,
      "exclusiveMinimum" -> schema.getExclusiveMinimum,
      "exclusiveMaximum" -> schema.getExclusiveMaximum,
      "allOf" -> schema.getAllOf,
      "anyOf" -> schema.getAnyOf,
      "oneOf" -> schema.getOneOf,
      "not" -> schema.getNot,
      "if" -> schema.getIf,
      "then" -> schema.getThen,
      "else" -> schema.getElse,
      "contentEncoding" -> schema.getContentEncoding,
      "contentMediaType" -> schema.getContentMediaType,
      "contentSchema" -> schema.getContentSchema,
      "properties" -> schema.getProperties,
      "patternProperties" -> schema.getPatternProperties,
      "additionalProperties" -> schema.getAdditionalProperties,
      "unevaluatedProperties" -> schema.getUnevaluatedProperties,
      "propertyNames" -> schema.getPropertyNames,
      "minProperties" -> schema.getMinProperties,
      "maxProperties" -> schema.getMaxProperties,
      "required" -> schema.getRequired,
      "dependentSchemas" -> schema.getDependentSchemas,
      "dependentRequired" -> schema.getDependentRequired,
      "items" -> schema.getItems,
      "prefixItems" -> schema.getPrefixItems,
      "additionalItems" -> schema.getAdditionalItems,
      "unevaluatedItems" -> schema.getUnevaluatedItems,
      "contains" -> schema.getContains,
      "minContains" -> schema.getMinContains,
      "maxContains" -> schema.getMaxContains,
      "minItems" -> schema.getMinItems,
      "maxItems" -> schema.getMaxItems,
      "uniqueItems" -> schema.getUniqueItems,
      "default" -> schema.getDefault,
      "examples" -> schema.getExamples,
      "nullable" -> schema.getNullable,
      "readOnly" -> schema.getReadOnly,
      "writeOnly" -> schema.getWriteOnly,
      "deprecated" -> schema.getDeprecated,
      "title" -> schema.getTitle,
      "xml" -> schema.getXml,
      "discriminator" -> schema.getDiscriminator,
      "$id" -> schema.get$id,
      s"$$schema" -> schema.get$schema,
      "$anchor" -> schema.get$anchor,
      "$dynamicAnchor" -> schema.get$dynamicAnchor,
      "$vocabulary" -> schema.get$vocabulary
    )
    // The parser stores unknown JSON Schema keywords alongside x- extensions.
    val unknown = Option(schema.getExtensions).toList.flatMap(
      _.keySet.asScala.filterNot(_.startsWith("x-")).toList.sorted
    )
    fields ++ unknown
  }
}
