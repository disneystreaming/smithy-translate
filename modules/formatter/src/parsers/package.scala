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
import cats.parse.Parser

package object parsers {

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
