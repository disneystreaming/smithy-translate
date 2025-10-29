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

final class ConstSpec extends munit.FunSuite {

  test("const properties") {
    val jsonSchString =
      s"""|{
          |  "$$id": "test.json",
          |  "$$schema": "http://json-schema.org/draft-07/schema#",
          |  "title": "Const",
          |  "type": "object",
          |  "properties": {
          |    "bool": { "const": true },
          |    "int": { "const": 1 },
          |    "long": { "const": ${Long.MaxValue} },
          |    "bigInt": { "const": ${BigInt(Long.MaxValue) + 1} },
          |    "decimal": { "const": 1.0 },
          |    "null": { "const": null },
          |    "array": { "const": [1, "2", null] },
          |    "object": { "const": { "a": 1, "b": "2", "c": null } }
          |  }
          |}
          |""".stripMargin

    val expectedString =
      s"""|namespace foo
          |
          |use smithytranslate#const
          |
          |structure Const {
          |  @const(true)
          |  bool: Document
          |  @const(1)
          |  int: Document
          |  @const(${Long.MaxValue})
          |  long: Document
          |  @const(${BigInt(Long.MaxValue) + 1})
          |  bigInt: Document
          |  @const(1.0)
          |  decimal: Document
          |  @const(null)
          |  null: Document
          |  @const([1, "2", null])
          |  array: Document
          |  @const({a: 1, b: "2", c: null})
          |  object: Document
          |}
          |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

  test("const defs") {
    val jsonSchString =
      s"""|{
          |  "$$id": "test.json",
          |  "$$schema": "http://json-schema.org/draft-07/schema#",
          |  "title": "Const",
          |  "$$defs": {
          |    "bool": { "const": true },
          |    "int": { "const": 1 },
          |    "long": { "const": ${Long.MaxValue} },
          |    "bigInt": { "const": ${BigInt(Long.MaxValue) + 1} },
          |    "decimal": { "const": 1.0 },
          |    "null": { "const": null },
          |    "array": { "const": [1, "2", null] },
          |    "object": { "const": { "a": 1, "b": "2", "c": null } }
          |  }
          |}
          |""".stripMargin

    val expectedString =
      s"""|namespace foo
          |
          |use smithytranslate#const
          |
          |@const(true)
          |document Bool
          |
          |@const(1)
          |document Int
          |
          |@const(${Long.MaxValue})
          |document Long
          |
          |@const(${BigInt(Long.MaxValue) + 1})
          |document BigInt
          |
          |@const(1.0)
          |document Decimal
          |
          |@const(null)
          |document Null
          |
          |@const([1, "2", null])
          |document Array
          |
          |@const({a: 1, b: "2", c: null})
          |document Object
          |
          |document Const
          |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }

}
