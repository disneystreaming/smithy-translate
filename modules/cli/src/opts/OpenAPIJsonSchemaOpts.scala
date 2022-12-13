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

package smithytranslate.cli.opts

import com.monovore.decline.*
import cats.syntax.all.*
import cats.data.NonEmptyList
import smithytranslate.cli.opts.SmithyTranslateCommand.OpenApiTranslate

final case class OpenAPIJsonSchemaOpts(
    isOpenapi: Boolean,
    inputFiles: NonEmptyList[os.Path],
    outputPath: os.Path,
    useVerboseNames: Boolean,
    failOnValidationErrors: Boolean,
    useEnumTraitSyntax: Boolean,
    outputJson: Boolean,
    debug: Boolean
)

object OpenAPIJsonSchemaOpts {

  private val verboseNames = Opts
    .flag(
      "verboseNames",
      "If set, names of shapes not be simplified and will be as verbose as possible"
    )
    .orFalse
  private val failOnValidationErrors = Opts
    .flag(
      "failOnValidationErrors",
      "If set, abort the conversion if any specs contains a validation error"
    )
    .orFalse

  private val useEnumTraitSyntax = Opts
    .flag(
      "enumTraitSyntax",
      "output enum types with the smithy v1 enum trait (deprecated) syntax"
    )
    .orFalse

  private val outputJson = Opts
    .flag(
      "outputJson",
      "changes output format to be json representations of the smithy models"
    )
    .orFalse

  private val debug = Opts
    .flag(
      "debug",
      "print more information as when processing the inputes"
    )
    .orFalse

  private def getOpts(isOpenapi: Boolean) =
    (
      CommonOpts.sources,
      CommonOpts.outputDirectory,
      verboseNames,
      failOnValidationErrors,
      useEnumTraitSyntax,
      outputJson,
      debug
    ).mapN(
      OpenAPIJsonSchemaOpts.apply(isOpenapi, _, _, _, _, _, _, _)
    )

  private val openApiToSmithyCmd = Command(
    name = "openapi-to-smithy",
    header = "Take Open API specs as input and produce Smithy files as output."
  ) { getOpts(isOpenapi = true) }
  val openApiToSmithy =
    Opts.subcommand(openApiToSmithyCmd).map(OpenApiTranslate)

  private val jsonSchemaToSmithyCmd = Command(
    name = "json-schema-to-smithy",
    header =
      "Take Json Schema specs as input and produce Smithy files as output."
  ) { getOpts(isOpenapi = false) }
  val jsonSchemaToSmithy =
    Opts.subcommand(jsonSchemaToSmithyCmd).map(OpenApiTranslate)
}
