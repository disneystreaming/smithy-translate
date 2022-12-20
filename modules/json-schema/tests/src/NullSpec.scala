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

final class NullSpec extends munit.FunSuite {

  test("null as a structure field") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Foo",
                           |  "type": "object",
                           |  "properties": {
                           |    "foo": {
                           |      "type": "null"
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use smithytranslate#Null
                            |
                            |structure Foo {
                            | foo: Null
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("null as a default") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Foo",
                           |  "type": "object",
                           |  "properties": {
                           |    "foo": {
                           |	    "default": null,
                           |	    "oneOf": [{
                           |		    "type": "string"
                           |	    },
                           |      {
                           |		    "type": "null"
                           |	    }]
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use alloy#nullable
                            |
                            |structure Foo {
                            | @default(null)
                            | @nullable
                            | foo: String
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("null as a default on member with nullable target") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Foo",
                           |  "type": "object",
                           |  "properties": {
                           |    "foo": {
                           |      "$ref": "#/$defs/nullableString"
                           |    }
                           |  },
                           |  "$defs": {
                           |    "nullableString": {
                           |      "default": null,
                           |      "type": ["string", "null"]
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use alloy#nullable
                            |
                            |structure Foo {
                            | @default(null)
                            | foo: NullableString
                            |}
                            |
                            |@nullable
                            |string NullableString""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("null as a reference") {
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
                           |      "type": "null"
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use smithytranslate#nullFormat
                            |
                            |structure Foo {
                            |  bar: Bar,
                            |}
                            |
                            |@nullFormat
                            |structure Bar {}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("null as an alternative") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Foo",
                           |  "type": "object",
                           |  "properties": {
                           |    "bar": {
                           |      "type": ["string", "null"]
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use alloy#nullable
                            |
                            |structure Foo {
                            | @nullable
                            | bar: String
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("null as an alternative - reference") {
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
                           |  "$defs": {
                           |    "bar": {
                           |      "type": ["string", "null"]
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use alloy#nullable
                            |
                            |structure Foo {
                            | bar: Bar
                            |}
                            |
                            |@nullable
                            |string Bar
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("null as an alternative to a nested definition") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "Foo",
                           |  "type": ["object", "null"],
                           |  "properties": {
                           |    "foo": {
                           |      "type": "string"
                           |    },
                           |    "bar": {
                           |      "type": ["string", "null"]
                           |    }
                           |  },
                           |  "required": [
                           |    "foo",
                           |    "bar"
                           |  ]
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |use alloy#nullable
                            |
                            |@nullable
                            |structure Foo {
                            |  @required
                            |  foo: String,
                            |  @required
                            |  @nullable
                            |  bar: String
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

}
