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

import ast.NodeValue.{
  NodeArray,
  NodeArrayValue,
  NodeKeyword,
  NodeObject,
  NodeObjectKey,
  NodeObjectKeyValuePair,
  NodeStringValue,
  SmithyNumber
}
import ast.{Comment, EscapedChar, NodeValue, QuotedChar, QuotedText, TextBlock}
import ast.EscapedChar.{CharCase, UnicodeEscape}
import ast.NodeValue.NodeStringValue.{
  QuotedTextCase,
  ShapeIdCase,
  TextBlockCase
}
import ast.NodeValue.Number.{Exp, Frac}
import ast.QuotedChar.{EscapedCharCase, NewLineCase, SimpleCharCase}
import util.string_ops.{addBrackets, indent, isTooWide}
import ShapeIdWriter.{identifierWriter, shapeIdWriter}
import WhiteSpaceWriter.wsWriter
import Writer.{WriterOps, WriterOpsIterable}

object NodeWriter {
  implicit val nodeArrayValueWriter: Writer[NodeArrayValue] =
    Writer.write { case NodeArrayValue(value, ws0) =>
      s"${value.write}\n${ws0.write}"
    }

  implicit val nodeArrayWriter: Writer[NodeArray] = Writer.write {
    case NodeArray(ws, Nil) => s"[${ws.write}]"
    case NodeArray(ws, elems) =>
      val comments = ws +: elems.map(_.ws0)
      val useNewLine =
        Comment.whitespacesHaveComments(comments) || isTooWide(elems)
      if (useNewLine) {
        val indentedComment =
          if (Comment.hasComment(ws)) indent(ws.write, "\n", 4)
          else ""
        elems
          .map(_.write)
          .map(indent(_, "\n", 4))
          .mkString_(s"[\n$indentedComment\n", "\n", "\n]")
      } else {
        // no comment, just print the value
        elems.map(_.value.write).mkString_(s"[${ws.write}", ", ", "]")
      }
  }

  implicit val nodeObjectKvpWriter: Writer[NodeObjectKeyValuePair] =
    Writer.write { case NodeObjectKeyValuePair(key, ws0, ws1, value) =>
      showKeyValue(key, ws0, ws1, value)
    }

  implicit val nodeObjectWriter: Writer[NodeObject] = Writer.write {
    case NodeObject(ws, None) => addBrackets(ws.write)
    case NodeObject(ws, Some((nokvp, Nil))) =>
      addBrackets(indent(s"${ws.write}${nokvp.write}", "\n", 4))
    case NodeObject(ws, Some((nokvp, rest))) =>
      addBrackets(
        ws.write + indent(
          s"${ws.write}${nokvp.write}${rest.map { case (ws, kvp) =>
              s"\n${ws.write}${kvp.write}"
            }.mkString}",
          "\n",
          4
        )
      )
  }

  implicit val nodeValueWriter: Writer[NodeValue] = Writer.write {
    case array: NodeArray       => array.write
    case nodeObject: NodeObject => nodeObject.write
    case number: SmithyNumber   => smithyNumberWriter(number)
    case NodeKeyword(value)     => value
    case value: NodeStringValue =>
      nodeStringValueWriter(value)
  }

  def nodeStringValueWriter(value: NodeStringValue): String = {
    value match {
      case ShapeIdCase(shapeId)       => shapeId.write
      case TextBlockCase(textBlock)   => textBlock.write
      case QuotedTextCase(quotedText) => quotedText.write
    }
  }

  def smithyNumberWriter(number: SmithyNumber): String = {
    val sign = number.minus.map(_.toString).getOrElse("")
    val int = number.smithyInt.write
    val frac = number.frac.map(_.write).getOrElse("")
    val exp = number.exp.map(_.write).getOrElse("")
    s"$sign$int${frac}${exp}"
  }

  implicit val nodeObjectKeyWriter: Writer[NodeObjectKey] = Writer.write {
    case NodeObjectKey.QuotedTextNok(text)       => text.write
    case NodeObjectKey.IdentifierNok(identifier) => identifier.write
  }
  implicit val escapeCarWriter: Writer[EscapedChar] = Writer.write {
    case CharCase(char) => s"\\${char.write}"
    case UnicodeEscape(hex, hex2, hex3) =>
      s"\\u${hex.write}${hex2.write}${hex3.write}"
  }
  implicit val quotedCharWriter: Writer[QuotedChar] = Writer.write {
    case SimpleCharCase(char)         => s"${char.write}"
    case EscapedCharCase(escapedChar) => escapedChar.write
    case NewLineCase                  => "\n"
  }
  implicit val quotedTextWriter: Writer[QuotedText] = Writer.write {
    case QuotedText(text) =>
      s"\"${text.map(_.write).mkString}\""
  }
  implicit val textBlockWriter: Writer[TextBlock] = Writer.write {
    case TextBlock(text) =>
      s"\"\"\"\n${text.writeN}\"\"\""
  }
  implicit val fracWriter: Writer[Frac] = Writer.write { case Frac(frac) =>
    s".${frac.write}"
  }
  implicit val expWriter: Writer[Exp] = Writer.write {
    case Exp(symbol, op, digits) =>
      s"${symbol.write}${op.write}${digits.write}"
  }

  /*
  NodeValue =
      NodeArray
    / NodeObject
    / Number
    / NodeKeywords
    / NodeStringValue

  NodeArray =
      "[" *WS *(NodeValue *WS) "]"

  NodeObject =
      "{" *WS [NodeObjectKvp *(WS NodeObjectKvp)] *WS "}"

  NodeObjectKvp =
      NodeObjectKey *WS ":" *WS NodeValue

  NodeObjectKey =
      QuotedText / Identifier

  Number =
      [Minus] Int [Frac] [Exp]

  DecimalPoint =
      %x2E ; .

  DigitOneToNine =
      %x31-39 ; 1-9

  E =
      %x65 / %x45 ; e E

  Exp =
      E [Minus / Plus] 1*DIGIT

  Frac =
      DecimalPoint 1*DIGIT

  Int =
      Zero / (DigitOneToNine *DIGIT)

  Minus =
      %x2D ; -

  Plus =
      %x2B ; +

  Zero =
      %x30 ; 0

  NodeKeywords =
      %s"true" / %s"false" / %s"null"

  NodeStringValue =
      ShapeId / TextBlock / QuotedText

  QuotedText =
      DQUOTE *QuotedChar DQUOTE

  QuotedChar =
      %x20-21     ; space - "!"
    / %x23-5B     ; "#" - "["
    / %x5D-10FFFF ; "]"+
    / EscapedChar
    / NL

  EscapedChar =
      Escape (Escape / "'" / DQUOTE / %s"b"
              / %s"f" / %s"n" / %s"r" / %s"t"
              / "/" / UnicodeEscape)

  UnicodeEscape =
      %s"u" Hex Hex Hex Hex

  Hex =
      DIGIT / %x41-46 / %x61-66

  Escape =
      %x5C ; backslash

  TextBlock =
      ThreeDquotes *SP NL *QuotedChar ThreeDquotes

  ThreeDquotes =
      DQUOTE DQUOTE DQUOTE
   */

}
