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

import smithytranslate.cli.opts.FormatterOpts.FormatOpts

sealed trait SmithyTranslateCommand
object SmithyTranslateCommand {
  case class Format(formatOpts: FormatOpts) extends SmithyTranslateCommand
  case class ProtoTranslate(protoOpts: ProtoOpts) extends SmithyTranslateCommand
  case class OpenApiTranslate(openAPIJsonSchemaOpts: OpenAPIJsonSchemaOpts)
      extends SmithyTranslateCommand
  case object Version extends SmithyTranslateCommand
}
