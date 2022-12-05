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

import smithytranslate.formatter.ast.{Break, Comment, Whitespace}
import smithytranslate.formatter.writers.Writer.WriterOpsIterable

object WhiteSpaceWriter {

  def prefixWithWhiteSpace(string: String): String = {
    s" ${string.trim}"
  }

  implicit val breakWriter: Writer[Break] = Writer.write {
    case Break(comments) =>
      if (comments.isEmpty) "\n"
      else s"${comments.writeN("\n", "\n", "\n")}"
  }
  implicit val commentWriter: Writer[Comment] = Writer.write {
    case Comment(commentType, text) =>
      s"${commentType.write}${prefixWithWhiteSpace(text)}"
  }
  implicit val wsWriter: Writer[Whitespace] =
    Writer.write(
      _.whitespace.writeN("", "\n", "")
    )

  /*
  WS = 1*(SP / NL / Comment / ",") ; whitespace

  SP =  1*(%x20 / %x09) ; one or more spaces or tabs

  NL = %x0A / %x0D.0A ; Newline:  \n and \r\n
  NotNL =  %x09 / %x20-10FFFF ; Any character except newline

    BR =  *SP 1*(Comment / NL) *WS; line break followed by whitespace

  Comment =
       DocumentationComment / LineComment

  DocumentationComment =
      "///" *NotNL NL

  LineComment =
       "//" *NotNL NL

   */

}
