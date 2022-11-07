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

final class ParameterSpec extends munit.FunSuite {

  test("parameter - referenced") {
    val openapiString = """|openapi: "3.0.3"
                           |info:
                           |  version: 1.0.0
                           |  title: Content Provider Activation API
                           |paths:
                           |  '/content-providers/{contentProvider}/activation-token':
                           |    parameters:
                           |      - $ref: '#/components/parameters/contentProvider'
                           |    get:
                           |      operationId: generateActivationToken
                           |
                           |      responses:
                           |        '200':
                           |          content: {}
                           |components:
                           |  parameters:
                           |    contentProvider:
                           |      name: contentProvider
                           |      in: path
                           |      schema:
                           |        type: string
                           |      required: true
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |service FooService {
                            |    operations: [
                            |        GenerateActivationToken,
                            |    ],
                            |}
                            |
                            |@http(
                            |    method: "GET",
                            |    uri: "/content-providers/{contentProvider}/activation-token",
                            |    code: 200,
                            |)
                            |operation GenerateActivationToken {
                            |    input: GenerateActivationTokenInput,
                            |    output: Unit,
                            |}
                            |
                            |structure GenerateActivationTokenInput {
                            |    @httpLabel
                            |    @required
                            |    contentProvider: String,
                            |}""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("parameter - referenced header") {
    val openapiString = """|openapi: "3.0.3"
                           |info:
                           |  version: 1.0.0
                           |  title: Content Provider Activation API
                           |paths:
                           |  '/somePath':
                           |    parameters:
                           |      - $ref: '#/components/parameters/someHeader'
                           |    get:
                           |      operationId: generateActivationToken
                           |
                           |      responses:
                           |        '200':
                           |          content: {}
                           |components:
                           |  parameters:
                           |    someHeader:
                           |      name: X-Header-Test
                           |      in: header
                           |      schema:
                           |        type: string
                           |      required: true
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |service FooService {
                            |    operations: [
                            |        GenerateActivationToken,
                            |    ],
                            |}
                            |
                            |@http(
                            |    method: "GET",
                            |    uri: "/somePath",
                            |    code: 200,
                            |)
                            |operation GenerateActivationToken {
                            |    input: GenerateActivationTokenInput,
                            |    output: Unit,
                            |}
                            |
                            |structure GenerateActivationTokenInput {
                            |    @httpHeader("X-Header-Test")
                            |    @required
                            |    X_Header_Test: String,
                            |}""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("parameter - snake case path param") {
    val openapiString = """|openapi: "3.0.3"
                           |info:
                           |  version: 1.0.0
                           |  title: Content Provider Activation API
                           |paths:
                           |  '/content-providers/{content_provider}/activation-token':
                           |    parameters:
                           |      - name: content_provider
                           |        in: path
                           |        schema:
                           |          type: string
                           |        required: true
                           |    get:
                           |      operationId: generateActivationToken
                           |
                           |      responses:
                           |        '200':
                           |          content: {}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |service FooService {
                            |    operations: [
                            |        GenerateActivationToken,
                            |    ],
                            |}
                            |
                            |@http(
                            |    method: "GET",
                            |    uri: "/content-providers/{content_provider}/activation-token",
                            |    code: 200,
                            |)
                            |operation GenerateActivationToken {
                            |    input: GenerateActivationTokenInput,
                            |    output: Unit,
                            |}
                            |
                            |structure GenerateActivationTokenInput {
                            |    @httpLabel
                            |    @required
                            |    content_provider: String,
                            |}""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("parameter - inlined") {
    val openapiString = """|openapi: "3.0.3"
                           |info:
                           |  version: 1.0.0
                           |  title: Content Provider Activation API
                           |paths:
                           |  '/content-providers/{contentProvider}/activation-token':
                           |    parameters:
                           |      - name: contentProvider
                           |        in: path
                           |        schema:
                           |          type: string
                           |        required: true
                           |    get:
                           |      operationId: generateActivationToken
                           |
                           |      responses:
                           |        '200':
                           |          content: {}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |service FooService {
                            |    operations: [
                            |        GenerateActivationToken,
                            |    ],
                            |}
                            |
                            |@http(
                            |    method: "GET",
                            |    uri: "/content-providers/{contentProvider}/activation-token",
                            |    code: 200,
                            |)
                            |operation GenerateActivationToken {
                            |    input: GenerateActivationTokenInput,
                            |    output: Unit,
                            |}
                            |
                            |structure GenerateActivationTokenInput {
                            |    @httpLabel
                            |    @required
                            |    contentProvider: String,
                            |}""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

}
