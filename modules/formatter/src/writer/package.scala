package smithytranslate
package formatter

import ast.{NodeValue, Whitespace}
import smithytranslate.formatter.writers.NodeWriter.{
  nodeObjectKeyWriter,
  nodeValueWriter
}
import smithytranslate.formatter.writers.WhiteSpaceWriter.wsWriter
import smithytranslate.formatter.writers.Writer.WriterOps

package object writers {
  def showKeyValue(
      nodeObjectKey: NodeValue.NodeObjectKey,
      ws0: Whitespace,
      ws1: Whitespace,
      nodeValue: NodeValue
  ) = {
    s"${nodeObjectKey.write}${ws0.write}: ${ws1.write}${nodeValue.write}"
  }

  implicit class IterableOps[A: Writer](val list: IterableOnce[A]) {
    def mkString_(prefix: String, delimiter: String, suffix: String): String = {
      val res = list.iterator
        .foldLeft(("", true)) { case ((acc, isFirst), line) =>
          val written = Writer[A].write(line)
          if (isFirst) {
            (prefix + written, false)
          } else {
            (acc + delimiter + written, isFirst)
          }
        }
        ._1
      if (res.nonEmpty) res + suffix else res
    }
  }
}
