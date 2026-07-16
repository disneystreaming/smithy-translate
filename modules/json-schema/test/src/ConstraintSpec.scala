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

final class ConstraintSpec extends munit.FunSuite {

  test("min max : int with long bounds") {
    val jsonSchString =
      """|{
         |  "$id": "test.json",
         |  "$schema": "http://json-schema.org/draft-07/schema#",
         |  "title": "Test",
         |  "type": "object",
         |  "properties": {
         |    "number": {
         |      "type": "integer",
         |      "minimum": -9223372036854775808,
         |      "maximum": 9223372036854775807
         |    }
         |  }
         |}
         |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Test {
                            |  @range(
                            |    min: -9223372036854775808,
                            |    max: 9223372036854775807
                            |  )
                            |  number: Long
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("min max : integer with small whole-number bounds") {
    val jsonSchString =
      """|{
         |  "$id": "test.json",
         |  "$schema": "http://json-schema.org/draft-07/schema#",
         |  "title": "Test",
         |  "type": "object",
         |  "properties": {
         |    "number": {
         |      "type": "integer",
         |      "minimum": 1,
         |      "maximum": 10
         |    }
         |  }
         |}
         |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Test {
                            |  @range(
                            |    min: 1,
                            |    max: 10
                            |  )
                            |  number: Integer
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("min max : long with big max and small min bounds") {
    val jsonSchString =
      """|{
         |  "$id": "test.json",
         |  "$schema": "http://json-schema.org/draft-07/schema#",
         |  "title": "Test",
         |  "type": "object",
         |  "properties": {
         |    "number": {
         |      "type": "integer",
         |      "minimum": 1,
         |      "maximum": 9223372036854775807
         |    }
         |  }
         |}
         |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Test {
                            |  @range(
                            |    min: 1,
                            |    max: 9223372036854775807
                            |  )
                            |  number: Long
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("min max : long with small max and big min bounds") {
    val jsonSchString =
      """|{
         |  "$id": "test.json",
         |  "$schema": "http://json-schema.org/draft-07/schema#",
         |  "title": "Test",
         |  "type": "object",
         |  "properties": {
         |    "number": {
         |      "type": "integer",
         |      "minimum": -9223372036854775808,
         |      "maximum": 1
         |    }
         |  }
         |}
         |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Test {
                            |  @range(
                            |    min: -9223372036854775808,
                            |    max: 1
                            |  )
                            |  number: Long
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("min max : double when number and some arbitrary range") {
    val jsonSchString =
      """|{
         |  "$id": "test.json",
         |  "$schema": "http://json-schema.org/draft-07/schema#",
         |  "title": "Test",
         |  "type": "object",
         |  "properties": {
         |    "number": {
         |      "type": "number",
         |      "maximum": 1
         |    }
         |  }
         |}
         |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Test {
                            |  @range(
                            |    max: 1.0
                            |  )
                            |  number: Double
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

}
