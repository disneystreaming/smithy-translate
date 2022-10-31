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
final class OpenapiV2Spec extends munit.FunSuite {

  test("operation - simple response") {
    val openapiV2String = """|swagger: '2.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |consumes:
                     |  - application/json; charset=utf-8
                     |produces:
                     |  - application/json; charset=utf-8
                     |paths:
                     |  /test:
                     |    get:
                     |      operationId: testOperationId
                     |      responses:
                     |        '200':
                     |           schema:
                     |             $ref: '#/definitions/Object'
                     |definitions:
                     |  Object:
                     |    type: object
                     |    properties:
                     |      s:
                     |        type: string
                     |    required:
                     |      - s
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |use smithytranslate#openapiExtensions
                      |use smithytranslate#contentType
                      |
                      |@openapiExtensions(
                      |  "x-original-swagger-version": "2.0."
                      |)
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
                      |    @contentType("application/json; charset=utf-8")
                      |    body: Object,
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiV2String, expectedString)
  }

}
