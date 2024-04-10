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
                           |      description: other
                           |      type: object
                           |      properties:
                           |        l:
                           |          type: integer
                           |    Object:
                           |      description: object
                           |      allOf:
                           |        - $ref: "#/components/schemas/Other"
                           |        - type: object
                           |          properties:
                           |            s:
                           |              type: string
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |/// object
                            |structure Object with [Other] {
                            | s: String
                            |}
                            |
                            |/// other
                            |@mixin
                            |structure Other {
                            |    l: Integer
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
                           |      description: one
                           |      type: object
                           |      properties:
                           |        o:
                           |          type: integer
                           |    Two:
                           |      description: two
                           |      type: object
                           |      properties:
                           |        t:
                           |          type: integer
                           |    Object:
                           |      description: object
                           |      allOf:
                           |        - $ref: "#/components/schemas/One"
                           |        - $ref: "#/components/schemas/Two"
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |/// object
                            |structure Object with [One, Two] {}
                            |
                            |/// one
                            |@mixin
                            |structure One {
                            |    o: Integer,
                            |}
                            |
                            |/// two
                            |@mixin
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
                           |      description: one
                           |      type: object
                           |      properties:
                           |        o:
                           |          type: integer
                           |    Two:
                           |      description: two
                           |      type: object
                           |      properties:
                           |    Object:
                           |      description: object
                           |      allOf:
                           |        - $ref: "#/components/schemas/One"
                           |        - $ref: "#/components/schemas/Two"
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |/// object
                            |document Object
                            |
                            |/// one
                            |structure One {
                            |    o: Integer,
                            |}
                            |
                            |/// two
                            |document Two
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("allOf - document ref with two layers") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths: {}
                           |components:
                           |  schemas:
                           |    Three:
                           |      description: three
                           |      type: object
                           |      properties:
                           |        o:
                           |          type: integer
                           |    One:
                           |      description: one
                           |      allOf:
                           |        - $ref: "#/components/schemas/Two"
                           |        - $ref: "#/components/schemas/Three"
                           |    Two:
                           |      description: two
                           |      type: object
                           |      properties:
                           |    Object:
                           |      description: object
                           |      allOf:
                           |        - $ref: "#/components/schemas/One"
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |/// object
                            |document Object
                            |
                            |/// three
                            |structure Three {
                            |    o: Integer
                            |}
                            |
                            |/// one
                            |document One
                            |
                            |/// two
                            |document Two
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("allOf - one ref one embedded with another reference") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths: {}
                           |components:
                           |  schemas:
                           |    Also:
                           |      description: also
                           |      type: object
                           |      properties:
                           |        other:
                           |          $ref: "#/components/schemas/Other"
                           |    Other:
                           |      description: other
                           |      type: object
                           |      properties:
                           |        l:
                           |          type: integer
                           |    Object:
                           |      description: object
                           |      allOf:
                           |        - $ref: "#/components/schemas/Other"
                           |        - type: object
                           |          properties:
                           |            s:
                           |              type: string
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |/// object
                            |structure Object with [OtherMixin] {
                            | s: String
                            |}
                            |
                            |/// other
                            |@mixin
                            |structure OtherMixin {
                            |    l: Integer
                            |}
                            |
                            |/// other
                            |structure Other with [OtherMixin] {}
                            |
                            |/// also
                            |structure Also {
                            |  other: Other
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("allOf - multiple layers") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths: {}
                           |components:
                           |  schemas:
                           |    Three:
                           |      description: three
                           |      type: object
                           |      properties:
                           |        three:
                           |          type: string
                           |    Two:
                           |      description: two
                           |      allOf:
                           |        - $ref: "#/components/schemas/Three"
                           |        - type: object
                           |          properties:
                           |            two:
                           |              type: string
                           |    One:
                           |      description: one
                           |      allOf:
                           |        - $ref: "#/components/schemas/Two"
                           |        - type: object
                           |          properties:
                           |            one:
                           |              type: string
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |/// one
                            |structure One with [Two] {
                            |  one: String
                            |}
                            |
                            |/// three
                            |@mixin
                            |structure Three {
                            |  three: String
                            |}
                            |
                            |/// two
                            |@mixin
                            |structure Two with [Three] {
                            |  two: String
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("allOf - multiple parents") {
    val openapiString =
      """|openapi: '3.0.'
         |info:
         |  title: test
         |  version: '1.0'
         |paths: {}
         |components:
         |  schemas:
         |    NumberParentOne:
         |      allOf:
         |        - $ref: '#/components/schemas/NumberParentOneParent'
         |    NumberParentOneParent:
         |      type: object
         |      properties:
         |        num:
         |          type: integer
         |      required:
         |        - num
         |    Number:
         |      allOf:
         |        - $ref: '#/components/schemas/NumberParentOne'
         |""".stripMargin

    val expectedString =
      """|namespace foo
         |
         |structure Number with [NumberParentOne] {}
         |
         |@mixin
         |structure NumberParentOne with [NumberParentOneParent] {}
         |
         |@mixin
         |structure NumberParentOneParent {
         |    @required
         |    num: Integer,
         |}
         |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("allOf - document AND normal parent refs") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths: {}
                           |components:
                           |  schemas:
                           |    One:
                           |      description: one
                           |      type: object
                           |      properties:
                           |        o:
                           |          type: integer
                           |    Two:
                           |      description: two
                           |      type: object
                           |      properties:
                           |    Three:
                           |      description: three
                           |      type: object
                           |      allOf:
                           |        - $ref: "#/components/schemas/One"
                           |    Object:
                           |      description: object
                           |      allOf:
                           |        - $ref: "#/components/schemas/One"
                           |        - $ref: "#/components/schemas/Two"
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |/// object
                            |document Object
                            |
                            |/// one
                            |@mixin
                            |structure One {
                            |    o: Integer,
                            |}
                            |
                            |/// three
                            |structure Three with [One] {}
                            |
                            |/// two
                            |document Two
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

}
