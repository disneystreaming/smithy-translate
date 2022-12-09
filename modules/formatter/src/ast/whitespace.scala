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

import smithytranslate.formatter.ast.CommentType.{Documentation, Line}

case object Comma {}
case class Whitespace(comments: List[Comment])

case class Break(comments: List[Comment])
sealed trait CommentType { self =>
  def write: String = self match {
    case Line          => "//"
    case Documentation => "///"
  }
}

object CommentType {
  case object Documentation extends CommentType
  type Documentation = Documentation.type
  case object Line extends CommentType
  type Line = Line.type
}
case class Comment(commentType: CommentType, text: String)

object Comment {
  def hasComment(br: Break): Boolean = br.comments.nonEmpty
  def hasComment(whitespace: Whitespace): Boolean =
    whitespace.comments.nonEmpty

  def whitespacesHaveComments(whitespace: Seq[Whitespace]): Boolean =
    whitespace.exists(hasComment)

  def breaksHaveComments(breaks: Seq[Break]): Boolean =
    breaks.exists(hasComment)
}
