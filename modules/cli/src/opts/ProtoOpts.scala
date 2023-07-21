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

import com.monovore.decline._
import cats.data.NonEmptyList
import cats.syntax.all._
import smithytranslate.cli.opts.SmithyTranslateCommand.ProtoTranslate

case class ProtoOpts(
    inputFiles: NonEmptyList[os.Path],
    outputPath: os.Path,
    deps: List[String],
    repositories: List[String],
    force: Boolean
)

object ProtoOpts {
  private val deps: Opts[List[String]] =
    Opts
      .options[String](
        long = "dependency",
        help = "Dependencies that contains Smithy definitions."
      )
      .orEmpty

  private val repositories: Opts[List[String]] =
    Opts
      .options[String](
        long = "repository",
        help = "Specify repositories to fetch dependencies from."
      )
      .orEmpty

  private val force: Opts[Boolean] = Opts
    .flag(
      "force",
      help = "Force overwrite smithy-build.json file if it's present"
    )
    .orFalse

  private val opts =
    (CommonOpts.sources, CommonOpts.outputDirectory, deps, repositories, force)
      .mapN(ProtoOpts.apply)

  private val smithyToProtoCmd = Command(
    name = "smithy-to-proto",
    header =
      "Take Smithy definitions as input and produce Proto files as output."
  ) { opts }
  val smithyToProto = Opts.subcommand(smithyToProtoCmd).map(ProtoTranslate)
}
