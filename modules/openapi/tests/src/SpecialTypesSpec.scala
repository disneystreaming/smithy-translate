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

final class SpecialTypesSpec extends munit.FunSuite {

  test("UUID") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Object:
                     |      type: object
                     |      properties:
                     |        u:
                     |          type: string
                     |          format: uuid
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |use alloy#UUID
                      |
                      |structure Object {
                      | u: UUID
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

}
