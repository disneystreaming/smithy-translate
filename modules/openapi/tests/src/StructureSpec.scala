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
final class StructureSpec extends munit.FunSuite {

  test("structures") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Object:
                     |      type: object
                     |      properties:
                     |        s:
                     |          type: string
                     |        i:
                     |          type: integer
                     |          format: int32
                     |        b:
                     |          type: boolean
                     |        l:
                     |          type: integer
                     |          format: int64
                     |          minimum: 100
                     |        t:
                     |          type: string
                     |          format: date-time
                     |        d:
                     |          type: object
                     |      required:
                     |        - s
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |structure Object {
                      | @required
                      | s: String,
                      | i: Integer,
                      | b: Boolean,
                      | @range(min: 100)
                      | l: Long,
                      | @timestampFormat("date-time")
                      | t: Timestamp,
                      | d: Document
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("structures - description") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Object:
                     |      description: Test
                     |      type: object
                     |      properties:
                     |        s:
                     |          type: string
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |@documentation("Test")
                      |structure Object {
                      | s: String
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("structures - double nested") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Object:
                     |      type: object
                     |      properties:
                     |        bar:
                     |          type: object
                     |          properties:
                     |            baz:
                     |              type: object
                     |              properties:
                     |                str:
                     |                  type: string
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |structure Object {
                      |    bar: Bar
                      |}
                      |
                      |structure Bar {
                      |    baz: Baz
                      |}
                      |
                      |structure Baz {
                      |    str: String
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("structures - nested") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Object:
                     |      type: object
                     |      properties:
                     |        bar:
                     |          type: object
                     |          properties:
                     |            str:
                     |              type: string
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |structure Object {
                      |    bar: Bar
                      |}
                      |
                      |structure Bar {
                      |    str: String
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("structures - nested reference") {
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
                     |        s:
                     |          type: string
                     |    Object:
                     |      type: object
                     |      properties:
                     |        t:
                     |          $ref: '#/components/schemas/Test'
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |structure Object {
                      |    t: Test,
                      |}
                      |
                      |structure Test {
                      |    s: String,
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("structures - list member") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Object:
                     |      type: object
                     |      properties:
                     |        test:
                     |          type: array
                     |          items:
                     |            type: string
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |structure Object {
                      | test: Test
                      |}
                      |
                      |list Test {
                      |  member: String
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("structures - map member") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Object:
                     |      type: object
                     |      properties:
                     |        test:
                     |          type: object
                     |          additionalProperties:
                     |            type: string
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |structure Object {
                      | test: Test
                      |}
                      |
                      |map Test {
                      |  key: String,
                      |  value: String
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("reference hinted newtype from struct") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    MyObj:
                     |      type: object
                     |      properties:
                     |        s:
                     |          $ref: '#/components/schemas/MyString'
                     |    MyString:
                     |      type: string
                     |      format: password
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |structure MyObj {
                      |  s: MyString
                      |}
                      |
                      |@sensitive
                      |string MyString
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("structures - no type specified") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths: {}
                           |components:
                           |  schemas:
                           |    Object:
                           |      properties:
                           |        s:
                           |          type: string
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Object {
                            | s: String
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("structures - name starting with a number") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths: {}
                           |components:
                           |  schemas:
                           |    100Object:
                           |      properties:
                           |        s:
                           |          type: string
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure n100Object {
                            | s: String
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

}
