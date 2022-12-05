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

import smithytranslate.formatter.parsers.IdlParser
import smithytranslate.formatter.writers.IdlWriter.idlWriter
import smithytranslate.formatter.writers.Writer._
import munit.Location

final class FormatterSpec extends munit.FunSuite {
  private def formatTest(src: String, expected: String)(implicit
      loc: Location
  ) = {
    IdlParser.idlParser
      .parseAll(src)
      .fold(
        err => fail(s"Parsing failed with error: $err"),
        idl => {
          assertEquals(
            idl.write,
            expected
          )
        }
      )
  }

  test("format test - keep new line after control section") {
    val src = """|$version: "2.0"
                 |
                 |namespace test
                 |
                 |use alloy#uuidFormat
                 |
                 |long TargetingRuleId
                 |""".stripMargin
    val expected = """|$version: "2.0"
                      |
                      |namespace test
                      |
                      |use alloy#uuidFormat
                      |
                      |long TargetingRuleId
                      |""".stripMargin
    formatTest(src, expected)
  }

}
