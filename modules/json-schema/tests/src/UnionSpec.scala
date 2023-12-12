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

final class UnionSpec extends munit.FunSuite {

  test("unions - oneOf") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Test",
                           |  "type": "object",
                           |  "properties": {
                           |    "example": {
                           |    "oneOf": [
                           |        {
                           |            "type": "integer"
                           |        },
                           |        {
                           |            "type": "string"
                           |        }
                           |      ]
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use alloy#untagged
                            |
                            |structure Test {
                            | example: Example
                            |}
                            |
                            |@untagged
                            |union Example {
                            | Integer: Integer,
                            | String: String,
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("unions - anyOf") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Test",
                           |  "type": "object",
                           |  "properties": {
                           |    "example": {
                           |    "anyOf": [
                           |        {
                           |            "type": "integer"
                           |        },
                           |        {
                           |            "type": "string"
                           |        }
                           |      ]
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use alloy#untagged
                            |
                            |structure Test {
                            | example: Example
                            |}
                            |
                            |@untagged
                            |union Example {
                            | Integer: Integer,
                            | String: String,
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("unions - oneOf with structure member") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Test",
                           |  "type" : "object",
                           |  "properties": {
                           |    "example": {
                           |      "oneOf" : [
                           |       {
                           |        "type" : "object",
                           |        "properties" : {
                           |            "firstName" : {
                           |                "type" : "string"
                           |            },
                           |            "lastName" : {
                           |                "type" : "string"
                           |            },
                           |            "sport" : {
                           |                "type" : "string"
                           |            }
                           |          }
                           |      },
                           |      {
                           |        "type" : "object",
                           |        "properties" : {
                           |            "vehicle" : {
                           |                "type" : "string"
                           |            },
                           |            "price" : {
                           |                "type" : "integer"
                           |            }
                           |          }
                           |        }
                           |      ]
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use alloy#untagged
                            |
                            |structure TestExampleOneOfAlt0 {
                            |    firstName: String,
                            |    lastName: String,
                            |    sport: String
                            |}
                            |
                            |structure TestExampleOneOfAlt1 {
                            |    vehicle: String,
                            |    price: Integer
                            |}
                            |
                            |@untagged
                            |union Example {
                            | alt0: TestExampleOneOfAlt0,
                            | alt1: TestExampleOneOfAlt1
                            |}
                            |
                            |structure Test {
                            | example: Example
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("unions - tagged style") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Test",
                           |  "type" : "object",
                           |  "properties": {
                           |    "example": {
                           |      "oneOf" : [
                           |       {
                           |        "type" : "object",
                           |        "properties" : {
                           |            "one" : {
                           |                "type" : "string"
                           |            }
                           |          },
                           |         "required": ["one"]
                           |      },
                           |      {
                           |        "type" : "object",
                           |        "properties" : {
                           |            "two" : {
                           |                "type" : "integer"
                           |            }
                           |          },
                           |          "required": ["two"]
                           |        }
                           |      ]
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure TestExampleOneOfAlt0 {
                            | @required
                            | one: String
                            |}
                            |
                            |structure TestExampleOneOfAlt1 {
                            | @required
                            | two: Integer
                            |}
                            |
                            |union Example {
                            | one: String,
                            | two: Integer
                            |}
                            |
                            |structure Test {
                            | example: Example
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }
}
