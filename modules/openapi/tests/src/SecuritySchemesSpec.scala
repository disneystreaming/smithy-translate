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

import cats.data.NonEmptyList

final class SecuritySchemesSpec extends munit.FunSuite {

  test("security schemes - apiKey - whole service") {
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
                     |  securitySchemes:
                     |    ApiKeyAuth:
                     |      type: apiKey
                     |      in: header
                     |      name: X-API-Key
                     |security:
                     |  - ApiKeyAuth: []
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |use smithytranslate#contentType
                      |
                      |@httpApiKeyAuth(name: "X-API-Key", in: "header")
                      |@auth([httpApiKeyAuth])
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

  test("security schemes - bearer - whole service") {
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
                     |  securitySchemes:
                     |    BearerAuth:
                     |      type: http
                     |      scheme: bearer
                     |      bearerFormat: JWT
                     |security:
                     |  - BearerAuth: []
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |use smithytranslate#contentType
                      |
                      |@httpBearerAuth
                      |@documentation("Bearer Format: JWT")
                      |@auth([httpBearerAuth])
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

  test("security schemes - basic - whole service") {
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
                     |  securitySchemes:
                     |    BasicAuth:
                     |      type: http
                     |      scheme: basic
                     |security:
                     |  - BasicAuth: []
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |use smithytranslate#contentType
                      |
                      |@httpBasicAuth
                      |@auth([httpBasicAuth])
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

  test("security schemes - different auth types for diff operations") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths:
                     |  /test:
                     |    get:
                     |      operationId: testOperationId
                     |      security:
                     |        - BasicAuth: []
                     |      responses:
                     |        '200':
                     |          content:
                     |            application/json:
                     |              schema:
                     |                $ref: '#/components/schemas/Object'
                     |  /other:
                     |    get:
                     |      operationId: other
                     |      security:
                     |        - BearerAuth: []
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
                     |  securitySchemes:
                     |    BasicAuth:
                     |      type: http
                     |      scheme: basic
                     |    BearerAuth:
                     |      type: http
                     |      scheme: bearer
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |use smithytranslate#contentType
                      |
                      |@httpBasicAuth
                      |@httpBearerAuth
                      |service FooService {
                      |    operations: [
                      |        TestOperationId,
                      |        Other
                      |    ]
                      |}
                      |
                      |@http(
                      |    method: "GET",
                      |    uri: "/test",
                      |    code: 200,
                      |)
                      |@auth([httpBasicAuth])
                      |operation TestOperationId {
                      |    input: Unit,
                      |    output: TestOperationId200,
                      |}
                      |
                      |@http(
                      |    method: "GET",
                      |    uri: "/other",
                      |    code: 200,
                      |)
                      |@auth([httpBearerAuth])
                      |operation Other {
                      |    input: Unit,
                      |    output: Other200,
                      |}
                      |
                      |structure Object {
                      |    s: String,
                      |}
                      |
                      |structure TestOperationId200 {
                      |    @httpPayload
                      |    @required
                      |    @contentType("application/json")
                      |    body: Object,
                      |}
                      |
                      |structure Other200 {
                      |    @httpPayload
                      |    @required
                      |    @contentType("application/json")
                      |    body: Object,
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("security schemes - one operation using default, one using specific") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths:
                     |  /test:
                     |    get:
                     |      operationId: testOperationId
                     |      security:
                     |        - BasicAuth: []
                     |      responses:
                     |        '200':
                     |          content:
                     |            application/json:
                     |              schema:
                     |                $ref: '#/components/schemas/Object'
                     |  /other:
                     |    get:
                     |      operationId: other
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
                     |  securitySchemes:
                     |    BasicAuth:
                     |      type: http
                     |      scheme: basic
                     |    BearerAuth:
                     |      type: http
                     |      scheme: bearer
                     |security:
                     |  - BearerAuth: []
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |use smithytranslate#contentType
                      |
                      |@httpBasicAuth
                      |@httpBearerAuth
                      |@auth([httpBearerAuth])
                      |service FooService {
                      |    operations: [
                      |        TestOperationId,
                      |        Other
                      |    ]
                      |}
                      |
                      |@http(
                      |    method: "GET",
                      |    uri: "/test",
                      |    code: 200,
                      |)
                      |@auth([httpBasicAuth])
                      |operation TestOperationId {
                      |    input: Unit,
                      |    output: TestOperationId200,
                      |}
                      |
                      |@http(
                      |    method: "GET",
                      |    uri: "/other",
                      |    code: 200,
                      |)
                      |
                      |operation Other {
                      |    input: Unit,
                      |    output: Other200,
                      |}
                      |
                      |structure Object {
                      |    s: String,
                      |}
                      |
                      |structure TestOperationId200 {
                      |    @httpPayload
                      |    @required
                      |    @contentType("application/json")
                      |    body: Object,
                      |}
                      |
                      |structure Other200 {
                      |    @httpPayload
                      |    @required
                      |    @contentType("application/json")
                      |    body: Object,
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("security schemes - one of operations has no auth") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths:
                     |  /test:
                     |    get:
                     |      operationId: testOperationId
                     |      security:
                     |        - BasicAuth: []
                     |      responses:
                     |        '200':
                     |          content:
                     |            application/json:
                     |              schema:
                     |                $ref: '#/components/schemas/Object'
                     |  /other:
                     |    get:
                     |      operationId: other
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
                     |  securitySchemes:
                     |    BasicAuth:
                     |      type: http
                     |      scheme: basic
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |use smithytranslate#contentType
                      |
                      |@httpBasicAuth
                      |service FooService {
                      |    operations: [
                      |        TestOperationId,
                      |        Other
                      |    ]
                      |}
                      |
                      |@http(
                      |    method: "GET",
                      |    uri: "/test",
                      |    code: 200,
                      |)
                      |@auth([httpBasicAuth])
                      |operation TestOperationId {
                      |    input: Unit,
                      |    output: TestOperationId200,
                      |}
                      |
                      |@http(
                      |    method: "GET",
                      |    uri: "/other",
                      |    code: 200,
                      |)
                      |@auth([])
                      |operation Other {
                      |    input: Unit,
                      |    output: Other200,
                      |}
                      |
                      |structure Object {
                      |    s: String,
                      |}
                      |
                      |structure TestOperationId200 {
                      |    @httpPayload
                      |    @required
                      |    @contentType("application/json")
                      |    body: Object,
                      |}
                      |
                      |structure Other200 {
                      |    @httpPayload
                      |    @required
                      |    @contentType("application/json")
                      |    body: Object,
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("security schemes - ANDing security requirements") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths:
                     |  /test:
                     |    get:
                     |      operationId: testOperationId
                     |      security:
                     |        - BasicAuth: []
                     |          BearerAuth: []
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
                     |  securitySchemes:
                     |    BasicAuth:
                     |      type: http
                     |      scheme: basic
                     |    BearerAuth:
                     |      type: http
                     |      scheme: bearer
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |use smithytranslate#contentType
                      |
                      |@httpBasicAuth
                      |@httpBearerAuth
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
                      |@auth([httpBasicAuth])
                      |operation TestOperationId {
                      |    input: Unit,
                      |    output: TestOperationId200,
                      |}
                      |
                      |structure Object {
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

    val input = TestUtils.ConversionTestInput(
      NonEmptyList.of("foo"),
      openapiString,
      expectedString,
      None
    )
    val TestUtils.ConversionResult(
      OpenApiCompiler.Success(errors, output),
      expected
    ) =
      TestUtils.runConversion(input)
    val expectedError = ModelError.Restriction(
      "Operation testOperationId contains an unsupported security requirement: `List(BasicAuth, BearerAuth)`. " +
        "Security schemes cannot be ANDed together. BasicAuth will be used and List(BearerAuth) will be ignored."
    )
    assertEquals(output, expected)
    assertEquals(errors, List(expectedError))
  }

  test("security schemes - ORing security requirements") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths:
                     |  /test:
                     |    get:
                     |      operationId: testOperationId
                     |      security:
                     |        - BasicAuth: []
                     |        - BearerAuth: []
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
                     |  securitySchemes:
                     |    BasicAuth:
                     |      type: http
                     |      scheme: basic
                     |    BearerAuth:
                     |      type: http
                     |      scheme: bearer
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |use smithytranslate#contentType
                      |
                      |@httpBasicAuth
                      |@httpBearerAuth
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
                      |@auth([httpBasicAuth, httpBearerAuth])
                      |operation TestOperationId {
                      |    input: Unit,
                      |    output: TestOperationId200,
                      |}
                      |
                      |structure Object {
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

  test("security schemes - ANDing and ORing security requirements") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths:
                     |  /test:
                     |    get:
                     |      operationId: testOperationId
                     |      security:
                     |        - BasicAuth: []
                     |          BearerAuth: []
                     |        - BearerAuth: []
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
                     |  securitySchemes:
                     |    BasicAuth:
                     |      type: http
                     |      scheme: basic
                     |    BearerAuth:
                     |      type: http
                     |      scheme: bearer
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |use smithytranslate#contentType
                      |
                      |@httpBasicAuth
                      |@httpBearerAuth
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
                      |@auth([httpBasicAuth, httpBearerAuth])
                      |operation TestOperationId {
                      |    input: Unit,
                      |    output: TestOperationId200,
                      |}
                      |
                      |structure Object {
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

    val input = TestUtils.ConversionTestInput(
      NonEmptyList.of("foo"),
      openapiString,
      expectedString,
      None
    )
    val TestUtils.ConversionResult(
      OpenApiCompiler.Success(errors, output),
      expected
    ) =
      TestUtils.runConversion(input)
    val expectedError = ModelError.Restriction(
      "Operation testOperationId contains an unsupported security requirement: `List(BasicAuth, BearerAuth)`. " +
        "Security schemes cannot be ANDed together. BasicAuth will be used and List(BearerAuth) will be ignored."
    )
    assertEquals(output, expected)
    assertEquals(errors, List(expectedError))
  }

  test("security schemes - OAuth2/OpenIdConnect - errors") {
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
                     |  securitySchemes:
                     |    OpenID:
                     |      type: openIdConnect
                     |      openIdConnectUrl: https://example.com/.well-known/openid-configuration
                     |    OAuth2:
                     |      type: oauth2
                     |      flows:
                     |        authorizationCode:
                     |          authorizationUrl: https://example.com/oauth/authorize
                     |          tokenUrl: https://example.com/oauth/token
                     |          scopes:
                     |            read: Grants read access
                     |            write: Grants write access
                     |            admin: Grants access to admin operations
                     |security:
                     |  - OAuth2:
                     |      - read
                     |      - write
                     |  - OpenID:
                     |    - admin
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

    val input = TestUtils.ConversionTestInput(
      NonEmptyList.of("foo"),
      openapiString,
      expectedString,
      None
    )
    val TestUtils.ConversionResult(
      OpenApiCompiler.Success(errors, output),
      expected
    ) =
      TestUtils.runConversion(input)
    val expectedErrors = List(
      ModelError.Restriction(
        "OpenIdConnect is not a supported security scheme."
      ),
      ModelError.Restriction("OAuth2 is not a supported security scheme.")
    )
    assertEquals(output, expected)
    assertEquals(errors, expectedErrors)
  }
}
