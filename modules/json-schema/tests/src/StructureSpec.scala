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

final class StructureSpec extends munit.FunSuite {

  test("structures") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Person",
                           |  "type": "object",
                           |  "properties": {
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
                            |structure Person {
                            | firstName: String,
                            | lastName: String
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("structures - nested") {
    val jsonSchString = """|{
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
}
