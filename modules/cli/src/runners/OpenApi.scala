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

package smithytranslate.cli.runners

import smithytranslate.cli.opts.OpenAPIJsonSchemaOpts
import smithytranslate.cli.transformer.TransformerLookup
import smithytranslate.cli.runners.openapi._

object OpenApi {

  def runOpenApi(opts: OpenAPIJsonSchemaOpts): Unit = {
    val transformers = TransformerLookup.getAll()

    val report = ReportResult(opts.outputPath, opts.outputJson).apply _

    report(
      ParseAndCompile.openapi(
        opts.inputFiles,
        useVerboseNames = opts.useVerboseNames,
        validateInput = opts.validateInput,
        validateOutput = opts.validateOutput,
        transformers,
        opts.useEnumTraitSyntax,
        opts.debug
      ),
      opts.debug
    )
  }

  def runJsonSchema(opts: OpenAPIJsonSchemaOpts): Unit = {
    val transformers = TransformerLookup.getAll()

    val report = ReportResult(opts.outputPath, opts.outputJson).apply _
    report(
      ParseAndCompile.jsonSchema(
        opts.inputFiles,
        useVerboseNames = opts.useVerboseNames,
        validateInput = opts.validateInput,
        validateOutput = opts.validateOutput,
        transformers,
        opts.useEnumTraitSyntax,
        opts.debug
      ),
      opts.debug
    )
  }
}
