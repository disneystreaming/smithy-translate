package smithytranslate.formatter.parsers

import smithytranslate.formatter.ast.Idl
import smithytranslate.formatter.parsers.IdlParser
import cats.syntax.all._

trait SmithyParser {
  def parse(input: String): Either[String, Idl]
}

object SmithyParserLive extends SmithyParser {
  override def parse(input: String): Either[String, Idl] =
    IdlParser.idlParser
      .parseAll(input)
      .leftMap(error =>
        s"failed at offset ${error.failedAtOffset} with error: ${error.show} ${error._2.map(_.context.mkString("")).toList.mkString}"
      )
}
