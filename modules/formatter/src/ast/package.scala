package smithytranslate.formatter
import cats.parse.Parser

package object ast {

  type Comma = Comma.type // for convenience
  case object Underscore
  type Underscore = Underscore.type
  implicit class CharRange(val char: Char) extends AnyVal {
    def inRange(from: Int, to: Int): Boolean =
      char >= from && char <= to
  }
  val escape: Parser[Unit] = Parser.char(0x5c)
  val zero: Parser[Unit] = Parser.char('0')
  val openCurly: Parser[Unit] = Parser.char('{')
  val closeCurly: Parser[Unit] = Parser.char('}')
  val openSquare: Parser[Unit] = Parser.char('[')
  val closeSquare: Parser[Unit] = Parser.char(']')
  val openParentheses: Parser[Unit] = Parser.char('(')
  val closeParentheses: Parser[Unit] = Parser.char(')')
  val escapeChars: List[Char] =
    List('\\', '"', '\'', 'b', 'f', 'n', 'r', 't', '/')
  val quotable0: List[Char] = (0x20.toChar to 0x21.toChar).toList
  val quotable1: List[Char] = (0x23.toChar to 0x5b.toChar).toList
  val quotable2: List[Char] = (0x5d.toChar to 0x10ffff.toChar).toList
  val allQuotable: List[Char] = quotable0 ++ quotable1 ++ quotable2
  val op: List[Char] = List('+', '-')
}
