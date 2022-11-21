package smithytranslate
package formatter
package parsers

import cats.parse.Rfc5234.{crlf, lf, wsp}
import cats.parse.{Parser, Parser0}
import smithytranslate.formatter.ast.CommentType.{Documentation, Line}
import smithytranslate.formatter.ast.{Break, Comment, CommentType, Whitespace}

object WhitespaceParser {
  val NL: Parser[Unit] = lf | crlf
  val sp: Parser[Unit] = wsp.rep.void
  val sp0: Parser0[Unit] = wsp.rep0.void
  val COMMA: Parser[Unit] = Parser.char(',')
  // requires defer due to cyclic dependency between Break and Comment
  // deviates from ABNF due to the fact that in examples provided there can be multiple new lines in a row between shapes
  lazy val br: Parser[Break] =
    Parser
      .defer(sp.rep0.with1 *> commentOrNewline.rep <* ws)
      .map(list => Break(list.toList.flatten))
  private val not_newline: Parser0[String] = Parser.until(NL)
  val line_comment: Parser[Line] = Parser.string("//").as(Line)
  val documentation_comment: Parser[CommentType.Documentation] =
    Parser.string("///").as(Documentation)
  val commentType: Parser[CommentType] =
    documentation_comment.backtrack | line_comment
  val comment: Parser[Comment] =
    (commentType ~ not_newline <* NL).map(Comment.tupled)
  private val commentOrNewline: Parser[Option[Comment]] =
    comment.eitherOr(NL).map(_.toOption)

  val ws: Parser0[Whitespace] = sp
    .eitherOr(NL.eitherOr(comment.eitherOr(COMMA)))
    .rep0
    .map(_.flatMap(_.swap.toOption.flatMap(_.swap.toOption)))
    .map(_.flatMap(_.toOption))
    .map(Whitespace)

}
