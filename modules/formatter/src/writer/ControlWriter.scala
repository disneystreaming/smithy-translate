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
package writers

import ast.{ControlSection, ControlStatement}
import util.string_ops.{doubleSpace, purgeIfNonText}
import NodeWriter.{nodeObjectKeyWriter, nodeValueWriter}
import WhiteSpaceWriter.breakWriter
import Writer.WriterOps

object ControlWriter {

  implicit val controlStatementWriter: Writer[ControlStatement] = Writer.write {
    case ControlStatement(nok, nv, br) =>
      s"$$${nok.write}: ${nv.write}${br.write}"
  }

  implicit val controlSectionWriter: Writer[ControlSection] = Writer.write {
    case ControlSection(list) =>
      purgeIfNonText(doubleSpace(list.map(_.write).mkString))
  }

  /*

  ControlSection =
   *(ControlStatement)

  ControlStatement =
      "$" NodeObjectKey *SP ":" *SP NodeValue BR
   */

}
