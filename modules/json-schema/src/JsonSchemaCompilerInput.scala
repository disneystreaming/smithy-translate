package smithytranslate.compiler.json_schema

import cats.data.NonEmptyList
import org.everit.json.schema.Schema
import io.circe.Json
import smithytranslate.compiler.FileContents

sealed trait JsonSchemaCompilerInput

object JsonSchemaCompilerInput {
  final case class UnparsedSpecs(specs: List[FileContents])
      extends JsonSchemaCompilerInput

  final case class ParsedSpec(
      path: NonEmptyList[String],
      rawJson: Json,
      schema: Schema
  ) extends JsonSchemaCompilerInput

}
