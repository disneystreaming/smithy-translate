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
package parsers

import cats.parse.Rfc5234.{crlf, lf, wsp}
import cats.parse.{Parser, Parser0}
import smithytranslate.formatter.ast.CommentType.{Documentation, Line}
import smithytranslate.formatter.ast.{Break, Comment, CommentType, Whitespace}

object WhitespaceParser {
  val nl: Parser[Unit] = lf | crlf
  val sp: Parser[Unit] = wsp.rep.void
  val sp0: Parser0[Unit] = wsp.rep0.void
  val comma: Parser[Unit] = Parser.char(',')
  // requires defer due to cyclic dependency between Break and Comment
  // deviates from ABNF due to the fact that in examples provided there can be multiple new lines in a row between shapes
  lazy val br: Parser[Break] =
    Parser
      .defer(sp.rep0.with1 *> commentOrNewline.rep <* ws)
      .map(list => Break(list.toList.flatten))
  private val not_newline: Parser0[String] =
    Parser.until(nl).?.map(_.getOrElse(""))
  val line_comment: Parser[Line] = Parser.string("//").as(Line)
  val documentation_comment: Parser[CommentType.Documentation] =
    Parser.string("///").as(Documentation)
  val commentType: Parser[CommentType] =
    documentation_comment.backtrack | line_comment
  val comment: Parser[Comment] =
    (commentType ~ not_newline <* nl).map(Comment.apply.tupled)
  private val commentOrNewline: Parser[Option[Comment]] =
    comment.eitherOr(nl).map(_.toOption)

  val ws: Parser0[Whitespace] = sp
    .eitherOr(nl.eitherOr(comment.eitherOr(comma)))
    .rep0
    .map(_.flatMap(_.swap.toOption.flatMap(_.swap.toOption)))
    .map(_.flatMap(_.toOption))
    .map(Whitespace)

}
