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

package smithytranslate.cli

import smithytranslate.cli.opts.OpenAPIJsonSchemaOpts
import smithytranslate.cli.runners.OpenApi
import smithytranslate.cli.opts.ProtoOpts
import smithytranslate.cli.runners.Proto

object Main
    extends smithytranslate.cli.CommandApp(
      name = "smithy-translate",
      header =
        "Provides conversion commands to and from Smithy to other languages.",
      main = {
        val cli = OpenAPIJsonSchemaOpts.openApiToSmithy
          .orElse(ProtoOpts.smithyToProto)
          .orElse(OpenAPIJsonSchemaOpts.jsonSchemaToSmithy)
        cli map {
          case opts: OpenAPIJsonSchemaOpts if opts.isOpenapi =>
            OpenApi.runOpenApi(opts)
          case opts: OpenAPIJsonSchemaOpts if !opts.isOpenapi =>
            OpenApi.runJsonSchema(opts)
          case opts: ProtoOpts => Proto.runFromCli(opts)
        }
      }
    )
