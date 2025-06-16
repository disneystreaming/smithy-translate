/* Copyright 2025 Disney Streaming
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

package smithytranslate.compiler.json_schema

final class TimeTypeSpec extends munit.FunSuite {

  test("local-date newtype definitions") {
    val jsonSchString =
      """|{
         |  "$id": "localDate.json",
         |  "$schema": "http://json-schema.org/draft-07/schema#",
         |  "title": "MyLocalDate",
         |  "type": "string",
         |  "format": "local-date"
         |}
      """.stripMargin

    val expectedString = """|namespace foo
                            |
                            |@alloy#dateFormat
                            |string MyLocalDate
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("local-time newtype definition") {

    val jsonSchString =
      """|{
         |  "$id": "localTime.json",
         |  "$schema": "http://json-schema.org/draft-07/schema#",
         |  "title": "MyLocalTime",
         |  "type": "string",
         |  "format": "local-time"
         |}
         |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |@alloy#localTimeFormat
                            |string MyLocalTime
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }


  test("nested definitions") {
    val jsonSchString =
      """|{
         |  "$id": "test.json",
         |  "$schema": "http://json-schema.org/draft-07/schema#",
         |  "title": "Foo",
         |  "type": "object",
         |  "properties": {
         |    "localDate": {
         |      "type": "string",
         |      "format": "local-date"
         |    },
         |    "localTime": {
         |      "type": "string",
         |      "format": "local-time"
         |    }
         |  }
         |}
         |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Foo {
                            | localDate: alloy#LocalDate
                            | localTime: alloy#LocalTime
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

}
