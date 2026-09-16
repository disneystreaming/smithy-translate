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

import TestUtils.OpenApiVersion.V3_1

final class PrimitiveSpec extends munit.FunSuite {

  test("primitive reference") {
    val openapiString =
      """|openapi: '3.0.'
         |info:
         |  title: test
         |  version: '1.0'
         |paths: {}
         |components:
         |  schemas:
         |    Object.MyString:
         |      type: string
         |    Object:
         |      type: object
         |      properties:
         |        s:
         |          $ref: '#/components/schemas/Object.MyString'
         |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Object {
                            | s: ObjectMyString
                            |}
                            |
                            |string ObjectMyString
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("primitive top level type with description") {
    val openapiString =
      """|openapi: '3.0.'
         |info:
         |  title: test
         |  version: '1.0'
         |paths: {}
         |components:
         |  schemas:
         |    Object.MyString:
         |      type: string
         |      description: MyString is a top-level primitive.
         |    Object:
         |      type: object
         |      properties:
         |        s:
         |          $ref: '#/components/schemas/Object.MyString'
         |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Object {
                            | s: ObjectMyString
                            |}
                            |
                            |@documentation("MyString is a top-level primitive.")
                            |string ObjectMyString
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("primitive top level type with description - external docs") {
    val openapiString =
      """|openapi: '3.0.'
         |info:
         |  title: test
         |  version: '1.0'
         |paths: {}
         |components:
         |  schemas:
         |    Object.MyString:
         |      type: string
         |      description: MyString is a top-level primitive.
         |      externalDocs:
         |        description: Example
         |        url: https://www.example.com
         |    Object:
         |      type: object
         |      properties:
         |        s:
         |          $ref: '#/components/schemas/Object.MyString'
         |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Object {
                            | s: ObjectMyString
                            |}
                            |
                            |@externalDocumentation(
                            |  "Example": "https://www.example.com"
                            |)
                            |@documentation("MyString is a top-level primitive.")
                            |string ObjectMyString
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }
  List(
    "integer" -> "integer",
    "integer, format: int16" -> "short",
    "integer, format: int32" -> "integer",
    "integer, format: int64" -> "long",
    "number" -> "double",
    "number, format: float" -> "float",
    "number, format: double" -> "double",
    "boolean" -> "boolean",
    "string, format: timestamp" -> "timestamp",
    "string, format: email" -> "string",
    "string, format: custom" -> "string"
  ).foreach { case (schema, smithyType) =>
    test(s"primitive $schema: named, inline and referenced") {
      val openapiString = s"""|openapi: '3.0.3'
                              |info: {title: test, version: '1.0'}
                              |paths: {}
                              |components:
                              |  schemas:
                              |    Scalar: {type: $schema}
                              |    Values:
                              |      type: object
                              |      properties:
                              |        inline: {type: $schema}
                              |        reference:
                              |          $$ref: '#/components/schemas/Scalar'
                              |""".stripMargin
      val expectedString = s"""|namespace foo
                               |$smithyType Scalar
                               |structure Values {
                               |  inline: ${smithyType.capitalize}
                               |  reference: Scalar
                               |}
                               |""".stripMargin
      TestUtils.runConversionTest(openapiString, expectedString)
    }
  }

  List("string", "integer", "number", "boolean").foreach { dataType =>
    test(s"OpenAPI 3.1 singleton type array: $dataType") {
      val openapiString = s"""|openapi: '3.1.0'
                              |info: {title: test, version: '1.0'}
                              |paths: {}
                              |components:
                              |  schemas:
                              |    Scalar: {type: [$dataType]}
                              |""".stripMargin
      val smithyType = if (dataType == "number") "double" else dataType
      TestUtils.runConversionTest(
        openapiString,
        s"namespace foo\n$smithyType Scalar",
        versions = List(V3_1)
      )
    }
  }
}
