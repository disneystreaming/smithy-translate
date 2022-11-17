package scala_js

import smithytranslate.parsers.SmithyParserLive
import smithytranslate.writer.Writer.WriterOps

import scala.scalajs.js.annotation._

@JSExportTopLevel("SmithyFormatter")
object SmithyFormatter {
  @JSExport
  def format(content: String): String = {
    SmithyParserLive
      .parse(content)
      .fold(err => throw new RuntimeException(err), _.write)
  }
}
