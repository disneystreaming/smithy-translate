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

final class RefSpec extends munit.FunSuite {

  test("structures referencing unconstrained primitives") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Person",
                           |  "type": "object",
                           |  "properties": {
                           |    "firstName": {
                           |      "$ref": "#/$defs/name"
                           |    },
                           |    "lastName": {
                           |      "$ref": "#/$defs/name"
                           |    }
                           |  },
                           |  "$defs":{
                           |    "name": {
                           |      "type": "string"
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Person {
                            | firstName: Name,
                            | lastName: Name
                            |}
                            |
                            |string Name
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("structures referencing primitives") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Person",
                           |  "type": "object",
                           |  "properties": {
                           |    "firstName": {
                           |      "$ref": "#/$defs/name"
                           |    },
                           |    "lastName": {
                           |      "$ref": "#/$defs/name"
                           |    }
                           |  },
                           |  "$defs":{
                           |    "name": {
                           |      "type": "string",
                           |      "minLength": 1
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Person {
                            | firstName: Name,
                            | lastName: Name
                            |}
                            |
                            |@length(min: 1)
                            |string Name
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("structures referencing other structures") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Foo",
                           |  "type": "object",
                           |  "properties": {
                           |    "bar": {
                           |      "$ref": "#/$defs/bar"
                           |    }
                           |  },
                           |  "$defs":{
                           |    "bar": {
                           |      "type": "object"
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Foo {
                            |  bar: Bar
                            |}
                            |
                            |structure Bar {
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("structures referencing other structures inside defs") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Foo",
                           |  "type": "object",
                           |  "properties": {
                           |    "bar": {
                           |      "$ref": "#/$defs/bar"
                           |    }
                           |  },
                           |  "$defs":{
                           |    "bar": {
                           |      "type": "object",
                           |      "properties": {
                           |        "one": {
                           |          "$ref": "#/$defs/one"
                           |        }
                           |      }
                           |    },
                           |    "one": {
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
                            |structure Foo {
                            |  bar: Bar
                            |}
                            |
                            |structure Bar {
                            |  one: One
                            |}
                            |
                            |structure One {
                            |  one: String
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("nested refs") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Test",
                           |  "type": "object",
                           |  "properties": {
                           |    "one": {
                           |      "$ref": "#/$defs/one"
                           |    }
                           |  },
                           |  "$defs":{
                           |    "one": {
                           |      "$ref": "#/$defs/two"
                           |    },
                           |    "two": {
                           |      "type": "string",
                           |      "minLength": 1
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Test {
                            | one: Two
                            |}
                            |
                            |@length(min: 1)
                            |string Two
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("nested refs - list type") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Test",
                           |  "type": "object",
                           |  "properties": {
                           |    "one": {
                           |      "$ref": "#/$defs/one"
                           |    }
                           |  },
                           |  "$defs":{
                           |    "one": {
                           |      "$ref": "#/$defs/idList"
                           |    },
                           |    "idList": {
                           |      "type": ["array", "null"],
                           |      "items": {
                           |        "type": "string"
                           |      }
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use alloy#nullable
                            |
                            |structure Test {
                            | one: IdList
                            |}
                            |
                            |@nullable
                            |list IdList {
                            | member: String
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }
}
