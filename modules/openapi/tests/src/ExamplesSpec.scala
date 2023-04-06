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

package smithytranslate.openapi

final class ExamplesSpec extends munit.FunSuite {

  test("example - integer") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths: {}
                           |components:
                           |  schemas:
                           |    Id:
                           |      type: integer
                           |      format: int32
                           |      example: 1
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use alloy#dataExamples
                            |
                            |@dataExamples([1])
                            |integer Id
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("example - string") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths: {}
                           |components:
                           |  schemas:
                           |    Id:
                           |      type: string
                           |      example: something
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use alloy#dataExamples
                            |
                            |@dataExamples(["something"])
                            |string Id
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("example - structure") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths: {}
                           |components:
                           |  schemas:
                           |    User:
                           |      type: object
                           |      properties:
                           |        int:
                           |          type: integer
                           |        str:
                           |          type: string
                           |        dbl:
                           |          type: number
                           |        lst:
                           |          type: array
                           |          items:
                           |            type: integer
                           |        bool:
                           |          type: boolean
                           |        struct:
                           |          type: object
                           |          properties:
                           |            str:
                           |              type: string
                           |      example:
                           |        int: 1
                           |        str: Jessica Smith
                           |        dbl: 1.1
                           |        lst: [1, 2, 3]
                           |        bool: true
                           |        struct:
                           |          str: whatever
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use alloy#dataExamples
                            |
                            |@dataExamples([
                            |  {
                            |    int: 1,
                            |    str: "Jessica Smith",
                            |    dbl: 1.1,
                            |    lst: [1, 2, 3],
                            |    bool: true,
                            |    struct: { str: "whatever" }
                            |  }
                            |])
                            |structure User {
                            |  int: Integer
                            |  str: String
                            |  dbl: Double
                            |  lst: Lst
                            |  bool: Boolean
                            |  struct: Struct
                            |}
                            |
                            |structure Struct {
                            |  str: String
                            |}
                            |
                            |list Lst {
                            |  member: Integer
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("example - array") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths: {}
                           |components:
                           |  schemas:
                           |    a:
                           |      type: array
                           |      items:
                           |        type: integer
                           |        format: int32
                           |      example: [1, 2, 3]
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use alloy#dataExamples
                            |
                            |@dataExamples([[1, 2, 3]])
                            |list A {
                            |  member: Integer
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("example - set") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths: {}
                           |components:
                           |  schemas:
                           |    a:
                           |      type: array
                           |      items:
                           |        type: integer
                           |        format: int32
                           |      example: [1, 2, 3]
                           |      uniqueItems: true
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use alloy#dataExamples
                            |
                            |@uniqueItems
                            |@dataExamples([[1, 2, 3]])
                            |list A {
                            |  member: Integer
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("example - map") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths: {}
                           |components:
                           |  schemas:
                           |    m:
                           |      type: object
                           |      additionalProperties:
                           |        type: string
                           |      example:
                           |        foo: bar
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use alloy#dataExamples
                            |
                            |@dataExamples([{
                            |  foo: "bar"
                            |}])
                            |map M {
                            |  key: String
                            |  value: String
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("example - map with structure member") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths: {}
                           |components:
                           |  schemas:
                           |    m:
                           |      type: object
                           |      additionalProperties:
                           |        type: object
                           |        properties:
                           |          test:
                           |            type: string
                           |      example:
                           |        foo:
                           |          test: bar
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use alloy#dataExamples
                            |
                            |@dataExamples([{
                            |  foo: {
                            |    test: "bar"
                            |  }
                            |}])
                            |map M {
                            |  key: String
                            |  value: MItem
                            |}
                            |
                            |structure MItem {
                            |  test: String
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }
}
