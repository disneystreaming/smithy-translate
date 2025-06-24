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

final class StructureSpec extends munit.FunSuite {

  test("structures") {
    val jsonSchString =
      """|{
         |  "$id": "test.json",
         |  "$schema": "http://json-schema.org/draft-07/schema#",
         |  "title": "Person",
         |  "type": "object",
         |  "properties": {
         |    "id": {
         |      "type": "string",
         |      "format": "uuid"
         |    },
         |    "firstName": {
         |      "type": "string"
         |    },
         |    "lastName": {
         |      "type": "string"
         |    }
         |  }
         |}
         |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use alloy#UUID
                            |
                            |structure Person {
                            | id: UUID,
                            | firstName: String,
                            | lastName: String
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("structures - constant") {
    val jsonSchString =
      """|{
         |  "$id": "test.json",
         |  "$schema": "http://json-schema.org/draft-07/schema#",
         |  "title": "Constant",
         |  "type": "object",
         |  "additionalProperties": false
         |}
         |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Constant {
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)

  }

  test("structures - nested") {
    val jsonSchString =
      """|{
         |  "$id": "test.json",
         |  "$schema": "http://json-schema.org/draft-07/schema#",
         |  "title": "Person",
         |  "type": "object",
         |  "properties": {
         |    "something": {
         |      "type": "object",
         |      "properties": {
         |        "one": {
         |          "type": "string"
         |        }
         |      }
         |    }
         |  }
         |}
         |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Person {
                            | something: Something
                            |}
                            |
                            |structure Something {
                            |  one: String
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("structures - retain property member casing") {
    val openapiString =
      """|{
         |  "$id": "test.json",
         |  "$schema": "http://json-schema.org/draft-07/schema#",
         |  "title": "Object",
         |  "type": "object",
         |  "properties": {
         |    "number_one": {
         |      "type": "string"
         |    },
         |    "numberTwo": {
         |      "type": "string"
         |    },
         |    "NumberThree": {
         |      "type": "string"
         |    },
         |    "NUMBER_FOUR": {
         |      "type": "string"
         |    },
         |    "nUMbeR_FiVE": {
         |      "type": "string"
         |    },
         |    "12_twelve": {
         |      "type": "string"
         |    },
         |    "X-something": {
         |      "type": "string"
         |    }
         |  }
         |}
         |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Object {
                            | number_one: String
                            | numberTwo: String
                            | NumberThree: String
                            | NUMBER_FOUR: String
                            | nUMbeR_FiVE: String
                            | @jsonName("12_twelve")
                            | n12_twelve: String
                            | @jsonName("X-something")
                            | X_something: String
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("structures - document field") {
    val jsonSchString =
      """|{
         |  "$id": "test.json",
         |  "$schema": "http://json-schema.org/draft-07/schema#",
         |  "title": "Something",
         |  "type": "object",
         |  "properties": {
         |    "anything": {}
         |  }
         |}
         |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Something {
                            | anything: Document
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }
}
