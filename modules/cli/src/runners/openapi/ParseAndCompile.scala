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

package smithytranslate.cli.runners.openapi

import cats.data.NonEmptyList
import smithytranslate.cli.runners.FileUtils.readAll
import smithytranslate.openapi.OpenApiCompiler
import smithytranslate.cli.transformer.TranslateTransformer
import software.amazon.smithy.model.Model
import smithytranslate.json_schema.JsonSchemaCompiler

object ParseAndCompile {
  def openapi(
      inputPaths: NonEmptyList[os.Path],
      useVerboseNames: Boolean,
      failOnInputValidationErrors: Boolean,
      failOnOutputValidationErrors: Boolean,
      transformers: List[TranslateTransformer],
      useEnumTraitSyntax: Boolean,
      debug: Boolean
  ): OpenApiCompiler.Result[Model] = {
    val includedExtensions = List("yaml", "yml", "json")
    val inputs = readAll(inputPaths, includedExtensions)
    val opts = OpenApiCompiler.Options(
      useVerboseNames,
      failOnInputValidationErrors,
      failOnOutputValidationErrors,
      transformers,
      useEnumTraitSyntax,
      debug
    )
    OpenApiCompiler.parseAndCompile(opts, inputs: _*)
  }

  def jsonSchema(
      inputPaths: NonEmptyList[os.Path],
      useVerboseNames: Boolean,
      failOnInputValidationErrors: Boolean,
      failOnOutputValidationErrors: Boolean,
      transformers: List[TranslateTransformer],
      useEnumTraitSyntax: Boolean,
      debug: Boolean
  ): OpenApiCompiler.Result[Model] = {
    val includedExtensions = List("json")
    val inputs = readAll(inputPaths, includedExtensions)
    val opts = OpenApiCompiler.Options(
      useVerboseNames,
      failOnInputValidationErrors,
      failOnOutputValidationErrors,
      transformers,
      useEnumTraitSyntax,
      debug
    )
    JsonSchemaCompiler.parseAndCompile(opts, inputs: _*)
  }

}
