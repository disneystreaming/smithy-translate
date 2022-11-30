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
