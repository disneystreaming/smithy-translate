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
final class ListSpec extends munit.FunSuite {

  test("lists") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths: {}
                           |components:
                           |  schemas:
                           |    StringArray:
                           |      type: array
                           |      items:
                           |        type: string
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |list StringArray {
                            |    member: String
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("lists - external docs") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths: {}
                           |components:
                           |  schemas:
                           |    StringArray:
                           |      type: array
                           |      items:
                           |        type: string
                           |      externalDocs:
                           |        description: Example
                           |        url: https://www.example.com
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |@externalDocumentation(
                            |  "Example": "https://www.example.com"
                            |)
                            |list StringArray {
                            |    member: String
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("lists - with description") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths: {}
                           |components:
                           |  schemas:
                           |    StringArray:
                           |      description: Test description
                           |      type: array
                           |      items:
                           |        type: string
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |@documentation("Test description")
                            |list StringArray {
                            |    member: String
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("lists - nested") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths: {}
                           |components:
                           |  schemas:
                           |    StringArray:
                           |      type: array
                           |      items:
                           |        type: array
                           |        items:
                           |          type: string
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |list StringArray {
                            |    member: StringArrayItem
                            |}
                            |
                            |list StringArrayItem {
                            |    member: String
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("lists - double nested") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths: {}
                           |components:
                           |  schemas:
                           |    StringArray:
                           |      type: array
                           |      items:
                           |        type: array
                           |        items:
                           |          type: array
                           |          items:
                           |            type: string
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |list StringArray {
                            |    member: StringArrayItem
                            |}
                            |
                            |list StringArrayItem {
                            |    member: StringArrayItemItem
                            |}
                            |
                            |list StringArrayItemItem {
                            |    member: String
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("lists - structure member") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths: {}
                           |components:
                           |  schemas:
                           |    StringArray:
                           |      type: array
                           |      items:
                           |        type: object
                           |        properties:
                           |          test:
                           |            type: string
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |list StringArray {
                            |    member: StringArrayItem
                            |}
                            |
                            |structure StringArrayItem {
                            |    test: String
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("lists - structure member ref") {
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
                           |    StringArray:
                           |      type: array
                           |      items:
                           |        $ref: '#/components/schemas/Test'
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Test {
                            |    test: String
                            |}
                            |
                            |list StringArray {
                            |    member: Test
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

}
