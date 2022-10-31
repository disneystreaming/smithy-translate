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
final class MapSpec extends munit.FunSuite {

  test("maps") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    StringStringMap:
                     |      type: object
                     |      additionalProperties:
                     |        type: string
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |map StringStringMap {
                      |    key: String,
                      |    value: String
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("maps - description") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    StringStringMap:
                     |      description: Test description
                     |      type: object
                     |      additionalProperties:
                     |        type: string
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |@documentation("Test description")
                      |map StringStringMap {
                      |    key: String,
                      |    value: String
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("maps - nested") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    StringStringMap:
                     |      type: object
                     |      additionalProperties:
                     |        type: object
                     |        additionalProperties:
                     |          type: string
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |map StringStringMapItem {
                      |    key: String,
                      |    value: String,
                      |}
                      |
                      |map StringStringMap {
                      |    key: String,
                      |    value: StringStringMapItem
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("maps - double nested") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    StringStringMap:
                     |      type: object
                     |      additionalProperties:
                     |        type: object
                     |        additionalProperties:
                     |          type: object
                     |          additionalProperties:
                     |            type: string
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |map StringStringMapItemItem {
                      |    key: String,
                      |    value: String,
                      |}
                      |
                      |map StringStringMapItem {
                      |    key: String,
                      |    value: StringStringMapItemItem,
                      |}
                      |
                      |map StringStringMap {
                      |    key: String,
                      |    value: StringStringMapItem
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("maps - structure member") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    StringStructMap:
                     |      type: object
                     |      additionalProperties:
                     |        type: object
                     |        properties:
                     |          test:
                     |            type: string
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |structure StringStructMapItem {
                      |    test: String
                      |}
                      |
                      |map StringStructMap {
                      |    key: String,
                      |    value: StringStructMapItem
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("maps - structure member reference") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Test:
                     |      type: object
                     |      properties:
                     |        one:
                     |          type: integer
                     |    StringStructMap:
                     |      type: object
                     |      additionalProperties:
                     |        $ref: '#/components/schemas/Test'
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |structure Test {
                      |    one: Integer
                      |}
                      |
                      |map StringStructMap {
                      |    key: String,
                      |    value: Test
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("maps - list member") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    StringStringMap:
                     |      type: object
                     |      additionalProperties:
                     |        type: array
                     |        items:
                     |          type: string
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |list StringStringMapItem {
                      |    member: String
                      |}
                      |
                      |map StringStringMap {
                      |    key: String,
                      |    value: StringStringMapItem
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

}
