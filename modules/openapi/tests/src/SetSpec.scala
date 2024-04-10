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

final class SetSpec extends munit.FunSuite {

  test("sets") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths: {}
                           |components:
                           |  schemas:
                           |    StringSet:
                           |      type: array
                           |      items:
                           |        type: string
                           |      uniqueItems: true
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |@uniqueItems
                            |list StringSet {
                            |    member: String
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("sets - description") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths: {}
                           |components:
                           |  schemas:
                           |    StringSet:
                           |      description: Test
                           |      type: array
                           |      items:
                           |        type: string
                           |      uniqueItems: true
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |@documentation("Test")
                            |@uniqueItems
                            |list StringSet {
                            |    member: String
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("sets - nested") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths: {}
                           |components:
                           |  schemas:
                           |    StringSet:
                           |      type: array
                           |      items:
                           |        type: array
                           |        items:
                           |          type: string
                           |        uniqueItems: true
                           |      uniqueItems: true
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |@uniqueItems
                            |list StringSet {
                            |    member: StringSetItem
                            |}
                            |
                            |@uniqueItems
                            |list StringSetItem {
                            |    member: String
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("sets - structure member") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths: {}
                           |components:
                           |  schemas:
                           |    StringSet:
                           |      type: array
                           |      items:
                           |        type: object
                           |        properties:
                           |          test:
                           |            type: string
                           |      uniqueItems: true
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |@uniqueItems
                            |list StringSet {
                            |    member: StringSetItem
                            |}
                            |
                            |structure StringSetItem {
                            |    test: String
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("sets - structure member ref") {
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
                           |        test:
                           |          type: string
                           |    StringSet:
                           |      type: array
                           |      items:
                           |        $ref: '#/components/schemas/Test'
                           |      uniqueItems: true
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Test {
                            |    test: String
                            |}
                            |
                            |@uniqueItems
                            |list StringSet {
                            |    member: Test
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

}
