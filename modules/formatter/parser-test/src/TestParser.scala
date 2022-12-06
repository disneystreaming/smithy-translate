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

package smithytranslate.formatter.parser_test

import com.monovore.decline._
import cats.syntax.validated._
import cats.syntax.apply._
import smithytranslate.formatter.parsers.IdlParser

object TestParserOpts {
  val printError: Opts[Boolean] =
    Opts
      .flag(
        "print-error",
        "When set, this will print the full error to stdout."
      )
      .orFalse
  val directory: Opts[os.Path] =
    Opts
      .argument[String]("file")
      .mapValidated { rawPath =>
        val osPath = os.Path(rawPath)
        if (os.exists(osPath) && os.isDir(osPath)) osPath.validNel
        else s"'$rawPath' does not exist or is not a directory.".invalidNel
      }
}

object TestParser {
  private def isSmithyFileOrDir(p: os.Path): Boolean =
    os.isFile(p) && p.lastOpt.exists(_.endsWith(".smithy"))

  def run(dir: os.Path, printErr: Boolean): Unit = {
    os.walk(dir)
      .filter(isSmithyFileOrDir)
      .map { file =>
        val content = os.read(file)
        val res = IdlParser.idlParser.parseAll(content)
        file -> res
      }
      .collect { case (file, Left(err)) =>
        file -> err
      }
      .foreach { case (file, err) =>
        if (printErr) {
          println(s"Parsing '$file' has failed with the following error:")
          println(err)
        } else {
          println(s"Parsing '$file' has failed.")
        }
      }
  }
}

object TestParserMain
    extends CommandApp(
      name = "test-parser",
      header = "Run the smithy-formatter parser against a directory",
      main = (TestParserOpts.directory, TestParserOpts.printError).mapN {
        TestParser.run
      }
    )
