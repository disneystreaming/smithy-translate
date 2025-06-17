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
      "string",
      "timestamp"
    )
  }

  test("offset-time newtype definition") {
    runNewtypeTest(
      "offset-time",
      "@alloy#offsetTimeFormat"
    )
  }

  test("zone-id newtype definition") {
    runNewtypeTest(
      "zone-id",
      "@alloy#zoneIdFormat"
    )
  }

  test("zone-offset newtype definition") {
    runNewtypeTest(
      "zone-offset",
      "@alloy#zoneOffsetFormat"
    )
  }

  test("zoned-date-time newtype definition") {
    runNewtypeTest(
      "zoned-date-time",
      "@alloy#zonedDateTimeFormat"
    )
  }

  test("year newtype definition") {
    runNewtypeTest(
      "year",
      List("@alloy#yearFormat"),
      "integer",
      "integer"
    )
  }

  test("year-month newtype definition") {
    runNewtypeTest(
      "year-month",
      "@alloy#yearMonthFormat"
    )

  }

  test("month-day newtype definition") {
    runNewtypeTest(
      "month-day",
      "@alloy#monthDayFormat",
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
         |    },
         |    "zoneId": {
         |      "type": "string",
         |      "format": "zone-id"
         |    },
         |    "zoneOffset": {
         |      "type": "string",
         |      "format": "zone-offset"
         |    },
         |    "zonedDateTime": {
         |      "type": "string",
         |      "format": "zoned-date-time"
         |    },
         |    "year": {
         |      "type": "integer",
         |      "format": "year"
         |    },
         |    "yearMonth": {
         |      "type": "string",
         |      "format": "year-month"
         |    },
         |    "monthDay": {
         |      "type": "string",
         |      "format": "month-day"
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
                            | zoneId: alloy#ZoneId
                            | zoneOffset: alloy#ZoneOffset
                            | zonedDateTime: alloy#ZonedDateTime
                            | year: alloy#Year
                            | yearMonth: alloy#YearMonth
                            | monthDay: alloy#MonthDay
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }
  private def runNewtypeTest(format: String, formatTrait: String): Unit = {
    runNewtypeTest(format, List(formatTrait))
  }

  private def runNewtypeTest(format: String, formatTraits: List[String], jsonType: String = "string", smithyType: String = "string"): Unit = {
    val jsonSchString =
      s"""|{
         |  "$$id": "test.json",
         |  "$$schema": "http://json-schema.org/draft-07/schema#",
         |  "title": "MyTimeType",
         |  "type": "$jsonType",
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
