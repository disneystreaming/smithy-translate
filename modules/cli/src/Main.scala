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

import com.monovore.decline.Opts
import smithytranslate.cli.internal.BuildInfo
import smithytranslate.cli.opts.{
  FormatterOpts,
  OpenAPIJsonSchemaOpts,
  ProtoOpts,
  SmithyTranslateCommand,
  VersionOpts
}
import smithytranslate.cli.opts.SmithyTranslateCommand.{
  Format,
  OpenApiTranslate,
  ProtoTranslate,
  Version
}
import smithytranslate.cli.runners.{OpenApi, Proto}
import smithytranslate.cli.runners.formatter.Formatter

object Main
    extends smithytranslate.cli.CommandApp(
      name = "smithytranslate",
      header =
        "utils to convert  To and From Smithy to other Languages and to format Smithy files.",
      main = {
        val cli: Opts[SmithyTranslateCommand] =
          OpenAPIJsonSchemaOpts.openApiToSmithy
            .orElse(ProtoOpts.smithyToProto)
            .orElse(OpenAPIJsonSchemaOpts.jsonSchemaToSmithy)
            .orElse(FormatterOpts.format)
            .orElse(VersionOpts.print)
        cli map {
          case OpenApiTranslate(opts) =>
            if (opts.isOpenapi)
              OpenApi.runOpenApi(opts)
            else
              OpenApi.runJsonSchema(opts)
            SmithyBuildJsonWriter.writeDefault(opts.outputPath, opts.force)

          case ProtoTranslate(opts) =>
            Proto.runFromCli(opts)

          case Format(opts) => Formatter.run(opts)

          case Version => println(BuildInfo.cliVersion)
        }
      }
    )
