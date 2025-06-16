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
    runNewtypeTest("local-date", "@alloy#dateFormat" )
  }

  test("local-time newtype definition") {
    runNewtypeTest("local-time", "@alloy#localTimeFormat" )
  }

  test("local-date-time newtype definition") {
    runNewtypeTest("local-date-time", "@alloy#localDateTimeFormat" )
  }

  test("offset-date-time newtype definition") {
    runNewtypeTest(
      "offset-date-time",
      List("@alloy#offsetDateTimeFormat", "@timestampFormat(\"date-time\")"),
      "timestamp"
    )
  }

  test("offset-time newtype definition") {
    runNewtypeTest(
      "offset-time",
      "@alloy#offsetTimeFormat"
    )
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
         |    },
         |    "localDateTime": {
         |      "type": "string",
         |      "format": "local-date-time"
         |    },
         |    "offsetDateTime": {
         |      "type": "string",
         |      "format": "offset-date-time"
         |    },
         |    "offsetTime": {
         |      "type": "string",
         |      "format": "offset-time"
         |    }
         |  }
         |}
         |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Foo {
                            | localDate: alloy#LocalDate
                            | localTime: alloy#LocalTime
                            | localDateTime: alloy#LocalDateTime
                            | offsetDateTime: alloy#OffsetDateTime
                            | offsetTime: alloy#OffsetTime
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }
  private def runNewtypeTest(format: String, formatTrait: String): Unit = {
    runNewtypeTest(format, formatTrait :: Nil, "string")
  }

  private def runNewtypeTest(format: String, formatTraits: List[String], smithyType: String = "string"): Unit = {
    val jsonSchString =
      s"""|{
         |  "$$id": "test.json",
         |  "$$schema": "http://json-schema.org/draft-07/schema#",
         |  "title": "MyTimeType",
         |  "type": "string",
         |  "format": "$format"
         |}
         |""".stripMargin

    val expectedString = s"""|namespace foo
                            |
                            |${formatTraits.mkString("\n")}
                            |$smithyType MyTimeType
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }
}
