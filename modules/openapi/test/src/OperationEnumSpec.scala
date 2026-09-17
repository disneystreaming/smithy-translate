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
import smithytranslate.compiler.FileContents
import smithytranslate.compiler.ToSmithyCompilerOptions
import smithytranslate.compiler.ToSmithyResult
import software.amazon.smithy.model.shapes.EnumShape
import software.amazon.smithy.model.shapes.ShapeId
import scala.jdk.CollectionConverters._

final class OperationEnumSpec extends munit.FunSuite {

  test("operation - request and response with enum") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths:
                           |  /test:
                           |    post:
                           |      operationId: testOperationId
                           |      requestBody:
                           |        $ref: '#/components/requestBodies/generic'
                           |      responses:
                           |        '200':
                           |          $ref: '#/components/responses/okay'
                           |components:
                           |  requestBodies:
                           |    generic:
                           |      required: true
                           |      content:
                           |        application/json:
                           |          schema:
                           |            type: object
                           |            properties:
                           |              myProperty:
                           |                type: string
                           |                enum:
                           |                  - one
                           |                  - two
                           |              also:
                           |                type: integer
                           |  responses:
                           |    okay:
                           |      content:
                           |        application/json:
                           |          schema:
                           |            type: object
                           |            properties:
                           |              test:
                           |                type: string
                           |                enum:
                           |                  - foo
                           |                  - bar
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use smithytranslate#contentType
                            |
                            |service FooService {
                            |    operations: [
                            |        TestOperationId,
                            |    ],
                            |}
                            |
                            |@http(
                            |    method: "POST",
                            |    uri: "/test",
                            |    code: 200,
                            |)
                            |operation TestOperationId {
                            |    input: TestOperationIdInput,
                            |    output: Okay,
                            |}
                            |
                            |structure Generic {
                            |    myProperty: MyProperty,
                            |    also: Integer,
                            |}
                            |
                            |structure Okay {
                            |    @httpPayload
                            |    @required
                            |    @contentType("application/json")
                            |    body: Body,
                            |}
                            |
                            |structure Body {
                            |    test: Test,
                            |}
                            |
                            |structure TestOperationIdInput {
                            |    @httpPayload
                            |    @required
                            |    @contentType("application/json")
                            |    body: Generic,
                            |}
                            |
                            |enum MyProperty {
                            |   one
                            |   two
                            |}
                            |
                            |
                            |enum Test {
                            |    foo
                            |    bar
                            |}
                            |
                            |""".stripMargin

    EnumTestUtils.runConversionTest(openapiString, expectedString)
  }

  test("operation - request and response with required enum field") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths:
                           |  /test:
                           |    post:
                           |      operationId: testOperationId
                           |      requestBody:
                           |        $ref: '#/components/requestBodies/generic'
                           |      responses:
                           |        '200':
                           |          $ref: '#/components/responses/okay'
                           |components:
                           |  requestBodies:
                           |    generic:
                           |      required: true
                           |      content:
                           |        application/json:
                           |          schema:
                           |            type: object
                           |            properties:
                           |              myProperty:
                           |                type: string
                           |                enum:
                           |                  - one
                           |                  - two
                           |              also:
                           |                type: integer
                           |            required:
                           |              - myProperty
                           |  responses:
                           |    okay:
                           |      content:
                           |        application/json:
                           |          schema:
                           |            type: object
                           |            properties:
                           |              test:
                           |                type: string
                           |                enum:
                           |                  - foo
                           |                  - bar
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use smithytranslate#contentType
                            |
                            |service FooService {
                            |    operations: [
                            |        TestOperationId,
                            |    ],
                            |}
                            |
                            |@http(
                            |    method: "POST",
                            |    uri: "/test",
                            |    code: 200,
                            |)
                            |operation TestOperationId {
                            |    input: TestOperationIdInput,
                            |    output: Okay,
                            |}
                            |
                            |structure Generic {
                            |    @required
                            |    myProperty: MyProperty,
                            |    also: Integer,
                            |}
                            |
                            |structure Okay {
                            |    @httpPayload
                            |    @required
                            |    @contentType("application/json")
                            |    body: Body,
                            |}
                            |
                            |structure Body {
                            |    test: Test,
                            |}
                            |
                            |structure TestOperationIdInput {
                            |    @httpPayload
                            |    @required
                            |    @contentType("application/json")
                            |    body: Generic,
                            |}
                            |
                            |enum MyProperty {
                            |   one
                            |   two
                            |}
                            |
                            |
                            |enum Test {
                            |    foo
                            |    bar
                            |}
                            |""".stripMargin

    EnumTestUtils.runConversionTest(openapiString, expectedString)
  }

  TestUtils.allVersions.foreach { version =>
    test(s"operation - validates enum parameters and bodies ($version)") {
      val openapiString =
        s"""|openapi: '$version'
            |info: {title: test, version: '1.0'}
            |paths:
            |  /colors:
            |    post:
            |      operationId: setColor
            |      parameters:
            |        - name: color
            |          in: query
            |          schema:
            |            $$ref: '#/components/schemas/Color'
            |      requestBody:
            |        required: true
            |        content:
            |          application/json:
            |            schema:
            |              type: object
            |              properties:
            |                shade:
            |                  type: string
            |                  enum: [light, dark]
            |      responses:
            |        '200':
            |          description: The selected color
            |          content:
            |            application/json:
            |              schema:
            |                $$ref: '#/components/schemas/Color'
            |components:
            |  schemas:
            |    Color:
            |      type: string
            |      enum: [red, green, blue]
            |""".stripMargin
      val result = OpenApiCompiler.compile(
        ToSmithyCompilerOptions(
          useVerboseNames = false,
          validateInput = true,
          validateOutput = true,
          transformers = List.empty,
          useEnumTraitSyntax = false,
          debug = false,
          allowedRemoteBaseURLs = Set.empty,
          namespaceRemaps = Map.empty
        ),
        OpenApiCompilerInput.UnparsedSpecs(
          List(FileContents(NonEmptyList.one("foo.yaml"), openapiString))
        )
      )

      result match {
        case ToSmithyResult.Success(errors, model) =>
          assertEquals(errors, Nil)
          val color =
            model.expectShape(ShapeId.from("foo#Color"), classOf[EnumShape])
          val shade =
            model.expectShape(ShapeId.from("foo#Shade"), classOf[EnumShape])
          assertEquals(
            color.getEnumValues.asScala.toMap,
            Map("red" -> "red", "green" -> "green", "blue" -> "blue")
          )
          assertEquals(
            shade.getEnumValues.asScala.toMap,
            Map("light" -> "light", "dark" -> "dark")
          )
        case ToSmithyResult.Failure(cause, errors) =>
          fail(s"Expected validated enum conversion: $errors", cause)
      }
    }
  }

}
