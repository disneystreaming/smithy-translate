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

import cats.data.NonEmptyList
import TestUtils.OpenApiVersion
import TestUtils.OpenApiVersion.V3_0
import TestUtils.OpenApiVersion.V3_1
import smithytranslate.compiler.SmithyVersion
import smithytranslate.compiler.ToSmithyResult

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

    EnumTestUtils.runConversionTest(openapiString, expectedString)
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

    EnumTestUtils.runConversionTest(openapiString, expectedString)
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

    EnumTestUtils.runConversionTest(
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
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |enum Path {
                            |    MEMBER_0 = "/"
                            |    docs = "/docs"
                            |    n1value = "/1value"
                            |}
                            |""".stripMargin

    EnumTestUtils.runConversionTest(
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

    EnumTestUtils.runConversionTest(openapiString, expectedString)
  }

  test("enum - OpenAPI 3.0 default") {
    val openapiString = """|openapi: '3.0.3'
                           |info: {title: test, version: '1.0'}
                           |paths: {}
                           |components:
                           |  schemas:
                           |    path:
                           |      type: string
                           |      enum: ["/", "/docs", "/1value", null]
                           |      default: "/"
                           |""".stripMargin
    val expectedString = """|namespace foo
                            |enum Path {
                            |    MEMBER_0 = "/"
                            |    docs = "/docs"
                            |    n1value = "/1value"
                            |}
                            |""".stripMargin

    EnumTestUtils.runConversionTest(
      openapiString,
      expectedString,
      versions = List(V3_0)
    )
  }

  test("enum - sanitizing member names preserves distinct values") {
    val openapiString = """|openapi: '3.0.3'
                           |info: {title: test, version: '1.0'}
                           |paths: {}
                           |components:
                           |  schemas:
                           |    Values:
                           |      type: string
                           |      enum: [a-b, a_b]
                           |""".stripMargin
    val expectedString = """|namespace foo
                            |enum Values {
                            |    a_b_1 = "a-b"
                            |    a_b
                            |}
                            |""".stripMargin

    EnumTestUtils.runConversionTest(openapiString, expectedString)
  }

  test("enum - OpenAPI 3.0 array bounds do not constrain string length") {
    val openapiString = """|openapi: '3.0.3'
                           |info: {title: test, version: '1.0'}
                           |paths: {}
                           |components:
                           |  schemas:
                           |    Color:
                           |      type: string
                           |      enum: [red, green]
                           |      minItems: 10
                           |      maxItems: 20
                           |""".stripMargin
    EnumTestUtils.runConversionTest(
      openapiString,
      "namespace foo\nenum Color { red, green }",
      versions = List(V3_0)
    )
  }

  List(SmithyVersion.One, SmithyVersion.Two).foreach { smithyVersion =>
    test(s"enum - named, inline and referenced ($smithyVersion)") {
      val openapiString = """|openapi: '3.0.3'
                             |info: {title: test, version: '1.0'}
                             |paths: {}
                             |components:
                             |  schemas:
                             |    Color:
                             |      type: string
                             |      enum: [red, green, blue]
                             |    Palette:
                             |      type: object
                             |      properties:
                             |        inline:
                             |          description: An inline color
                             |          type: string
                             |          enum: [cyan, magenta, yellow]
                             |        reference:
                             |          $ref: '#/components/schemas/Color'
                             |""".stripMargin
      val enums = smithyVersion match {
        case SmithyVersion.One => """|@enum([
                                     |  {value: "red"},
                                     |  {value: "green"},
                                     |  {value: "blue"}
                                     |])
                                     |string Color
                                     |@documentation("An inline color")
                                     |@enum([
                                     |  {value: "cyan"},
                                     |  {value: "magenta"},
                                     |  {value: "yellow"}
                                     |])
                                     |string Inline
                                     |""".stripMargin
        case SmithyVersion.Two => """|enum Color { red, green, blue }
                                     |@documentation("An inline color")
                                     |enum Inline { cyan, magenta, yellow }
                                     |""".stripMargin
      }
      val expectedString = s"""|namespace foo
                               |$enums
                               |structure Palette {
                               |    inline: Inline
                               |    reference: Color
                               |}
                               |""".stripMargin

      EnumTestUtils.runConversionTest(
        openapiString,
        expectedString,
        smithyVersion
      )
    }

    List(false, true).foreach { password =>
      val testName =
        if (password)
          "OpenAPI 3.1 password enum with constraints and description"
        else "constraints and description"
      test(s"enum - $testName ($smithyVersion)") {
        val format = if (password) "format: password" else ""
        val sensitive = if (password) "@sensitive" else ""
        val openapiString = s"""|openapi: '3.0.3'
                                |info: {title: test, version: '1.0'}
                                |paths: {}
                                |components:
                                |  schemas:
                                |    Code:
                                |      description: A color code
                                |      type: string
                                |      $format
                                |      enum: [RED, GREEN, BLUE]
                                |      minLength: 3
                                |      maxLength: 5
                                |      pattern: '^[A-Z]+$$'
                                |""".stripMargin
        val enumShape = smithyVersion match {
          case SmithyVersion.One => """|@enum([
                                       |  {value: "RED"},
                                       |  {value: "GREEN"},
                                       |  {value: "BLUE"}
                                       |])
                                       |string Code
                                       |""".stripMargin
          case SmithyVersion.Two => "enum Code { RED, GREEN, BLUE }"
        }
        val expectedString = s"""|namespace foo
                                 |@documentation("A color code")
                                 |$sensitive
                                 |@length(min: 3, max: 5)
                                 |@pattern("^[A-Z]+$$")
                                 |$enumShape
                                 |""".stripMargin

        EnumTestUtils.runConversionTest(
          openapiString,
          expectedString,
          smithyVersion,
          versions = if (password) List(V3_1) else TestUtils.allVersions
        )
      }
    }

    test(s"enum - OpenAPI 3.1 singleton type array ($smithyVersion)") {
      val openapiString = """|openapi: '3.1.0'
                             |info: {title: test, version: '1.0'}
                             |paths: {}
                             |components:
                             |  schemas:
                             |    Color:
                             |      type: [string]
                             |      enum: [red, green, blue]
                             |""".stripMargin
      val enumShape = smithyVersion match {
        case SmithyVersion.One => """|@enum([
                                     |  {value: "red"},
                                     |  {value: "green"},
                                     |  {value: "blue"}
                                     |])
                                     |string Color
                                     |""".stripMargin
        case SmithyVersion.Two => "enum Color { red, green, blue }"
      }

      EnumTestUtils.runConversionTest(
        openapiString,
        s"namespace foo\n$enumShape",
        smithyVersion,
        versions = List(V3_1)
      )
    }
  }

}

private[openapi] object EnumTestUtils {
  def runConversionTest(
      openapiSpec: String,
      smithySpec: String,
      smithyVersion: SmithyVersion = SmithyVersion.Two,
      versions: List[OpenApiVersion] = TestUtils.allVersions
  )(implicit loc: munit.Location): Unit = {
    val input = TestUtils
      .ConversionTestInput(
        NonEmptyList.one("foo.yaml"),
        openapiSpec,
        smithySpec,
        smithyVersion = smithyVersion
      )
      .copy(versions = versions)

    TestUtils.runConversionAllVersions(input).foreach {
      case (version, TestUtils.ConversionResult(result, expected)) =>
        result match {
          case ToSmithyResult.Success(errors, output) =>
            munit.Assertions.assertEquals(errors, Nil, version)
            munit.Assertions.assertEquals(output, expected, version)
          case ToSmithyResult.Failure(cause, errors) =>
            munit.Assertions.fail(
              s"Expected successful OpenAPI $version enum conversion: $errors",
              cause
            )
        }
    }
  }
}
