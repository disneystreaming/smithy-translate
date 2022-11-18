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

import cats.data.NonEmptyList
import cats.implicits.catsSyntaxTuple2Semigroupal
import com.monovore.decline.{Command, Opts}
import smithytranslate.cli.opts.CommonOpts.osPathArg
import smithytranslate.cli.opts.SmithyTranslateCommand.Format

object FormatterOpts {
  case class FormatOpts(smithyFile: NonEmptyList[os.Path], noClobber: Boolean)
  val header = "validates and formats smithy files"

  val smithyFile: Opts[NonEmptyList[os.Path]] =
    Opts.arguments[os.Path](
      "path to Smithy file or directory containing Smithy files"
    )
  val noClobber: Opts[Boolean] = Opts
    .flag(
      "no-clobber",
      "dont overwrite existing file instead create a new file with the formatted appended"
    )
    .orFalse

  val formatOpts: Opts[FormatOpts] =
    (smithyFile, noClobber).mapN(FormatOpts.apply)

  val formatCommand: Command[FormatOpts] =
    Command(name = "format", header = header)(formatOpts)

  val format: Opts[Format] =
    Opts.subcommand(command = formatCommand).map(Format)

}
