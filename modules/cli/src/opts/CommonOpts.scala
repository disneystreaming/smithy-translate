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
import cats.data.ValidatedNel
import cats.data.Validated
import cats.implicits._

object CommonOpts {
  implicit val osPathArg: Argument[os.Path] = new Argument[os.Path] {
    def defaultMetavar: String = "path"
    def read(string: String): ValidatedNel[String, os.Path] =
      implicitly[Argument[java.nio.file.Path]].read(string).andThen { path =>
        try {
          if (path.isAbsolute()) Validated.validNel(os.Path(path))
          else Validated.validNel(os.pwd / os.RelPath(path))
        } catch {
          case e: Throwable =>
            Validated.invalidNel(e.getMessage() + ":" + string)
        }
      }
  }

  val sources: Opts[NonEmptyList[os.Path]] = Opts
    .options[os.Path]("input", "input source files", "i")
    .mapValidated(paths =>
      paths.traverse(path =>
        if (os.exists(path)) Validated.valid(path)
        else Validated.invalidNel(s"$path does not exist")
      )
    )

  val outputDirectory: Opts[os.Path] =
    Opts
      .argument[os.Path](metavar = "directory")
      .mapValidated(p =>
        if (os.isDir(p)) p.validNel
        else "outputDirectory has to be a directory".invalidNel
      )
}
