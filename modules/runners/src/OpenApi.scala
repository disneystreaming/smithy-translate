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

import cats.data.NonEmptyList
import smithytranslate.runners.transformer.TransformerLookup
import smithytranslate.runners.openapi._

object OpenApi {

  def runOpenApi(
      inputFiles: NonEmptyList[os.Path],
      outputPath: os.Path,
      useVerboseNames: Boolean,
      validateInput: Boolean,
      validateOutput: Boolean,
      useEnumTraitSyntax: Boolean,
      outputJson: Boolean,
      debug: Boolean
  ): Unit = {
    val transformers = TransformerLookup.getAll()

    val report = ReportResult(outputPath, outputJson).apply _

    report(
      ParseAndCompile.openapi(
        inputFiles,
        useVerboseNames = useVerboseNames,
        validateInput = validateInput,
        validateOutput = validateOutput,
        transformers,
        useEnumTraitSyntax,
        debug
      ),
      debug
    )
  }

  def runJsonSchema(
      inputFiles: NonEmptyList[os.Path],
      outputPath: os.Path,
      useVerboseNames: Boolean,
      validateInput: Boolean,
      validateOutput: Boolean,
      useEnumTraitSyntax: Boolean,
      outputJson: Boolean,
      debug: Boolean
  ): Unit = {
    val transformers = TransformerLookup.getAll()

    val report = ReportResult(outputPath, outputJson).apply _
    report(
      ParseAndCompile.jsonSchema(
        inputFiles,
        useVerboseNames = useVerboseNames,
        validateInput = validateInput,
        validateOutput = validateOutput,
        transformers,
        useEnumTraitSyntax,
        debug
      ),
      debug
    )
  }
}
