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

package smithytranslate.runners.openapi

import cats.data.NonEmptyList
import cats.data.NonEmptyChain
import smithytranslate.runners.FileUtils.readAll
import smithytranslate.runners.transformer.TranslateTransformer
import smithytranslate.compiler.openapi.OpenApiCompiler
import software.amazon.smithy.model.Model
import smithytranslate.compiler.json_schema.JsonSchemaCompiler
import smithytranslate.compiler.ToSmithyResult
import smithytranslate.compiler.ToSmithyCompilerOptions
import smithytranslate.compiler.openapi.OpenApiCompilerInput
import smithytranslate.compiler.json_schema.JsonSchemaCompilerInput
import cats.data.Chain

object ParseAndCompile {
  def openapi(
      inputPaths: NonEmptyList[os.Path],
      useVerboseNames: Boolean,
      validateInput: Boolean,
      validateOutput: Boolean,
      transformers: List[TranslateTransformer],
      useEnumTraitSyntax: Boolean,
      debug: Boolean
  ): ToSmithyResult[Model] = {
    val includedExtensions = List("yaml", "yml", "json")
    val input = OpenApiCompilerInput.UnparsedSpecs(
      readAll(inputPaths, includedExtensions)
    )
    val opts = ToSmithyCompilerOptions(
      useVerboseNames,
      validateInput,
      validateOutput,
      transformers,
      useEnumTraitSyntax,
      debug,
      Set.empty,
      Map.empty
    )
    OpenApiCompiler.compile(opts, input)
  }

  def jsonSchema(
      inputPaths: NonEmptyList[os.Path],
      useVerboseNames: Boolean,
      validateInput: Boolean,
      validateOutput: Boolean,
      transformers: List[TranslateTransformer],
      useEnumTraitSyntax: Boolean,
      debug: Boolean,
      allowedRemoteBaseURLs: Set[String],
      namespaceRemaps: Map[NonEmptyChain[String], Chain[String]]
  ): ToSmithyResult[Model] = {
    val includedExtensions = List("json")
    val input = JsonSchemaCompilerInput.UnparsedSpecs(
      readAll(inputPaths, includedExtensions)
    )
    val opts = ToSmithyCompilerOptions(
      useVerboseNames,
      validateInput,
      validateOutput,
      transformers,
      useEnumTraitSyntax,
      debug,
      allowedRemoteBaseURLs,
      namespaceRemaps
    )
    JsonSchemaCompiler.compile(opts, input)
  }

}
