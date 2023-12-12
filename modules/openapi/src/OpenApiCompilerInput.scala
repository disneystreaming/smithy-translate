package smithytranslate.compiler.openapi

import cats.data.NonEmptyList
import io.swagger.v3.oas.models.OpenAPI
import smithytranslate.compiler.FileContents

sealed trait OpenApiCompilerInput

object OpenApiCompilerInput {
  case class UnparsedSpecs(files: List[FileContents])
      extends OpenApiCompilerInput

  case class ParsedSpec(path: NonEmptyList[String], openapiModel: OpenAPI)
      extends OpenApiCompilerInput

}
