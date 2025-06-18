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
import smithytranslate.compiler.ToSmithyResult
import smithytranslate.compiler.ToSmithyError

final class OperationMultipleSuccessSpec extends munit.FunSuite {

  test("operation - multiple success responses") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths:
                           |  /test:
                           |    get:
                           |      operationId: testOperationId
                           |      responses:
                           |        '200':
                           |          content:
                           |            application/json:
                           |              schema:
                           |                $ref: '#/components/schemas/Object'
                           |        '202':
                           |          content:
                           |            application/json:
                           |              schema:
                           |                $ref: '#/components/schemas/Object'
                           |components:
                           |  schemas:
                           |    Object:
                           |      type: object
                           |      properties:
                           |        s:
                           |          type: string
                           |      required:
                           |        - s
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use smithytranslate#contentType
                            |
                            |service FooService {
                            |    operations: [
                            |        TestOperationId
                            |    ]
                            |}
                            |
                            |@http(
                            |    method: "GET",
                            |    uri: "/test",
                            |    code: 200,
                            |)
                            |operation TestOperationId {
                            |    input: Unit,
                            |    output: TestOperationId200,
                            |}
                            |
                            |structure Object {
                            |    @required
                            |    s: String,
                            |}
                            |
                            |structure TestOperationId200 {
                            |    @httpPayload
                            |    @required
                            |    @contentType("application/json")
                            |    body: Object,
                            |}
                            |""".stripMargin

    val expectedError =
      """|namespace error
         |
         |use smithytranslate#errorMessage
         |use foo#Object
         |use smithytranslate#contentType
         |
         |@errorMessage("Multiple success responses are not supported. Found status code 202 when 200 was already recorded")
         |structure TestOperationId202 {
         |    @httpPayload
         |    @required
         |    @contentType("application/json")
         |    body: Object,
         |}
         |""".stripMargin

    val input = TestUtils.ConversionTestInput(
      NonEmptyList.of("foo.yaml"),
      openapiString,
      expectedString,
      Some(expectedError)
    )
    val TestUtils.ConversionResult(
      ToSmithyResult.Success(errors, output),
      expectedModel
    ) =
      TestUtils.runConversion(input)
    val expectedErrors = List(
      ToSmithyError.Restriction(
        "Multiple success responses are not supported. Found status code 202 when 200 was already recorded"
      )
    )
    assertEquals(errors, expectedErrors)
    assertEquals(output, expectedModel)
  }

  test("operation - multiple success responses with references") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths:
                           |  /test:
                           |    get:
                           |      operationId: testOperationId
                           |      responses:
                           |        '200':
                           |          $ref: '#/components/responses/okay'
                           |        '202':
                           |          $ref: '#/components/responses/alsoOkay'
                           |components:
                           |  responses:
                           |    okay:
                           |      content:
                           |        application/json:
                           |          schema:
                           |            type: object
                           |            properties:
                           |              s:
                           |                type: string
                           |            required:
                           |              - s
                           |    alsoOkay:
                           |      content:
                           |        application/json:
                           |          schema:
                           |            type: object
                           |            properties:
                           |              i:
                           |                type: integer
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use smithytranslate#contentType
                            |
                            |service FooService {
                            |    operations: [
                            |        TestOperationId
                            |    ]
                            |}
                            |
                            |@http(
                            |    method: "GET",
                            |    uri: "/test",
                            |    code: 200,
                            |)
                            |operation TestOperationId {
                            |    input: Unit,
                            |    output: Okay,
                            |}
                            |
                            |structure Okay {
                            |    @httpPayload
                            |    @required
                            |    @contentType("application/json")
                            |    body: OkayBody,
                            |}
                            |
                            |structure OkayBody {
                            |    @required
                            |    s: String,
                            |}
                            |
                            |structure AlsoOkay {
                            |    @httpPayload
                            |    @required
                            |    @contentType("application/json")
                            |    body: AlsoOkayBody,
                            |}
                            |
                            |structure AlsoOkayBody {
                            |    i: Integer,
                            |}
                            |""".stripMargin

    val input = TestUtils.ConversionTestInput(
      NonEmptyList.of("foo.yaml"),
      openapiString,
      expectedString,
      None // no error namespace shapes because `alsoOkay` is defined as a reusable response
    )
    val TestUtils.ConversionResult(
      ToSmithyResult.Success(errors, output),
      expectedModel
    ) =
      TestUtils.runConversion(input)
    val expectedErrors = List(
      ToSmithyError.Restriction(
        "Multiple success responses are not supported. Found status code 202 when 200 was already recorded"
      )
    )
    assertEquals(errors, expectedErrors)
    assertEquals(output, expectedModel)
  }

}
