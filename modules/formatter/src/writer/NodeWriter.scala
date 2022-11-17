package smithytranslate
package formatter
package writers

import ast.NodeValue.{
  NodeArray,
  NodeKeyword,
  NodeObject,
  NodeObjectKey,
  NodeObjectKeyValuePair,
  NodeStringValue,
  SmithyNumber
}
import ast.{
  EscapedChar,
  NodeValue,
  PreservedDouble,
  QuotedChar,
  QuotedText,
  TextBlock
}
import ast.EscapedChar.{CharCase, UnicodeEscape}
import ast.NodeValue.NodeStringValue.{
  QuotedTextCase,
  ShapeIdCase,
  TextBlockCase
}
import ast.NodeValue.Number.{Exp, Frac}
import ast.QuotedChar.{
  EscapedCharCase,
  NewLineCase,
  PreservedDoubleCase,
  SimpleCharCase
}
import util.string_ops.{addBrackets, indent}
import ShapeIdWriter.{identifierWriter, shapeIdWriter}
import WhiteSpaceWriter.wsWriter
import Writer.{WriterOps, WriterOpsIterable}

object NodeWriter {
  implicit val nodeArrayWriter: Writer[NodeArray] = Writer.write {
    case NodeArray(ws, Nil) => s"[${ws.write}]"
    case NodeArray(ws, elems) =>
      "[\n" +
        indent(
          s"${ws.write}${elems.map { case (value, ws) =>
              s"${value.write}${ws.write},\n"
            }.mkString}\n]",
          "\n",
          4
        )
  }
  implicit val nodeObjectKvpWriter: Writer[NodeObjectKeyValuePair] =
    Writer.write { case NodeObjectKeyValuePair(key, ws0, ws1, value) =>
      showKeyValue(key, ws0, ws1, value)
    }

  implicit val nodeObjectWriter: Writer[NodeObject] = Writer.write {
    case NodeObject(ws, None)               => s"{${ws.write}}"
    case NodeObject(ws, Some((nokvp, Nil))) => s"{${ws.write}${nokvp.write}}"
    case NodeObject(ws, Some((nokvp, rest))) =>
      ws.write + indent(
        s"${ws.write}${nokvp.write}${rest.map { case (ws, kvp) =>
            s",\n${ws.write}${kvp.write}"
          }.mkString}",
        "\n",
        4
      )
  }

  implicit val nodeValueWriter: Writer[NodeValue] = Writer.write {
    case array: NodeArray       => array.write
    case nodeObject: NodeObject => addBrackets(nodeObject.write)
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
    s"${number.minus.getOrElse("")}${number.smithyInt.write}${number.frac
        .getOrElse("")}${number.exp.getOrElse("")}"
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
  implicit val preservedDoubleWriter: Writer[PreservedDouble] = Writer.write {
    case PreservedDouble(char) => s"\\${char.write}"
  }
  implicit val quotedCharWriter: Writer[QuotedChar] = Writer.write {
    case SimpleCharCase(char)                 => s"${char.write}"
    case EscapedCharCase(escapedChar)         => escapedChar.write
    case PreservedDoubleCase(preservedDouble) => preservedDouble.write
    case NewLineCase                          => "\\n"
  }
  implicit val quotedTextWriter: Writer[QuotedText] = Writer.write {
    case QuotedText(text) =>
      s"\"${text.map(_.write).mkString}\""
  }
  implicit val textBlockWriter: Writer[TextBlock] = Writer.write {
    case TextBlock(text) =>
      s"\"\"\"${text.writeN}\"\"\""
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

}
