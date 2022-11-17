package smithytranslate
package formatter
package parsers

import smithytranslate.formatter.ast.EscapedChar.{CharCase, UnicodeEscape}
import smithytranslate.formatter.ast.QuotedChar.{
  EscapedCharCase,
  NewLineCase,
  PreservedDoubleCase,
  SimpleCharCase
}
import smithytranslate.formatter.ast.NodeValue.Number.{Exp, Frac}
import smithytranslate.formatter.ast.NodeValue.{
  NodeArray,
  NodeKeyword,
  NodeObject,
  NodeObjectKey,
  NodeObjectKeyValuePair,
  NodeStringValue,
  SmithyNumber
}
import smithytranslate.formatter.ast.NodeValue.NodeStringValue.{
  QuotedTextCase,
  ShapeIdCase,
  TextBlockCase
}
import smithytranslate.formatter.ast.{
  allQuotable,
  escape,
  escapeChars,
  op,
  openCurly,
  openSquare,
  EscapedChar,
  NodeValue,
  PreservedDouble,
  QuotedChar,
  QuotedText,
  TextBlock
}
import smithytranslate.formatter.ast.whitespace_parser.{sp0, ws, NL}
import cats.parse.Numbers.nonNegativeIntString
import cats.parse.Parser
import cats.parse.Rfc5234.{digit, dquote, hexdig}
import parsers.ShapeIdParser.{identifier, shape_id}

object NodeParser {

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
  / PreservedDouble
  / NL

EscapedChar =
    Escape (Escape / "'" / DQUOTE / %s"b"
            / %s"f" / %s"n" / %s"r" / %s"t"
            / "/" / UnicodeEscape)

UnicodeEscape =
    %s"u" Hex Hex Hex Hex

Hex =
    DIGIT / %x41-46 / %x61-66

PreservedDouble =
    Escape (%x20-21 / %x23-5B / %x5D-10FFFF)

Escape =
    %x5C ; backslash

TextBlock =
    ThreeDquotes *SP NL *QuotedChar ThreeDquotes

ThreeDquotes =
    DQUOTE DQUOTE DQUOTE
 */
