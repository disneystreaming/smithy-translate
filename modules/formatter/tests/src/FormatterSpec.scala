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
                      |
                      |""".stripMargin
    formatTest(src, expected)
  }

  test("format test - handles triple comments") {
    // this test is intended to catch instances where
    // the line after a triple comment is moved onto the comment
    // line. this example produces the following:
    //  `/// Used to identify the disney platformstring PartnerName`
    // this only happes if `long TargetingRuleId` is there
    val src = """|$version: "2.0"
                 |
                 |namespace test
                 |
                 |use dslib#uuidFormat
                 |
                 |long TargetingRuleId
                 |
                 |/// Used to identify the disney platform
                 |string PartnerName
                 |
                 |integer FeatureVersion
                 |""".stripMargin
    val expected = """|$version: "2.0"
                      |
                      |namespace test
                      |
                      |use dslib#uuidFormat
                      |
                      |long TargetingRuleId
                      |
                      |/// Used to identify the disney platform
                      |string PartnerName
                      |
                      |integer FeatureVersion
                      |
                      |""".stripMargin
    formatTest(src, expected)
  }

  test("format test - newlines between each shape") {
    val src = """|$version: "2.0"
                 |
                 |namespace test
                 |
                 |use dslib#uuidFormat
                 |
                 |long TargetingRuleId
                 |
                 |string PartnerName
                 |
                 |integer FeatureVersion
                 |""".stripMargin
    val expected = """|$version: "2.0"
                      |
                      |namespace test
                      |
                      |use dslib#uuidFormat
                      |
                      |long TargetingRuleId
                      |
                      |string PartnerName
                      |
                      |integer FeatureVersion
                      |
                      |""".stripMargin
    formatTest(src, expected)
  }

  test("format test - comment on traits") {
    val src = """|$version: "2.0"
                 |
                 |namespace test
                 |
                 |/// comment between trait and shape
                 |@documentation("this is a test")
                 |string PartnerName
                 |
                 |integer FeatureVersion
                 |""".stripMargin
    val expected = """|$version: "2.0"
                      |
                      |namespace test
                      |
                      |/// comment between trait and shape
                      |@documentation("this is a test")
                      |string PartnerName
                      |
                      |integer FeatureVersion
                      |
                      |""".stripMargin
    formatTest(src, expected)
  }

  test("format test - comment beteen trait and shape") {
    val src = """|$version: "2.0"
                 |
                 |namespace test
                 |
                 |@documentation("this is a test")
                 |/// comment between trait and shape
                 |string PartnerName
                 |
                 |integer FeatureVersion
                 |""".stripMargin
    val expected = """|$version: "2.0"
                      |
                      |namespace test
                      |
                      |@documentation("this is a test")
                      |/// comment between trait and shape
                      |string PartnerName
                      |
                      |integer FeatureVersion
                      |
                      |""".stripMargin
    formatTest(src, expected)
  }

  test("format test - comments beteen trait and shape") {
    val src = """|$version: "2.0"
                 |
                 |namespace test
                 |
                 |@documentation("this is a test")
                 |/// comment between trait and shape
                 |///two comments?
                 |string PartnerName
                 |
                 |integer FeatureVersion
                 |""".stripMargin
    val expected = """|$version: "2.0"
                      |
                      |namespace test
                      |
                      |@documentation("this is a test")
                      |/// comment between trait and shape
                      |/// two comments?
                      |string PartnerName
                      |
                      |integer FeatureVersion
                      |
                      |""".stripMargin
    formatTest(src, expected)
  }

  test("format test - enum") {
    val src = """|$version: "2.0"
                 |
                 |namespace test
                 |
                 |enum MyEnum {
                 |  VALUE1,
                 |    VALUE2
                 |}
                 |enum OtherEnum {
                 |  V1 = "v1"
                 |V2 = "v2"
                 |}
                 |""".stripMargin
    val expected = """|$version: "2.0"
                      |
                      |namespace test
                      |
                      |enum MyEnum {
                      |    VALUE1
                      |    VALUE2
                      |}
                      |
                      |enum OtherEnum {
                      |    V1 = "v1"
                      |    V2 = "v2"
                      |}
                      |
                      |""".stripMargin
    formatTest(src, expected)
  }

  test("format test - identation on shape with body") {
    val src = """|$version: "2.0"
                 |
                 |namespace test
                 |
                 |structure MyStruct {
                 |this: String,
                 |  that: Integer
                 |}
                 |
                 |structure MyStructDefault {
                 |  other: Integer = 1
                 |}
                 |
                 |union MyUnion {
                 | this: String,
                 |orThat: Integer
                 |}
                 |""".stripMargin
    val expected = """|$version: "2.0"
                      |
                      |namespace test
                      |
                      |structure MyStruct {
                      |    this: String,
                      |    that: Integer
                      |}
                      |
                      |structure MyStructDefault {
                      |    other: Integer = 1
                      |}
                      |
                      |union MyUnion {
                      |    this: String,
                      |    orThat: Integer,
                      |}
                      |
                      |""".stripMargin
    formatTest(src, expected)
  }
}
