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
