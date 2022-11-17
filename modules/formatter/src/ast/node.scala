package smithytranslate
package formatter
package ast

import ast.EscapedChar.{CharCase, UnicodeEscape}
import ast.NodeValue._
import ast.NodeValue.NodeStringValue.{
  QuotedTextCase,
  ShapeIdCase,
  TextBlockCase
}
import ast.NodeValue.Number.{Exp, Frac}
import ast.QuotedChar.{
  EscapedCharCase,
  PreservedDoubleCase,
  SimpleCharCase,
  NewLineCase
}
import ast.whitespace_parser.{sp0, ws, NL}
import cats.parse.Numbers.nonNegativeIntString
import cats.parse.Parser
import cats.parse.Rfc5234.{digit, dquote, hexdig}
import parsers.ShapeIdParser.{identifier, shape_id}

sealed trait NodeValue

object NodeValue {
  case class NodeArray(
      whitespace: Whitespace,
      values: List[(NodeValue, Whitespace)]
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

object node_parser {

  val opParser: Parser[Char] = Parser.charIn(op)
  val qChar: Parser[Char] = Parser.charIn(allQuotable)
  val preserved_double: Parser[PreservedDouble] = escape *> qChar
    .map(PreservedDouble)
  val unicode_escape: Parser[UnicodeEscape] =
    (Parser.char('u') *> hexdig ~ hexdig ~ hexdig).map { case ((a, b), c) =>
      UnicodeEscape(a, b, c)
    }

  val escaped_char: Parser[EscapedChar] =
    escape *> (Parser.charIn(escapeChars).map(CharCase) | unicode_escape)
  val QuotedChar: Parser[QuotedChar] =
    qChar.backtrack.map(SimpleCharCase) | escaped_char.backtrack.map(
      EscapedCharCase
    ) | preserved_double.map(PreservedDoubleCase) | NL.as(NewLineCase)

  val ThreeDquotes = dquote ~ dquote ~ dquote
  val text_block: Parser[TextBlock] =
    ((ThreeDquotes ~ sp0 *> NL) *> QuotedChar.rep0 <* ThreeDquotes)
      .map(TextBlock)
  val quoted_text: Parser[QuotedText] =
    (dquote *> QuotedChar.rep0 <* dquote).map(QuotedText)
  val node_string_value: Parser[NodeStringValue] = shape_id.backtrack.map(
    ShapeIdCase
  ) | text_block.backtrack.map(TextBlockCase) | quoted_text.backtrack
    .map(QuotedTextCase)
  val node_keywords: Parser[NodeKeyword] =
    Parser.stringIn(Set("true", "false", "null")).map(NodeKeyword)

  val frac: Parser[Frac] = Parser.char('.') *> digit.string.map(Frac)
  val exp: Parser[Exp] =
    (Parser.charWhere(c => c == 'e' || c == 'E') ~ opParser.? ~ digit.string)
      .map { case ((a, b), c) =>
        Exp(a, b, c)
      }

  val number: Parser[SmithyNumber] =
    (Parser.charWhere(_ == '-').?.with1 ~ nonNegativeIntString ~ frac.? ~ exp.?)
      .map { case (((a, b), c), d) =>
        SmithyNumber(a, b, c, d)
      }
  lazy val node_value: Parser[NodeValue] = Parser.defer(
    nodeArray.backtrack | nodeObject.backtrack | number.backtrack | node_keywords.backtrack | node_string_value.backtrack
  )

  val node_object_key: Parser[NodeObjectKey] = quoted_text.backtrack
    .map(
      NodeObjectKey.QuotedTextNok
    ) | identifier.map(NodeObjectKey.IdentifierNok)

  lazy val node_object_kvp: Parser[NodeObjectKeyValuePair] =
    ((node_object_key ~ ws <* Parser.char(':')) ~ ws ~ node_value).map {
      case (((a, b), c), d) => NodeObjectKeyValuePair(a, b, c, d)
    }

  val nodeObject: Parser[NodeObject] =
    (openCurly *> ws ~ (node_object_kvp ~ (ws.with1 ~ node_object_kvp).backtrack.rep0).?.backtrack <* (ws ~ closeCurly))
      .map { case (whitespace, maybeTuple) =>
        NodeObject(whitespace, maybeTuple)
      }

  val nodeArray: Parser[NodeArray] =
    ((openSquare *> ws ~ (node_value ~ ws).backtrack.rep0) <* (ws ~ closeSquare))
      .map { case (whitespace, maybeTuple) =>
        NodeArray(whitespace, maybeTuple)
      }
}
