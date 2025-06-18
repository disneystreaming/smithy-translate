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

final class OperationErrorSpec extends munit.FunSuite {

  test("operation - error response") {
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
                           |        '404':
                           |          content:
                           |            application/json:
                           |              schema:
                           |                type: object
                           |                properties:
                           |                  message:
                           |                    type: string
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
                            |    errors: [TestOperationId404]
                            |}
                            |
                            |structure Object {
                            |    @required
                            |    s: String,
                            |}
                            |
                            |@error("client")
                            |@httpError(404)
                            |structure TestOperationId404 {
                            |    @httpPayload
                            |    @required
                            |    @contentType("application/json")
                            |    body: Body,
                            |}
                            |
                            |structure Body {
                            |    message: String
                            |}
                            |
                            |structure TestOperationId200 {
                            |    @httpPayload
                            |    @required
                            |    @contentType("application/json")
                            |    body: Object,
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("operation - multiple error responses") {
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
                           |        '404':
                           |          content:
                           |            application/json:
                           |              schema:
                           |                type: object
                           |                properties:
                           |                  message:
                           |                    type: string
                           |        '500':
                           |          content:
                           |            application/json:
                           |              schema:
                           |                type: object
                           |                properties:
                           |                  message:
                           |                    type: string
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
                            |    errors: [TestOperationId404, TestOperationId500]
                            |}
                            |
                            |structure Object {
                            |    @required
                            |    s: String,
                            |}
                            |
                            |@error("client")
                            |@httpError(404)
                            |structure TestOperationId404 {
                            |    @httpPayload
                            |    @required
                            |    @contentType("application/json")
                            |    body: TestOperationId404Body,
                            |}
                            |
                            |structure TestOperationId404Body {
                            |    message: String
                            |}
                            |
                            |@error("server")
                            |@httpError(500)
                            |structure TestOperationId500 {
                            |    @httpPayload
                            |    @required
                            |    @contentType("application/json")
                            |    body: TestOperationId500Body,
                            |}
                            |
                            |structure TestOperationId500Body {
                            |    message: String
                            |}
                            |
                            |structure TestOperationId200 {
                            |    @httpPayload
                            |    @required
                            |    @contentType("application/json")
                            |    body: Object,
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("operation - error response schema ref") {
    val openapiString =
      """|openapi: '3.0.3'
         |info:
         |  title: test
         |  version: '1.0'
         |paths:
         |  /test:
         |    get:
         |      operationId: testOperationId
         |      responses:
         |        '200':
         |          description: test
         |          content: {}
         |        '401':
         |          $ref: '#/components/responses/AResponse'
         |        '402':
         |          description: test
         |          content:
         |            application/json:
         |              schema:
         |                $ref: '#/components/schemas/AResponse2'
         |components:
         |  responses:
         |    AResponse:
         |      description: test
         |      content:
         |        application/json:
         |          schema:
         |            type: object
         |            properties:
         |              s:
         |                type: string
         |            required:
         |              - s
         |  schemas:
         |    AResponse2:
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
                            |        TestOperationId,
                            |    ],
                            |}
                            |
                            |@http(
                            |    method: "GET",
                            |    uri: "/test",
                            |    code: 200,
                            |)
                            |operation TestOperationId {
                            |    input: Unit,
                            |    output: Unit,
                            |    errors: [
                            |        AResponse,
                            |        TestOperationId402,
                            |    ],
                            |}
                            |
                            |@error("client")
                            |@httpError(401)
                            |structure AResponse {
                            |    @httpPayload
                            |    @required
                            |    @contentType("application/json")
                            |    body: Body,
                            |}
                            |
                            |structure AResponse2 {
                            |    @required
                            |    s: String,
                            |}
                            |
                            |structure Body {
                            |    @required
                            |    s: String,
                            |}
                            |
                            |@error("client")
                            |@httpError(402)
                            |structure TestOperationId402 {
                            |    @httpPayload
                            |    @required
                            |    @contentType("application/json")
                            |    body: AResponse2,
                            |}""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

}
