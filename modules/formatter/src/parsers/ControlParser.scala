package smithytranslate
package formatter
package parsers

import cats.parse.{Parser, Parser0}
import smithytranslate.formatter.parsers.WhitespaceParser.{br, sp0}
import smithytranslate.formatter.ast.{ControlSection, ControlStatement}
import smithytranslate.formatter.parsers.NodeParser.{
  node_object_key,
  node_value
}

object ControlParser {
  val control_statement: Parser[ControlStatement] =
    ((Parser.char('$') *> node_object_key <* sp0 ~ Parser.char(
      ':'
    ) ~ sp0) ~ node_value ~ br).map { case (tuple, break) =>
      ControlStatement(tuple._1, tuple._2, break)
    }

  val control_section: Parser0[ControlSection] =
    control_statement.rep0.map(ControlSection)

}
