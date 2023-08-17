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

final class AllOfSpec extends munit.FunSuite {

  test("allOf - one ref one embedded") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Other:
                     |      type: object
                     |      properties:
                     |        l:
                     |          type: integer
                     |    Object:
                     |      allOf:
                     |        - $ref: "#/components/schemas/Other"
                     |        - type: object
                     |          properties:
                     |            s:
                     |              type: string
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |structure Object with [Other] {
                      | s: String
                      |}
                      |
                      |@mixin
                      |structure Other {
                      |    l: Integer,
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("allOf - two refs") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    One:
                     |      type: object
                     |      properties:
                     |        o:
                     |          type: integer
                     |    Two:
                     |      type: object
                     |      properties:
                     |        t:
                     |          type: integer
                     |    Object:
                     |      allOf:
                     |        - $ref: "#/components/schemas/One"
                     |        - $ref: "#/components/schemas/Two"
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |structure Object {
                      | o: Integer,
                      | t: Integer
                      |}
                      |
                      |structure One {
                      |    o: Integer,
                      |}
                      |
                      |structure Two {
                      |    t: Integer,
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("allOf - two refs - description") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    One:
                     |      type: object
                     |      properties:
                     |        o:
                     |          type: integer
                     |    Two:
                     |      type: object
                     |      properties:
                     |        t:
                     |          type: integer
                     |    Object:
                     |      description: Test
                     |      allOf:
                     |        - $ref: "#/components/schemas/One"
                     |        - $ref: "#/components/schemas/Two"
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |@documentation("Test")
                      |structure Object {
                      | o: Integer,
                      | t: Integer
                      |}
                      |
                      |structure One {
                      |    o: Integer,
                      |}
                      |
                      |structure Two {
                      |    t: Integer,
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("allOf - document ref") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    One:
                     |      type: object
                     |      properties:
                     |        o:
                     |          type: integer
                     |    Two:
                     |      type: object
                     |      properties:
                     |    Object:
                     |      allOf:
                     |        - $ref: "#/components/schemas/One"
                     |        - $ref: "#/components/schemas/Two"
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |document Object
                      |
                      |structure One {
                      |    o: Integer,
                      |}
                      |
                      |document Two
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }
}
