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

final class TimestampSpec extends munit.FunSuite {
  test("date-time") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    MyTimestamp:
                     |      type: string
                     |      format: date-time
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |@timestampFormat("date-time")
                      |timestamp MyTimestamp
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("date-time in struct") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    MyObj:
                     |      type: object
                     |      properties:
                     |        t:
                     |          $ref: '#/components/schemas/MyTimestamp'
                     |    MyTimestamp:
                     |      type: string
                     |      format: date-time
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |structure MyObj {
                      |  t: MyTimestamp
                      |}
                      |
                      |@timestampFormat("date-time")
                      |timestamp MyTimestamp
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("simple date") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    MyDate:
                     |      type: string
                     |      format: date
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |use alloy#dateOnly
                      |
                      |@dateOnly
                      |string MyDate
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }
}
