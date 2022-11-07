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

final class OperationHeaderSpec extends munit.FunSuite {

  test("operation - header") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths:
                     |  /test:
                     |    get:
                     |      operationId: testOperationId
                     |      parameters:
                     |        - in: header
                     |          name: X-username
                     |          schema:
                     |            type: string
                     |      responses:
                     |        '200':
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
                      |    input: TestOperationIdInput,
                      |    output: TestOperationId200,
                      |}
                      |
                      |structure TestOperationIdInput {
                      |    @httpHeader("X-username")
                      |    X_username: String,
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

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("operation - response header") {
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
                     |          headers:
                     |            X-RateLimit-Limit:
                     |              schema:
                     |                type: integer
                     |              description: Request limit per hour.
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
                      |    @httpHeader("X-RateLimit-Limit")
                      |    X_RateLimit_Limit: Integer,
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("operation - headers with body") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths:
                     |  /test:
                     |    post:
                     |      operationId: testOperationId
                     |      requestBody:
                     |        description: Optional description in *Markdown*
                     |        required: false
                     |        content:
                     |          application/json:
                     |            schema:
                     |              $ref: '#/components/schemas/Object'
                     |      parameters:
                     |        - in: header
                     |          name: X-username
                     |          schema:
                     |            type: string
                     |      responses:
                     |        '200':
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
                      |    method: "POST",
                      |    uri: "/test",
                      |    code: 200,
                      |)
                      |operation TestOperationId {
                      |    input: TestOperationIdInput,
                      |    output: TestOperationId200,
                      |}
                      |
                      |structure TestOperationIdInput {
                      |    @httpHeader("X-username")
                      |    X_username: String,
                      |    @httpPayload
                      |    @documentation("Optional description in *Markdown*")
                      |    @contentType("application/json")
                      |    body: Object
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

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("operation - header reference") {
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
                     |          headers:
                     |            X-Test-Header:
                     |              $ref: '#/components/headers/X-Test-Header'
                     |          content:
                     |            application/json:
                     |              schema:
                     |                $ref: '#/components/schemas/Object'
                     |components:
                     |  headers:
                     |    X-Test-Header:
                     |      schema:
                     |        type: integer
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
                      |    @httpHeader("X-Test-Header")
                      |    X_Test_Header: Integer
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("operation - header embedded in component response") {
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
                   |              s:
                   |                type: string
                   |            required:
                   |              - s
                   |  responses:
                   |    okay:
                   |      headers:
                   |        X-Test-Header:
                   |          schema:
                   |            type: integer
                   |      content:
                   |        application/json:
                   |          schema:
                   |            type: object
                   |            properties:
                   |              sNum:
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
                    |    method: "POST",
                    |    uri: "/test",
                    |    code: 200,
                    |)
                    |operation TestOperationId {
                    |    input: TestOperationIdInput,
                    |    output: Okay,
                    |}
                    |
                    |structure Okay {
                    |    @httpPayload
                    |    @required
                    |    @contentType("application/json")
                    |    body: Body,
                    |    @httpHeader("X-Test-Header")
                    |    X_Test_Header: Integer,
                    |}
                    |
                    |structure Body {
                    |    sNum: Integer,
                    |}
                    |
                    |structure Generic {
                    |    @required
                    |    s: String
                    |}
                    |
                    |structure TestOperationIdInput {
                    |    @httpPayload
                    |    @required
                    |    @contentType("application/json")
                    |    body: Generic
                    |}
                    |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

}
