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

final class AllOfSpec extends munit.FunSuite {

  test("unions - allOf two structures") {
    val jsonSchString =
      """|{
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
    val jsonSchString =
      """|{
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

  test("unions - single allOf reference") {
    val jsonSchString =
      """|{
         |  "$id": "test.json",
         |  "$schema": "http://json-schema.org/draft-07/schema#",
         |  "title": "Test",
         |  "type" : "object",
         |  "properties" : {
         |    "example": {
         |      "type": "object",
         |      "allOf": [
         |        { "$ref": "#/$defs/two" }
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
                            |structure Example with [Two] {}
                            |
                            |structure Test {
                            |  example: Example
                            |}
                            |
                            |@mixin
                            |structure Two {
                            |  vehicle: String,
                            |  price: Integer
                            |}
                            |
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("unions - Parent type explicitly referenced and extended") {
    val jsonSchString =
      """|{
         |  "$id": "test.json",
         |  "$schema": "http://json-schema.org/draft-07/schema#",
         |  "title": "Test",
         |  "type" : "object",
         |  "properties" : {
         |    "root": { "$ref": "#/$defs/root" }
         |  },
         |  "$defs": {
         |    "root": { 
         |      "type": "object",
         |      "properties": { 
         |        "s": { 
         |          "type": "string"
         |        }
         |      }
         |    },
         |    "subB": {
         |      "type": "object",
         |      "allOf": [
         |        { "$ref": "#/$defs/root" },
         |        { "type": "object", "properties": { "x": { "type": "integer" } } }
         |      ]
         |    },
         |    "subA": {
         |      "type": "object",
         |      "allOf": [
         |        { "$ref": "#/$defs/root" },
         |        { "type": "object", "properties": { "x": { "type": "integer" } } }
         |      ]
         |    }
         |  }
         |}
         |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Root with [RootMixin] {} 
                            |
                            |@mixin
                            |structure RootMixin {
                            |  s: String
                            |}
                            |
                            |structure SubA with [RootMixin] {
                            |  x: Integer
                            |}
                            |
                            |structure SubB with [RootMixin] {
                            |  x: Integer 
                            |}
                            |
                            |structure Test {
                            |  root: Root
                            |}
                            |
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("unions - multiple mixins") {
    val jsonSchString =
      """|{
         |  "$id": "test.json",
         |  "$schema": "http://json-schema.org/draft-07/schema#",
         |  "title": "Test",
         |  "type" : "object",
         |  "properties" : {
         |    "root": { "$ref": "#/$defs/root" },
         |    "rootTwo": { "$ref": "#/$defs/rootTwo" }
         |  },
         |  "$defs": {
         |    "root": { 
         |      "type": "object",
         |      "properties": { 
         |        "s": { 
         |          "type": "string"
         |        }
         |      }
         |    },
         |    "rootTwo": { 
         |      "type": "object",
         |      "properties": { 
         |        "sTwo": { 
         |          "type": "integer"
         |        }
         |      }
         |    },
         |    "subB": {
         |      "type": "object",
         |      "allOf": [
         |        { "$ref": "#/$defs/root" },
         |        { "$ref": "#/$defs/rootTwo" },
         |        { "type": "object", "properties": { "x": { "type": "integer" } } }
         |      ]
         |    },
         |    "subA": {
         |      "type": "object",
         |      "allOf": [
         |        { "$ref": "#/$defs/root" },
         |        { "type": "object", "properties": { "x": { "type": "integer" } } }
         |      ]
         |    }
         |  }
         |}
         |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure Root with [RootMixin] {} 
                            |
                            |structure RootTwo with [RootTwoMixin] {}
                            |
                            |@mixin
                            |structure RootMixin {
                            |  s: String
                            |}
                            |
                            |@mixin
                            |structure RootTwoMixin {
                            |    sTwo: Integer
                            |}
                            |
                            |structure SubA with [RootMixin] {
                            |  x: Integer
                            |}
                            |
                            |structure SubB with [RootMixin, RootTwoMixin] {
                            |  x: Integer 
                            |}
                            |
                            |structure Test {
                            |  root: Root
                            |  rootTwo: RootTwo
                            |}
                            |
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }
  
  test("B mixes in A, C mixes in B, B is directly referenced".only) {
    // BMixin is generated with no fields; they are generated in B
    // B mixes in A and BMixin, should only mix in BMixin
    val jsonSchString =
      """|{
         |  "$id": "test.json",
         |  "$schema": "http://json-schema.org/draft-07/schema#",
         |  "title": "Test",
         |  "type" : "object",
         |  "properties" : {
         |    "b": { "$ref": "#/$defs/B" }
         |  },
         |  "$defs": {
         |    "C": {
         |      "type": "object",
         |      "allOf": [
         |        { "$ref": "#/$defs/B" },
         |        {
         |          "type": "object",
         |          "properties": {
         |            "c": { "type": "integer" }
         |          }
         |        }
         |      ]
         |    },
         |    "B": {
         |      "type": "object",
         |      "allOf": [
         |        { "$ref": "#/$defs/A" },
         |        {
         |          "type": "object",
         |          "properties": {
         |            "b": { "type": "boolean"}
         |          }
         |        }
         |      ]
         |    },
         |    "A": {
         |      "type": "object",
         |      "properties": {
         |        "a": { "type": "integer" }
         |      }
         |    }
         |  }
         |}
         |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |@mixin
                            |structure A {
                            |    a: Integer
                            |}
                            |
                            |@mixin
                            |structure BMixin with [A] {
                            |    b: Boolean
                            |}
                            |
                            |structure B with [BMixin] {}
                            |
                            |structure C with [BMixin] {
                            |  c: Integer
                            |}
                            |
                            |structure Test {
                            |  b: B
                            |}
                            |
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }
  
  test("B mixes in A, C mixes in B".only) {
    // B fails to be annotated as a mixin
    // C fails to mix in B
    val jsonSchString =
      """|{
         |  "$id": "test.json",
         |  "$schema": "http://json-schema.org/draft-07/schema#",
         |  "title": "Test",
         |  "type" : "object",
         |  "properties" : {
         |    "c": { "$ref": "#/$defs/C" }
         |  },
         |  "$defs": {
         |    "C": {
         |      "type": "object",
         |      "allOf": [
         |        { "$ref": "#/$defs/B" },
         |        {
         |          "type": "object",
         |          "properties": {
         |            "c": { "type": "integer" }
         |          }
         |        }
         |      ]
         |    },
         |    "B": {
         |      "type": "object",
         |      "allOf": [
         |        { "$ref": "#/$defs/A" },
         |        {
         |          "type": "object",
         |          "properties": {
         |            "b": { "type": "boolean"}
         |          }
         |        }
         |      ]
         |    },
         |    "A": {
         |      "type": "object",
         |      "properties": {
         |        "a": { "type": "integer" }
         |      }
         |    }
         |  }
         |}
         |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |@mixin
                            |structure A {
                            |    a: Integer
                            |}
                            |
                            |
                            |@mixin
                            |structure B with [A] {
                            |    b: Boolean
                            |}
                            |
                            |structure C with [B] {
                            |  c: Integer
                            |}
                            |
                            |structure Test {
                            |  c: C
                            |}
                            |
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }
  

}
