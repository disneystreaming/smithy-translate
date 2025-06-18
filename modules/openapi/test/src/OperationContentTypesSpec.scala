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

final class OperationContentTypesSpec extends munit.FunSuite {

  test("operation - application/octet-stream") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths:
                           |  /test:
                           |    post:
                           |      operationId: testOperationId
                           |      requestBody:
                           |        required: true
                           |        content:
                           |          application/octet-stream:
                           |            schema:
                           |              type: string
                           |              format: binary
                           |      responses:
                           |        '200':
                           |          content:
                           |            application/octet-stream:
                           |              schema:
                           |                type: string
                           |                format: binary
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
                            |    @httpPayload
                            |    @required
                            |    @contentType("application/octet-stream")
                            |    body: Blob
                            |}
                            |
                            |structure TestOperationId200 {
                            |    @httpPayload
                            |    @required
                            |    @contentType("application/octet-stream")
                            |    body: Blob
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("operation - custom application/json") {
    val openapiString =
      """|openapi: '3.0.'
         |info:
         |  title: test
         |  version: '1.0'
         |paths:
         |  /test:
         |    post:
         |      operationId: testOperationId
         |      requestBody:
         |        required: true
         |        content:
         |          application/test-service+json; version=2:
         |            schema:
         |              $ref: '#/components/schemas/ObjectIn'
         |      responses:
         |        '200':
         |          content:
         |            application/test-service+json; version=2:
         |              schema:
         |                $ref: '#/components/schemas/ObjectOut'
         |components:
         |  schemas:
         |    ObjectIn:
         |      type: object
         |      properties:
         |        s:
         |          type: string
         |      required:
         |        - s
         |    ObjectOut:
         |      type: object
         |      properties:
         |        sNum:
         |          type: integer
         |""".stripMargin

    val expectedString =
      """|namespace foo
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
         |structure ObjectIn {
         |    @required
         |    s: String,
         |}
         |
         |structure ObjectOut {
         |    sNum: Integer,
         |}
         |
         |structure TestOperationId200 {
         |    @httpPayload
         |    @required
         |    @contentType("application/test-service+json; version=2")
         |    body: ObjectOut,
         |}
         |
         |structure TestOperationIdInput {
         |    @httpPayload
         |    @required
         |    @contentType("application/test-service+json; version=2")
         |    body: ObjectIn,
         |}
         |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("operation - multiple content types") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths:
                           |  /test:
                           |    post:
                           |      operationId: testOperationId
                           |      requestBody:
                           |        required: true
                           |        content:
                           |          application/octet-stream:
                           |            schema:
                           |              type: string
                           |              format: binary
                           |          application/json:
                           |            schema:
                           |              type: object
                           |              properties:
                           |                s:
                           |                  type: string
                           |      responses:
                           |        '200':
                           |          content:
                           |            application/octet-stream:
                           |              schema:
                           |                type: string
                           |                format: binary
                           |            application/json:
                           |              schema:
                           |                type: object
                           |                properties:
                           |                  s:
                           |                    type: string
                           |""".stripMargin

    val expectedString =
      """|namespace foo
         |
         |use smithytranslate#contentTypeDiscriminated
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
         |    @httpPayload
         |    @required
         |    body: TestOperationIdInputBody
         |}
         |
         |structure TestOperationId200 {
         |    @httpPayload
         |    @required
         |    body: TestOperationId200Body
         |}
         |
         |@contentTypeDiscriminated
         |union TestOperationId200Body {
         |    @contentType("application/octet-stream")
         |    applicationOctetStream: Blob,
         |    @contentType("application/json")
         |    applicationJson: TestOperationId200BodyApplicationJson
         |}
         |
         |structure TestOperationId200BodyApplicationJson {
         |    s: String
         |}
         |
         |@contentTypeDiscriminated
         |union TestOperationIdInputBody {
         |  @contentType("application/octet-stream")
         |  applicationOctetStream: Blob,
         |  @contentType("application/json")
         |  applicationJson: TestOperationIdInputBodyApplicationJson
         |}
         |
         |structure TestOperationIdInputBodyApplicationJson {
         |  s: String
         |}
         |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("operation - multiple content types with same target shape") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths:
                           |  /employees:
                           |    get:
                           |      responses:
                           |        '200':
                           |          content:
                           |            application/json:
                           |             schema:
                           |               $ref: '#/components/schemas/Employee'
                           |            application/xml:
                           |             schema:
                           |               $ref: '#/components/schemas/Employee'
                           |components:
                           |  schemas:
                           |    Employee:
                           |      type: object
                           |      properties:
                           |        id:
                           |          type: integer
                           |        name:
                           |          type: string
                           |        fullTime:
                           |          type: boolean
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use smithytranslate#contentType
                            |use smithytranslate#contentTypeDiscriminated
                            |
                            |service FooService {
                            |    operations: [
                            |        EmployeesGET,
                            |    ],
                            |}
                            |
                            |@http(
                            |    method: "GET",
                            |    uri: "/employees",
                            |    code: 200,
                            |)
                            |operation EmployeesGET {
                            |    input: Unit,
                            |    output: EmployeesGET200,
                            |}
                            |
                            |structure Employee {
                            |    id: Integer,
                            |    name: String,
                            |    fullTime: Boolean,
                            |}
                            |
                            |structure EmployeesGET200 {
                            |    @httpPayload
                            |    @required
                            |    body: Body,
                            |}
                            |
                            |@contentTypeDiscriminated
                            |union Body {
                            |    @contentType("application/json")
                            |    applicationJson: Employee,
                            |    @contentType("application/xml")
                            |    applicationXml: Employee,
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("operation - request and response with multiple content types") {
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
                           |              also:
                           |                type: integer
                           |        application/octet-stream:
                           |          schema:
                           |            type: string
                           |            format: binary
                           |  responses:
                           |    okay:
                           |      content:
                           |        application/json:
                           |          schema:
                           |            type: object
                           |            properties:
                           |              test:
                           |                type: string
                           |        application/octet-stream:
                           |          schema:
                           |            type: string
                           |            format: binary
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use smithytranslate#contentTypeDiscriminated
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
                            |@contentTypeDiscriminated
                            |union Generic {
                            |    @contentType("application/json")
                            |    applicationJson: GenericApplicationJson,
                            |    @contentType("application/octet-stream")
                            |    applicationOctetStream: Blob,
                            |}
                            |
                            |structure GenericApplicationJson {
                            |    myProperty: String,
                            |    also: Integer,
                            |}
                            |
                            |structure Okay {
                            |    @httpPayload
                            |    @required
                            |    body: Body,
                            |}
                            |
                            |@contentTypeDiscriminated
                            |union Body {
                            |    @contentType("application/json")
                            |    applicationJson: OkayBodyApplicationJson,
                            |    @contentType("application/octet-stream")
                            |    applicationOctetStream: Blob,
                            |}
                            |
                            |structure OkayBodyApplicationJson {
                            |    test: String,
                            |}
                            |
                            |structure TestOperationIdInput {
                            |    @httpPayload
                            |    @required
                            |    body: Generic,
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("operation - multiple content types in error response") {
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
                           |            application/octet-stream:
                           |              schema:
                           |                type: string
                           |                format: binary
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
                            |use smithytranslate#contentTypeDiscriminated
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
                            |    body: Body,
                            |}
                            |
                            |@contentTypeDiscriminated
                            |union Body {
                            |    @contentType("application/octet-stream")
                            |    applicationOctetStream: Blob,
                            |    @contentType("application/json")
                            |    applicationJson: ApplicationJson,
                            |}
                            |
                            |structure ApplicationJson {
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

  test("operation - multiple content types in success and error response") {
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
                           |            application/octet-stream:
                           |              schema:
                           |                type: string
                           |                format: binary
                           |            application/json:
                           |              schema:
                           |                $ref: '#/components/schemas/Object'
                           |        '404':
                           |          content:
                           |            application/octet-stream:
                           |              schema:
                           |                type: string
                           |                format: binary
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
                            |use smithytranslate#contentTypeDiscriminated
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
                            |    body: TestOperationId404Body,
                            |}
                            |
                            |@contentTypeDiscriminated
                            |union TestOperationId404Body {
                            |    @contentType("application/octet-stream")
                            |    applicationOctetStream: Blob,
                            |    @contentType("application/json")
                            |    applicationJson: ApplicationJson,
                            |}
                            |
                            |structure ApplicationJson {
                            |    message: String
                            |}
                            |
                            |structure TestOperationId200 {
                            |    @httpPayload
                            |    @required
                            |    body: TestOperationId200Body,
                            |}
                            |
                            |@contentTypeDiscriminated
                            |union TestOperationId200Body {
                            |   @contentType("application/octet-stream")
                            |  applicationOctetStream: Blob,
                            |  @contentType("application/json")
                            |  applicationJson: Object
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

}
