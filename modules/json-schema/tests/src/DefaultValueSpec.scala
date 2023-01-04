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

package smithytranslate.json_schema

final class DefaultValueSpec extends munit.FunSuite {

  test("default value - string") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Person",
                           |  "type": "object",
                           |  "properties": {
                           |    "firstName": {
                           |      "type": "string",
                           |      "default": "Testing this out."
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Person {
                            | @default("Testing this out.")
                            | firstName: String
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("default value - int") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Test",
                           |  "type": "object",
                           |  "properties": {
                           |    "one": {
                           |      "type": "integer",
                           |      "default": 2022
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Test {
                            | @default(2022)
                            | one: Integer
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("default value - double") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Test",
                           |  "type": "object",
                           |  "properties": {
                           |    "one": {
                           |      "type": "number",
                           |      "default": 2022.17
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Test {
                            | @default(2022.17)
                            | one: Double
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("default value - list") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Test",
                           |  "type": "object",
                           |  "properties": {
                           |    "one": {
                           |      "type": "array",
                           |      "items": {
                           |        "type": "string"
                           |      },
                           |      "default": []
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |list One {
                            | member: String
                            |}
                            |
                            |structure Test {
                            | @default([])
                            | one: One
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("default value - string reference") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Person",
                           |  "type": "object",
                           |  "properties": {
                           |    "firstName": {
                           |      "$ref": "#/$defs/name"
                           |    }
                           |  },
                           |  "$defs":{
                           |    "name": {
                           |      "type": "string",
                           |      "default": "Testing this out."
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |string Name
                            |
                            |structure Person {
                            | firstName: Name = "Testing this out."
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("default value - list reference") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Test",
                           |  "type": "object",
                           |  "properties": {
                           |    "one": {
                           |      "$ref": "#/$defs/OneList"
                           |    }
                           |  },
                           |  "$defs":{
                           |    "OneList": {
                           |      "type": "array",
                           |      "items": {
                           |        "type": "string"
                           |      },
                           |      "default": []
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |list OneList {
                            | member: String
                            |}
                            |
                            |structure Test {
                            | @default([])
                            | one: OneList
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("default value conflicts with required") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Person",
                           |  "type": "object",
                           |  "required": ["firstName1"],
                           |  "properties": {
                           |    "firstName1": {
                           |      "type": "string",
                           |      "default": "Testing this out."
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Person {
                            | @default("Testing this out.")
                            | firstName1: String
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }
}
