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

package smithytranslate.runners

import cats.data.{NonEmptyList, NonEmptyChain, Chain}
import smithytranslate.runners.FileUtils.readAll
import smithytranslate.runners.transformer.TransformerLookup
import smithytranslate.runners.openapi._
import smithytranslate.compiler.FileContents
import smithytranslate.compiler.openapi.OpenApiCompilerInput
import smithytranslate.compiler.json_schema.JsonSchemaCompilerInput

object OpenApi {

  private def processOpenApi(
      input: List[FileContents],
      useVerboseNames: Boolean,
      validateInput: Boolean,
      validateOutput: Boolean,
      useEnumTraitSyntax: Boolean,
      debug: Boolean
  ) = {
    val transformers = TransformerLookup.getAll()

    ParseAndCompile.openapi(
      OpenApiCompilerInput.UnparsedSpecs(input.toList),
      useVerboseNames = useVerboseNames,
      validateInput = validateInput,
      validateOutput = validateOutput,
      transformers,
      useEnumTraitSyntax,
      debug
    )
  }

  private def processJsonSchema(
      input: List[FileContents],
      useVerboseNames: Boolean,
      validateInput: Boolean,
      validateOutput: Boolean,
      useEnumTraitSyntax: Boolean,
      debug: Boolean,
      allowedRemoteBaseURLs: Set[String],
      namespaceRemaps: Map[NonEmptyChain[String], Chain[String]]
  ) = {
    val transformers = TransformerLookup.getAll()

    ParseAndCompile.jsonSchema(
      JsonSchemaCompilerInput.UnparsedSpecs(input.toList),
      useVerboseNames = useVerboseNames,
      validateInput = validateInput,
      validateOutput = validateOutput,
      transformers,
      useEnumTraitSyntax,
      debug,
      allowedRemoteBaseURLs,
      namespaceRemaps
    )
  }

  def runOpenApi(
      inputPaths: NonEmptyList[os.Path],
      outputPath: os.Path,
      useVerboseNames: Boolean,
      validateInput: Boolean,
      validateOutput: Boolean,
      useEnumTraitSyntax: Boolean,
      outputJson: Boolean,
      debug: Boolean
  ): Unit = {
    val report = ReportResult(outputPath, outputJson).apply _
    val includedExtensions = List("yaml", "yml", "json")
    val input = readAll(inputPaths, includedExtensions)

    report(
      processOpenApi(
        input,
        useVerboseNames = useVerboseNames,
        validateInput = validateInput,
        validateOutput = validateOutput,
        useEnumTraitSyntax,
        debug
      ),
      debug
    )
  }

  def transformOpenApi(
      input: List[FileContents],
      useVerboseNames: Boolean,
      validateInput: Boolean,
      validateOutput: Boolean,
      useEnumTraitSyntax: Boolean,
      outputJson: Boolean,
      debug: Boolean
  ): Either[String, Map[String, String]] = {
    ProcessResult(
      processOpenApi(
        input,
        useVerboseNames = useVerboseNames,
        validateInput = validateInput,
        validateOutput = validateOutput,
        useEnumTraitSyntax,
        debug
      ),
      outputJson
    )
  }

  def runJsonSchema(
      inputPaths: NonEmptyList[os.Path],
      outputPath: os.Path,
      useVerboseNames: Boolean,
      validateInput: Boolean,
      validateOutput: Boolean,
      useEnumTraitSyntax: Boolean,
      outputJson: Boolean,
      debug: Boolean,
      allowedRemoteBaseURLs: Set[String],
      namespaceRemaps: Map[NonEmptyChain[String], Chain[String]]
  ): Unit = {
    val report = ReportResult(outputPath, outputJson).apply _
    val includedExtensions = List("json")
    val input = readAll(inputPaths, includedExtensions)

    report(
      processJsonSchema(
        input,
        useVerboseNames = useVerboseNames,
        validateInput = validateInput,
        validateOutput = validateOutput,
        useEnumTraitSyntax,
        debug,
        allowedRemoteBaseURLs,
        namespaceRemaps
      ),
      debug
    )
  }

  def transformJsonSchema(
      input: List[FileContents],
      useVerboseNames: Boolean,
      validateInput: Boolean,
      validateOutput: Boolean,
      useEnumTraitSyntax: Boolean,
      outputJson: Boolean,
      debug: Boolean,
      allowedRemoteBaseURLs: Set[String],
      namespaceRemaps: Map[NonEmptyChain[String], Chain[String]]
  ): Either[String, Map[String, String]] = {
    ProcessResult(
      processJsonSchema(
        input,
        useVerboseNames = useVerboseNames,
        validateInput = validateInput,
        validateOutput = validateOutput,
        useEnumTraitSyntax,
        debug,
        allowedRemoteBaseURLs,
        namespaceRemaps
      ),
      outputJson
    )
  }

  def runJsonSchema(
      inputPaths: NonEmptyList[os.Path],
      outputPath: os.Path,
      useVerboseNames: Boolean,
      validateInput: Boolean,
      validateOutput: Boolean,
      useEnumTraitSyntax: Boolean,
      outputJson: Boolean,
      debug: Boolean
  ): Unit = runJsonSchema(
    inputPaths,
    outputPath,
    useVerboseNames,
    validateInput,
    validateOutput,
    useEnumTraitSyntax,
    outputJson,
    debug,
    Set.empty,
    Map.empty
  )
}
