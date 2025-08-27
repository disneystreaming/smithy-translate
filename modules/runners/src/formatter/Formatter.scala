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
package runners
package formatter

import cats.implicits.toBifunctorOps
import os.Path
import smithytranslate.runners.formatter.FormatterError.{
  InvalidModel,
  UnableToParse,
  UnableToReadFile
}
import smithytranslate.formatter.parsers.SmithyParserLive
import smithytranslate.formatter.writers.IdlWriter.idlWriter
import smithytranslate.formatter.writers.Writer.WriterOps
import software.amazon.smithy.model.Model

import scala.util.Try

import scala.collection.compat._

object Formatter {

  def run(
      files: List[os.Path],
      noClobber: Boolean,
      validateModel: Boolean
  ): Unit = {
    files.foreach(
      reformat(_, noClobber, validateModel).report()
    )
  }

  private def reformat(
      smithyWorkspacePath: os.Path,
      noClobber: Boolean,
      validate: Boolean
  ): Report = {

    val res: List[Either[FormatterError, Path]] = discoverFiles(
      smithyWorkspacePath
    ).map(_.flatMap { case (basePath, contents) =>
      if (validate && !validator.validate(contents)) {
        Left(
          InvalidModel(basePath)
        )
      } else {
        SmithyParserLive
          .parse(contents)
          .fold(
            message => Left(UnableToParse(basePath, message)),
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
              Right(newPath)
            }
          )
      }

    })
    res.partitionMap(identity) match {
      case (errors, formatted) => Report(formatted, errors)
    }
  }

  private def discoverFiles(
      smithyFilePath: os.Path
  ): List[Either[FormatterError, (Path, String)]] = {
    def readFile(path: os.Path): Either[FormatterError, (os.Path, String)] = {
      Try((path, os.read(path))).toEither.leftMap(t =>
        UnableToReadFile.apply(path, t.getMessage)
      )
    }

    if (os.isDir(smithyFilePath)) {
      val smithyFiles = os.walk(smithyFilePath).filter(p => p.ext == "smithy")
      smithyFiles.map { discoveredPath =>
        readFile(discoveredPath)
      }.toList
    } else {
      List(readFile(smithyFilePath))
    }

  }
}

case class FormatterError(fileName: String, errorType: FormatterErrorType)
    extends Throwable
sealed trait FormatterErrorType
object FormatterError {
  case class UnableToParse(message: String) extends FormatterErrorType
  object UnableToParse {
    def apply(fileName: os.Path, message: String): FormatterError =
      FormatterError(
        fileName.toNIO.getFileName.toString,
        UnableToParse(message)
      )
  }
  case object InvalidModel extends FormatterErrorType {
    def apply(fileName: os.Path): FormatterError =
      FormatterError(fileName.toNIO.getFileName.toString, InvalidModel)
  }

  case class UnableToReadFile(cause: String) extends FormatterErrorType
  object UnableToReadFile {
    def apply(fileName: os.Path, cause: String): FormatterError =
      FormatterError(
        fileName.toNIO.getFileName.toString,
        UnableToReadFile(cause)
      )
  }

  def invalidModel(fileName: String): FormatterError =
    FormatterError(fileName, InvalidModel)
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
