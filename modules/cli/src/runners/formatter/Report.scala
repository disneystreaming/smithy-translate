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

import os.Path
import smithytranslate.cli.runners.formatter.FormatterError.{
  InvalidModel,
  UnableToParse,
  UnableToReadFile
}
import smithytranslate.cli.runners.formatter.Report.logError

case class Report(success: List[Path], errors: List[FormatterError]) {
  override def toString: String = {
    val formatReport = if (success.nonEmpty) {
      "Successfully formatted the following files:\n" +
        success
          .map { path => s"\t${path.toNIO.getFileName.toString}" }
          .mkString("\n")
    } else {
      "No files were formatted"
    }

    val errorReport = if (errors.nonEmpty) {
      "\nThe following files were not formatted:\n\n" + errors
        .map(_.fileName)
        .mkString("\n") + "\nfor the following reasons\n" + errors
        .map(error => logError(error))
        .mkString("\n")
    } else ""

    formatReport + "\n" + errorReport
  }

  def report(): Unit = {
    println(toString())
  }

}
object Report {
  def success(success: List[Path]): Report = new Report(success, List.empty)
  def error(error: FormatterError): Report = new Report(List.empty, List(error))

  def logError(formatterError: FormatterError): String = formatterError match {
    case FormatterError(fileName, UnableToParse(message)) =>
      s"unable to parse the Smithy file: $fileName for the following reason: $message"

    case FormatterError(fileName, UnableToReadFile(cause)) =>
      s"unable to read the Smithy file $fileName for the following reason: $cause"

    case FormatterError(fileName, InvalidModel) =>
      s"unable to validate the Smithy model  in the file $fileName"
  }
}
