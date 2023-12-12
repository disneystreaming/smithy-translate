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

package smithytranslate.compiler.json_schema

final class SpecialFormatSpec extends munit.FunSuite {

  test("uuid format") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Test",
                           |  "type": "object",
                           |  "properties": {
                           |    "id": {
                           |      "type": "string",
                           |      "format": "uuid"
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use alloy#UUID
                            |
                            |structure Test {
                            | id: UUID
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("uuid format - ref") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Test",
                           |  "type": "object",
                           |  "properties": {
                           |    "id": {
                           |      "$ref": "#/$defs/id"
                           |    }
                           |  },
                           |  "$defs":{
                           |    "id": {
                           |      "type": "string",
                           |      "format": "uuid"
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use alloy#uuidFormat
                            |
                            |structure Test {
                            | id: Id
                            |}
                            |
                            |@uuidFormat
                            |string Id
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("date format") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Test",
                           |  "type": "object",
                           |  "properties": {
                           |    "id": {
                           |      "type": "string",
                           |      "format": "date"
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use alloy#dateFormat
                            |
                            |structure Test {
                            |    @dateFormat
                            |    id: String
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("date-time format") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Test",
                           |  "type": "object",
                           |  "properties": {
                           |    "id": {
                           |      "type": "string",
                           |      "format": "date-time"
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Test {
                            | @timestampFormat("date-time")
                            | id: Timestamp
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }
}
