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

final class AllOfSpec extends munit.FunSuite {

  test("unions - allOf two structures") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Test",
                           |  "type" : "object",
                           |  "properties": {
                           |    "example": {
                           |      "allOf" : [
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
                            |structure Example {
                            |    firstName: String,
                            |    lastName: String,
                            |    sport: String,
                            |    vehicle: String,
                            |    price: Integer
                            |}
                            |
                            |structure Test {
                            | example: Example
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("unions - allOf reference") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Test",
                           |  "type" : "object",
                           |  "properties": {
                           |    "example": {
                           |      "allOf" : [
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
                           |       },
                           |       {
                           |        "$ref": "#/$defs/two"
                           |       }
                           |      ]
                           |    }
                           |  },
                           |  "$defs": {
                           |    "two": {
                           |      "type" : "object",
                           |      "properties" : {
                           |         "vehicle" : {
                           |            "type" : "string"
                           |         },
                           |         "price" : {
                           |            "type" : "integer"
                           |         }
                           |      }
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Example with [Two] {
                            |    firstName: String,
                            |    lastName: String,
                            |    sport: String
                            |}
                            |
                            |@mixin
                            |structure Two {
                            |  vehicle: String,
                            |  price: Integer
                            |}
                            |
                            |structure Test {
                            | example: Example
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

}
