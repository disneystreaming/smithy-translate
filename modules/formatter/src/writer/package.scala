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

import ast.{NodeValue, Whitespace}
import smithytranslate.formatter.writers.NodeWriter.{
  nodeObjectKeyWriter,
  nodeValueWriter
}
import smithytranslate.formatter.writers.WhiteSpaceWriter.wsWriter
import smithytranslate.formatter.writers.Writer.WriterOps

package object writers {
  val traitKeyValueLimitLength = 80

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
