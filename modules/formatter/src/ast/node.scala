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

import smithytranslate.formatter.ast.NodeValue.Number.{Exp, Frac}

sealed trait NodeValue

object NodeValue {
  case class NodeArrayValue(
      value: NodeValue,
      ws0: Whitespace
  )
  case class NodeArray(
      whitespace: Whitespace,
      values: List[NodeArrayValue]
  ) extends NodeValue
  case class NodeObject(
      whitespace: Whitespace,
      values: Option[
        (NodeObjectKeyValuePair, List[(Whitespace, NodeObjectKeyValuePair)])
      ]
  ) extends NodeValue

  case class NodeObjectKeyValuePair(
      nodeObjectKey: NodeObjectKey,
      ws0: Whitespace,
      ws1: Whitespace,
      nodeValue: NodeValue
  )

  sealed trait NodeObjectKey

  object NodeObjectKey {
    case class QuotedTextNok(text: QuotedText) extends NodeObjectKey

    case class IdentifierNok(identifier: Identifier) extends NodeObjectKey
  }

  case class SmithyNumber(
      minus: Option[Char],
      smithyInt: String,
      frac: Option[Frac],
      exp: Option[Exp]
  ) extends NodeValue

  object Number {

    case class Frac(digits: String)

    case class Exp(symbol: Char, op: Option[Char], digits: String)

  }

  case class NodeKeyword(value: String) extends NodeValue

  sealed trait NodeStringValue extends NodeValue

  object NodeStringValue {
    case class ShapeIdCase(shapeId: ShapeId) extends NodeStringValue

    case class TextBlockCase(textBlock: TextBlock) extends NodeStringValue

    case class QuotedTextCase(quotedText: QuotedText) extends NodeStringValue
  }
}

case class QuotedText(text: List[QuotedChar])

sealed trait QuotedChar

object QuotedChar {

  case class SimpleCharCase(char: Char) extends QuotedChar

  case class EscapedCharCase(char: EscapedChar) extends QuotedChar

  case class PreservedDoubleCase(preservedDouble: PreservedDouble)
      extends QuotedChar

  case object NewLineCase extends QuotedChar
}

case class EscapedCharValue(escapedChar: EscapedChar)

//    escape (escape / "'" / DQUOTE / "b" / "f" / "n" / "r" / "t" / "/" / unicode_escape)
sealed trait EscapedChar

object EscapedChar {
  case class CharCase(char: Char) extends EscapedChar

  case class UnicodeEscape(hex: Char, hex2: Char, hex3: Char)
      extends EscapedChar
}

case class PreservedDouble(char: Char)

case class TextBlock(quotedChars: List[QuotedChar])
