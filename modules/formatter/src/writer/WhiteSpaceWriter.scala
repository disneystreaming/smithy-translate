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
      else s"${comments.writeN("\n", "\n", "")}"
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
