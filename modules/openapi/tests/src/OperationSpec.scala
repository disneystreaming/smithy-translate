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

final class OperationSpec extends munit.FunSuite {

  test("operation - response") {
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

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("operation - request and response") {
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
                     |          application/json:
                     |            schema:
                     |              $ref: '#/components/schemas/ObjectIn'
                     |      responses:
                     |        '200':
                     |          content:
                     |            application/json:
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
                      |    @contentType("application/json")
                      |    body: ObjectOut,
                      |}
                      |
                      |structure TestOperationIdInput {
                      |    @httpPayload
                      |    @required
                      |    @contentType("application/json")
                      |    body: ObjectIn,
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("operation - request and response embedded schemas") {
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
                     |          application/json:
                     |            schema:
                     |              type: object
                     |              properties:
                     |                s:
                     |                  type: string
                     |              required:
                     |                - s
                     |      responses:
                     |        '200':
                     |          content:
                     |            application/json:
                     |              schema:
                     |                type: object
                     |                properties:
                     |                  sNum:
                     |                    type: integer
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
                      |  input: TestOperationIdInput,
                      |  output: TestOperationId200
                      |}
                      |
                      |structure TestOperationId200 {
                      |    @httpPayload
                      |    @required
                      |    @contentType("application/json")
                      |    body: TestOperationId200Body,
                      |}
                      |
                      |structure TestOperationId200Body {
                      |    sNum: Integer,
                      |}
                      |
                      |structure TestOperationIdInput {
                      |    @httpPayload
                      |    @required
                      |    @contentType("application/json")
                      |    body: TestOperationIdInputBody,
                      |}
                      |
                      |structure TestOperationIdInputBody {
                      |  @required
                      |  s: String
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("operation - path parameter") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths:
                     |  /test/{userId}:
                     |    get:
                     |      operationId: testOperationId
                     |      parameters:
                     |        - in: path
                     |          name: userId
                     |          schema:
                     |            type: integer
                     |          required: true
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
                      |    uri: "/test/{userId}",
                      |    code: 200,
                      |)
                      |operation TestOperationId {
                      |    input: TestOperationIdInput,
                      |    output: TestOperationId200,
                      |}
                      |
                      |structure TestOperationIdInput {
                      |    @httpLabel
                      |    @required
                      |    userId: Integer,
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

  test("operation - query") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths:
                     |  /test:
                     |    get:
                     |      operationId: testOperationId
                     |      parameters:
                     |        - in: query
                     |          name: userId
                     |          schema:
                     |            type: integer
                     |        - in: query
                     |          name: some_id
                     |          schema:
                     |            type: integer
                     |        - in: query
                     |          name: other-id
                     |          schema:
                     |            type: integer
                     |        - in: query
                     |          name: 12-twelve
                     |          schema:
                     |            type: integer
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
                      |    @httpQuery("userId")
                      |    userId: Integer,
                      |    @httpQuery("some_id")
                      |    some_id: Integer,
                      |    @httpQuery("other-id")
                      |    other_id: Integer
                      |    @httpQuery("12-twelve")
                      |    n12_twelve: Integer
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

  test("operation - response reference") {
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
                    |    body: Body,
                    |}
                    |
                    |structure Body {
                    |    @required
                    |    s: String,
                    |}
                    |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("operation - request reference") {
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
                    |structure Generic {
                    |    @required
                    |    s: String
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
                    |    sNum: Integer,
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

  test("operation - request and operation have the same name") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths:
                           |  /test2:
                           |    post:
                           |      operationId: testOperation2
                           |      requestBody:
                           |        $ref: '#/components/requestBodies/TestOperation2'
                           |      responses:
                           |        '200':
                           |          $ref: '#/components/responses/okay'
                           |  /test:
                           |    post:
                           |      operationId: testOperation
                           |      requestBody:
                           |        $ref: '#/components/requestBodies/testOperation'
                           |      responses:
                           |        '200':
                           |          $ref: '#/components/responses/okay'
                           |components:
                           |  requestBodies:
                           |    TestOperation2:
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
                           |    testOperation:
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
                            |        TestOperation,
                            |        TestOperation2,
                            |    ],
                            |}
                            |
                            |@http(
                            |    method: "POST",
                            |    uri: "/test",
                            |    code: 200,
                            |)
                            |operation TestOperation {
                            |    input: TestOperationInput,
                            |    output: Okay,
                            |}
                            |
                            |@http(
                            |    method: "POST",
                            |    uri: "/test2",
                            |    code: 200,
                            |)
                            |operation TestOperation2 {
                            |    input: TestOperation2Input,
                            |    output: Okay,
                            |}
                            |
                            |structure Body {
                            |    sNum: Integer,
                            |}
                            |
                            |structure ComponentsRequestBodiesTestOperation {
                            |    @required
                            |    s: String,
                            |}
                            |
                            |structure ComponentsRequestBodiesTestOperation2 {
                            |    @required
                            |    s: String,
                            |}
                            |
                            |structure Okay {
                            |    @httpPayload
                            |    @required
                            |    @contentType("application/json")
                            |    body: Body,
                            |}
                            |
                            |structure TestOperation2Input {
                            |    @httpPayload
                            |    @required
                            |    @contentType("application/json")
                            |    body: ComponentsRequestBodiesTestOperation2,
                            |}
                            |
                            |structure TestOperationInput {
                            |    @httpPayload
                            |    @required
                            |    @contentType("application/json")
                            |    body: ComponentsRequestBodiesTestOperation,
                            |}
                            |""".stripMargin
    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("operation - uses of a restricted header add suppression") {
    val openapiString = """|openapi: '3.0.'
                           |info:
                           |  title: test
                           |  version: '1.0'
                           |paths:
                           |  /test:
                           |    post:
                           |      operationId: testOperation
                           |      parameters:
                           |      - in: header
                           |        name: X-Request-Id
                           |        schema:
                           |          type: string
                           |      - in: header
                           |        name: X-Forwarded-For
                           |        schema:
                           |          type: string
                           |      requestBody:
                           |        $ref: '#/components/requestBodies/testOperation'
                           |      responses:
                           |        '200':
                           |          content: {}
                           |components:
                           |  requestBodies:
                           |    testOperation:
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
                           |""".stripMargin

    val expectedString = """|metadata suppressions = [
                            |  {
                            |        id: "HttpHeaderTrait",
                            |        namespace: "foo",
                            |        reason: "Restricted headers are in use. See https://awslabs.github.io/smithy/1.0/spec/core/http-traits.html#restricted-http-headers."
                            |    }
                            |]
                            |
                            |namespace foo
                            |
                            |use smithytranslate#contentType
                            |
                            |service FooService {
                            |    operations: [
                            |        TestOperation,
                            |    ],
                            |}
                            |
                            |@http(
                            |    method: "POST",
                            |    uri: "/test",
                            |    code: 200,
                            |)
                            |operation TestOperation {
                            |    input: TestOperationInput,
                            |    output: Unit,
                            |}
                            |
                            |structure ComponentsRequestBodiesTestOperation {
                            |    @required
                            |    s: String,
                            |}
                            |
                            |structure TestOperationInput {
                            |    @httpHeader("X-Request-Id")
                            |    X_Request_Id: String,
                            |    @httpHeader("X-Forwarded-For")
                            |    X_Forwarded_For: String,
                            |    @httpPayload
                            |    @required
                            |    @contentType("application/json")
                            |    body: ComponentsRequestBodiesTestOperation,
                            |}""".stripMargin
    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("operation - description") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths:
                     |  /test:
                     |    get:
                     |      operationId: testOperationId
                     |      description: Testing test
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
                      |@documentation("Testing test")
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

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("operation - simplify repeated namespace") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths:
                     |  /test/test:
                     |    get:
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
                      |        TestGET
                      |    ]
                      |}
                      |
                      |@http(
                      |    method: "GET",
                      |    uri: "/test/test",
                      |    code: 200,
                      |)
                      |operation TestGET {
                      |    input: Unit,
                      |    output: TestGET200,
                      |}
                      |
                      |structure Object {
                      |    @required
                      |    s: String,
                      |}
                      |
                      |structure TestGET200 {
                      |    @httpPayload
                      |    @required
                      |    @contentType("application/json")
                      |    body: Object,
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("operation - do not simplify repeated namespace") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths:
                     |  /test/test:
                     |    get:
                     |      responses:
                     |        '200':
                     |          content:
                     |            application/json:
                     |              schema:
                     |                $ref: '#/components/schemas/Object'
                     |  /test:
                     |    get:
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
                      |        TestGET,
                      |        TestTestGET
                      |    ]
                      |}
                      |
                      |@http(
                      |    method: "GET",
                      |    uri: "/test",
                      |    code: 200,
                      |)
                      |operation TestGET {
                      |    input: Unit,
                      |    output: TestGET200,
                      |}
                      |
                      |@http(
                      |    method: "GET",
                      |    uri: "/test/test",
                      |    code: 200,
                      |)
                      |operation TestTestGET {
                      |    input: Unit,
                      |    output: TestTestGET200,
                      |}
                      |
                      |structure Object {
                      |    @required
                      |    s: String,
                      |}
                      |
                      |structure TestGET200 {
                      |    @httpPayload
                      |    @required
                      |    @contentType("application/json")
                      |    body: Object,
                      |}
                      |
                      |structure TestTestGET200 {
                      |    @httpPayload
                      |    @required
                      |    @contentType("application/json")
                      |    body: Object,
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("operation - simplify repeated namespace embedded schema") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths:
                     |  /test/{test}:
                     |    get:
                     |      parameters:
                     |        - in: path
                     |          name: test
                     |          schema:
                     |            type: string
                     |          required: true
                     |      responses:
                     |        '200':
                     |          content:
                     |            application/json:
                     |              schema:
                     |                type: object
                     |                properties:
                     |                  s:
                     |                    type: string
                     |                required:
                     |                  - s
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |use smithytranslate#contentType
                      |
                      |service FooService {
                      |    operations: [
                      |        TestGET
                      |    ]
                      |}
                      |
                      |@http(
                      |    method: "GET",
                      |    uri: "/test/{test}",
                      |    code: 200,
                      |)
                      |operation TestGET {
                      |    input: TestGETInput,
                      |    output: TestGET200,
                      |}
                      |
                      |structure TestGETInput {
                      |  @httpLabel
                      |  @required
                      |  test: String
                      |}
                      |
                      |structure Body {
                      |    @required
                      |    s: String,
                      |}
                      |
                      |structure TestGET200 {
                      |    @httpPayload
                      |    @required
                      |    @contentType("application/json")
                      |    body: Body,
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

}
