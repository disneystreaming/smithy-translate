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

final class TimeTypesSpec extends munit.FunSuite {
  test("top-level/newtypes definitions") {
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
                           |    MyLocalDate:
                           |      type: string
                           |      format: local-date
                           |    MyLocalTime:
                           |      type: string
                           |      format: local-time
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |@timestampFormat("date-time")
                            |timestamp MyTimestamp
                            |
                            |@alloy#dateFormat
                            |string MyLocalDate
                            |
                            |@alloy#localTimeFormat
                            |string MyLocalTime
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("time types in struct") {
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
                           |        a:
                           |          $ref: '#/components/schemas/MyTimestamp'
                           |        b:
                           |          $ref: '#/components/schemas/MyLocalDate'
                           |        localDate:
                           |          type: string
                           |          format: local-date
                           |        localTime:
                           |          type: string
                           |          format: local-time
                           |    MyTimestamp:
                           |      type: string
                           |      format: date-time
                           |    MyLocalDate:
                           |      type: string
                           |      format: local-date
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure MyObj {
                            |  a: MyTimestamp
                            |  b: MyLocalDate
                            |  localDate: alloy#LocalDate
                            |  localTime: alloy#LocalTime
                            |}
                            |
                            |@timestampFormat("date-time")
                            |timestamp MyTimestamp
                            |
                            |@alloy#dateFormat
                            |string MyLocalDate
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
                            |use alloy#dateFormat
                            |
                            |@dateFormat
                            |string MyDate
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("date-time with example") {
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
                           |      example: '2017-07-21T17:32:28Z'
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use alloy#dataExamples
                            |
                            |@dataExamples([
                            |    {
                            |        json: "2017-07-21T17:32:28Z"
                            |    }
                            |])
                            |
                            |@timestampFormat("date-time")
                            |timestamp MyTimestamp
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("simple date with example") {
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
                           |      example: '2017-07-21'
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use alloy#dateFormat
                            |use alloy#dataExamples
                            |
                            |@dataExamples([
                            |    {
                            |        json: "2017-07-21"
                            |    }
                            |])
                            |@dateFormat
                            |string MyDate
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }
}
