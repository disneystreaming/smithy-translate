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
