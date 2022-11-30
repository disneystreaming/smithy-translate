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

case class Report(results: List[Validated[FormatterError, Path]]) {
  def report(): Unit = {
    val (invalidResults, validResults) = results.partitionMap { _.toEither }
    if (validResults.nonEmpty) {
      println("Successfully formatted the following files:")
      validResults.foreach { path =>
        println(s"\t${path.toNIO.getFileName.toString}")
      }
    }

    if (invalidResults.nonEmpty) {
      println("The following files were not formatted:\n")
      invalidResults.foreach(error => logError(error))
    }

    def logError(formatterError: FormatterError): Unit = formatterError match {
      case FormatterError.UnableToParse(message) =>
        println(
          s"unable to parse the Smithy file for the following reason: $message"
        )
      case FormatterError.InvalidModel(message) =>
        println(
          s"the Smithy file passed in did not pass the  Smithy model validation: $message"
        )
    }
  }

}
