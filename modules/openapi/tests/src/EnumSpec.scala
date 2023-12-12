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

package smithytranslate.compiler.openapi

import smithytranslate.compiler.SmithyVersion


final class EnumSpec extends munit.FunSuite {

  test("enum") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Color:
                     |      type: string
                     |      enum:
                     |        - red
                     |        - green
                     |        - blue
                     |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |enum Color {
                            |    red
                            |    green
                            |    blue
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("enum - number starting name") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Numbers:
                     |      type: string
                     |      enum:
                     |        - 3three
                     |        - four
                     |        - 5five
                     |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |enum Numbers {
                            |    n3three = "3three"
                            |    four
                            |    n5five = "5five"
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("enum - v1") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Color:
                     |      type: string
                     |      enum:
                     |        - red
                     |        - green
                     |        - blue
                     |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |@enum([
                            | {value: "red"},
                            | {value: "green"},
                            | {value: "blue"}
                            |])
                            |string Color
                            |""".stripMargin

    TestUtils.runConversionTest(
      openapiString,
      expectedString,
      SmithyVersion.One
    )
  }

  test("enum - weird enum values") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths: {}
                           |components:
                           |  schemas:
                           |    path:
                           |      type: string
                           |      enum:
                           |        - "/"
                           |        - "/docs"
                           |        - "/1value"
                           |        - null
                           |      default: "/"
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |enum Path {
                            |    MEMBER_0 = "/"
                            |    docs = "/docs"
                            |    n1value = "/1value"
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(
      openapiString,
      expectedString,
      SmithyVersion.Two
    )
  }

  test("enum - description") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Color:
                     |      description: Test
                     |      type: string
                     |      enum:
                     |        - red
                     |        - green
                     |        - blue
                     |""".stripMargin

    val expectedString = """|namespace foo
                            |@documentation("Test")
                            |enum Color {
                            |    red
                            |    green
                            |    blue
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

}
