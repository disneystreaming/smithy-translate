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

import smithytranslate.formatter.ast.Idl
import cats.syntax.all._

trait SmithyParser {
  def parse(input: String): Either[String, Idl]
}

object SmithyParserLive extends SmithyParser {
  override def parse(input: String): Either[String, Idl] =
    IdlParser.idlParser
      .parseAll(input)
      .leftMap(error =>
        s"failed at offset ${error.failedAtOffset} with error: ${error.show}  ${error._2.map(_.context.mkString("")).toList.mkString}"
      )
}
