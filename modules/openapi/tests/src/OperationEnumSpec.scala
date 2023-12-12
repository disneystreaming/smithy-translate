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

    TestUtils.runConversionTest(openapiString, expectedString)
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

    TestUtils.runConversionTest(openapiString, expectedString)
  }

}
