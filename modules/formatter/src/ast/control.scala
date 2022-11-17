package smithytranslate
package formatter
package ast

import ast.NodeValue.NodeObjectKey
import ast.node_parser.{node_object_key, node_value}
import ast.whitespace_parser.{br, sp0}
import cats.parse.{Parser, Parser0}

case class ControlSection(list: List[ControlStatement])

case class ControlStatement(
    nodeObjectKey: NodeObjectKey,
    nodeValue: NodeValue,
    break: Break
)

object control_parser {
  val control_statement: Parser[ControlStatement] =
    ((Parser.char('$') *> node_object_key <* sp0 ~ Parser.char(
      ':'
    ) ~ sp0) ~ node_value ~ br).map { case (tuple, break) =>
      ControlStatement(tuple._1, tuple._2, break)
    }

  val control_section: Parser0[ControlSection] =
    control_statement.rep0.map(ControlSection)
}

/*

ControlSection =
 *(ControlStatement)

ControlStatement =
    "$" NodeObjectKey *SP ":" *SP NodeValue BR
 */
