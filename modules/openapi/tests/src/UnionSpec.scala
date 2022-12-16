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
final class UnionSpec extends munit.FunSuite {

  test("unions - primitive targets") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    TestUnion:
                     |      oneOf:
                     |        - type: string
                     |        - type: integer
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |use alloy#untagged
                      |
                      |@untagged
                      |union TestUnion {
                      |    String: String,
                      |    Integer: Integer
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("unions - primitive targets - description") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    TestUnion:
                     |      description: Test
                     |      oneOf:
                     |        - type: string
                     |        - type: integer
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |use alloy#untagged
                      |
                      |@untagged
                      |@documentation("Test")
                      |union TestUnion {
                      |    String: String,
                      |    Integer: Integer
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("unions - structure targets") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Cat:
                     |      type: object
                     |      properties:
                     |        name:
                     |          type: string
                     |    Dog:
                     |      type: object
                     |      properties:
                     |        breed:
                     |          type: string
                     |    TestUnion:
                     |      oneOf:
                     |        - $ref: '#/components/schemas/Cat'
                     |        - $ref: '#/components/schemas/Dog'
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |use alloy#untagged
                      |
                      |structure Cat {
                      |    name: String
                      |}
                      |
                      |structure Dog {
                      |    breed: String
                      |}
                      |
                      |@untagged
                      |union TestUnion {
                      |    Cat: Cat,
                      |    Dog: Dog
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("unions - structure targets - one embedded") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Cat:
                     |      type: object
                     |      properties:
                     |        name:
                     |          type: string
                     |    Dog:
                     |      type: object
                     |      properties:
                     |        breed:
                     |          type: string
                     |    TestUnion:
                     |      oneOf:
                     |        - $ref: '#/components/schemas/Cat'
                     |        - type: object
                     |          properties:
                     |            s:
                     |              type: string
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |use alloy#untagged
                      |
                      |structure Cat {
                      |    name: String
                      |}
                      |
                      |structure Dog {
                      |    breed: String
                      |}
                      |
                      |@untagged
                      |union TestUnion {
                      |    Cat: Cat,
                      |    alt1: TestUnionOneOfAlt1
                      |}
                      |
                      |structure TestUnionOneOfAlt1 {
                      |    s: String,
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("unions - untagged due to same field names") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Number:
                     |      type: object
                     |      properties:
                     |        value:
                     |          type: integer
                     |      required:
                     |        - value
                     |    Text:
                     |      type: object
                     |      properties:
                     |        value:
                     |          type: string
                     |      required:
                     |        - value
                     |    TestUnion:
                     |      oneOf:
                     |        - $ref: '#/components/schemas/Number'
                     |        - $ref: '#/components/schemas/Text'
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |use alloy#untagged
                      |
                      |structure Number {
                      |    @required
                      |    value: Integer
                      |}
                      |
                      |structure Text {
                      |    @required
                      |    value: String
                      |}
                      |
                      |@untagged
                      |union TestUnion {
                      |    Number: Number,
                      |    Text: Text
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("unions - tagged") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Number:
                     |      type: object
                     |      properties:
                     |        num:
                     |          type: integer
                     |      required:
                     |        - num
                     |    Text:
                     |      type: object
                     |      properties:
                     |        txt:
                     |          type: string
                     |      required:
                     |        - txt
                     |    TestUnion:
                     |      oneOf:
                     |        - $ref: '#/components/schemas/Number'
                     |        - $ref: '#/components/schemas/Text'
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |structure Number {
                      |    @required
                      |    num: Integer,
                      |}
                      |
                      |structure Text {
                      |    @required
                      |    txt: String,
                      |}
                      |
                      |union TestUnion {
                      |    num: Integer,
                      |    txt: String
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("unions - tagged with parent fields") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    NumberParentOne:
                     |      type: object
                     |      properties:
                     |        num:
                     |          type: integer
                     |      required:
                     |        - num
                     |    Number:
                     |      allOf:
                     |        - $ref: '#/components/schemas/NumberParentOne'
                     |    Text:
                     |      type: object
                     |      properties:
                     |        txt:
                     |          type: string
                     |      required:
                     |        - txt
                     |    TestUnion:
                     |      oneOf:
                     |        - $ref: '#/components/schemas/Number'
                     |        - $ref: '#/components/schemas/Text'
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |structure Number {
                      |    @required
                      |    num: Integer,
                      |}
                      |
                      |structure NumberParentOne {
                      |    @required
                      |    num: Integer,
                      |}
                      |
                      |structure Text {
                      |    @required
                      |    txt: String,
                      |}
                      |
                      |union TestUnion {
                      |    num: Integer,
                      |    txt: String
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("unions - tagged with multiple layers of parent fields") {
    val openapiString = """|openapi: '3.0.'
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
                     |    Text:
                     |      type: object
                     |      properties:
                     |        txt:
                     |          type: string
                     |      required:
                     |        - txt
                     |    TestUnion:
                     |      oneOf:
                     |        - $ref: '#/components/schemas/Number'
                     |        - $ref: '#/components/schemas/Text'
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |structure Number {
                      |    @required
                      |    num: Integer,
                      |}
                      |
                      |structure Text {
                      |    @required
                      |    txt: String,
                      |}
                      |
                      |structure NumberParentOne {
                      |    @required
                      |    num: Integer,
                      |}
                      |
                      |structure NumberParentOneParent {
                      |    @required
                      |    num: Integer,
                      |}
                      |
                      |union TestUnion {
                      |    num: Integer,
                      |    txt: String
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("unions - discriminated targets") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Cat:
                     |      type: object
                     |      properties:
                     |        name:
                     |          type: string
                     |        pet_type:
                     |          type: string
                     |    Dog:
                     |      type: object
                     |      properties:
                     |        breed:
                     |          type: string
                     |        pet_type:
                     |          type: string
                     |    TestUnion:
                     |      oneOf:
                     |        - $ref: '#/components/schemas/Cat'
                     |        - $ref: '#/components/schemas/Dog'
                     |      discriminator:
                     |        propertyName: pet_type
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |use alloy#discriminated
                      |
                      |structure Cat {
                      |    name: String,
                      |}
                      |
                      |structure Dog {
                      |    breed: String,
                      |}
                      |
                      |@discriminated("pet_type")
                      |union TestUnion {
                      |    Cat: Cat,
                      |    Dog: Dog
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("unions - allOf containing document") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    NumberParentOne:
                     |      type: object
                     |      properties:
                     |        num:
                     |          type: integer
                     |      required:
                     |        - num
                     |    NumberParentTwo:
                     |      type: object
                     |      properties:
                     |    Number:
                     |      allOf:
                     |        - $ref: '#/components/schemas/NumberParentOne'
                     |        - $ref: '#/components/schemas/NumberParentTwo'
                     |    Text:
                     |      type: object
                     |      properties:
                     |        txt:
                     |          type: string
                     |      required:
                     |        - txt
                     |    TestUnion:
                     |      oneOf:
                     |        - $ref: '#/components/schemas/Number'
                     |        - $ref: '#/components/schemas/Text'
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |use alloy#untagged
                      |
                      |structure NumberParentOne {
                      |    @required
                      |    num: Integer,
                      |}
                      |
                      |structure Text {
                      |    @required
                      |    txt: String,
                      |}
                      |
                      |@untagged
                      |union TestUnion {
                      |    Number: Number,
                      |    Text: Text
                      |}
                      |
                      |document Number
                      |
                      |document NumberParentTwo
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("unions - sanitize names") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Number:
                     |      type: object
                     |      properties:
                     |        3three:
                     |          type: integer
                     |      required:
                     |        - 3three
                     |    Text:
                     |      type: object
                     |      properties:
                     |        version1.1:
                     |          type: string
                     |      required:
                     |        - version1.1
                     |    TestUnion:
                     |      oneOf:
                     |        - $ref: '#/components/schemas/Number'
                     |        - $ref: '#/components/schemas/Text'
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |structure Number {
                      |    @required
                      |    @jsonName("3three")
                      |    n3three: Integer,
                      |}
                      |
                      |structure Text {
                      |    @required
                      |    @jsonName("version1.1")
                      |    version11: String,
                      |}
                      |
                      |union TestUnion {
                      |    @jsonName("3three")
                      |    n3three: Integer,
                      |    @jsonName("version1.1")
                      |    version11: String
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }
}
