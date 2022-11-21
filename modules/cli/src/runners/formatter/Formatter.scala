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
package smithytranslate
package cli
package runners
package formatter

import cats.data.Validated
import os.Path
import smithytranslate.cli.runners.formatter.FormatterError.{
  InvalidModel,
  UnableToParse
}
import smithytranslate.formatter.parsers.SmithyParserLive
import smithytranslate.formatter.writers.IdlWriter.idlWriter
import smithytranslate.formatter.writers.Writer.WriterOps
import software.amazon.smithy.model.Model

import scala.util.Try

object Formatter {

  def reformat(
      smithyWorkspacePath: os.Path,
      noClobber: Boolean
  ): List[Validated[FormatterError, Path]] = {

    val filesAndContent: List[(Path, String)] = discoverFiles(
      smithyWorkspacePath
    )
    filesAndContent.map { case (basePath, contents) =>
      if (validator.validate(contents)) {
        SmithyParserLive
          .parse(contents)
          .fold(
            message => Validated.Invalid(UnableToParse(message)),
            idl => {
              val newPath =
                if (noClobber) {
                  val newFile = basePath
                    .getSegment(basePath.segmentCount - 1)
                    .split("\\.")
                    .mkString("_formatted.")
                  os.Path(basePath.wrapped.getParent) / s"$newFile"
                } else basePath

              os.write.over(newPath, idl.write)
              Validated.Valid(newPath)
            }
          )
      } else {
        Validated.Invalid(
          InvalidModel(basePath.toNIO.getFileName.toString)
        )
      }

    }
  }

  def discoverFiles(smithyFilePath: os.Path): List[(Path, String)] = {
    if (os.isDir(smithyFilePath)) {
      val smithyFiles = os.walk(smithyFilePath).filter(p => p.ext == "smithy")
      smithyFiles.map { discoveredPath =>
        discoveredPath -> os.read(discoveredPath)
      }.toList
    } else {
      List(smithyFilePath -> os.read(smithyFilePath))
    }
  }
}

sealed trait FormatterError extends Throwable
object FormatterError {
  case class UnableToParse(cause: String) extends FormatterError
  case class InvalidModel(fileName: String) extends FormatterError
}
object validator {
  def validate(smithy: String): Boolean = {
    Try {
      Model
        .assembler()
        .addUnparsedModel("formatter.smithy", smithy)
        .assemble()
        .validate()
        .isPresent
    }.getOrElse(false)
  }
}
