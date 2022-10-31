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

import cats.data.NonEmptyList

final class FileBasedSpec extends munit.FunSuite {

  /* . \|-- nested.json
   *   \|-- wrapper.json
   */
  test("multiple files - same directory") {
    val dir = os.temp.dir()
    val nestedFile = dir / "nested.json"
    val wrapperFile = dir / "wrapper.json"
    val nested = s"""|{
                     |  "$$schema": "http://json-schema.org/draft-07/schema#",
                     |  "$$id": "file:///$nestedFile",
                     |  "type": "object",
                     |  "title": "nested",
                     |  "additionalProperties": false,
                     |  "properties": {
                     |    "id": {
                     |      "type": "string"
                     |    }
                     |  }
                     |}""".stripMargin
    val wrapper = s"""|{
                      |  "$$schema": "http://json-schema.org/draft-07/schema#",
                      |  "$$id": "file:///$wrapperFile",
                      |  "type": "object",
                      |  "title": "wrapper",
                      |  "additionalProperties": false,
                      |  "properties": {
                      |    "data": {
                      |      "$$ref": "nested.json"
                      |    }
                      |  }
                      |}""".stripMargin

    os.write.over(nestedFile, nested)
    os.write.over(wrapperFile, wrapper)

    val expectedNested = """|namespace nested
                            |
                            |structure Nested {
                            |    id: String,
                            |}
                            |""".stripMargin

    val expectedWrapper = """|namespace wrapper
                             |
                             |use nested#Nested
                             |
                             |structure Wrapper {
                             |    data: Nested
                             |}
                             |""".stripMargin

    val inOne = TestUtils.ConversionTestInput(
      NonEmptyList.of("nested.json"),
      nested,
      expectedNested
    )
    val inTwo = TestUtils.ConversionTestInput(
      NonEmptyList.of("wrapper.json"),
      wrapper,
      expectedWrapper
    )
    TestUtils.runConversionTest(inOne, inTwo)
  }

  test("multiple files - primitive type target") {
    val dir = os.temp.dir()
    val nestedFile = dir / "nested.json"
    val wrapperFile = dir / "wrapper.json"
    val nested = s"""|{
                     |  "$$schema": "http://json-schema.org/draft-07/schema#",
                     |  "$$id": "file:///$nestedFile",
                     |  "type": "object",
                     |  "title": "nested",
                     |  "additionalProperties": false,
                     |  "properties": {
                     |    "id": {
                     |      "type": "string"
                     |    }
                     |  }
                     |}""".stripMargin
    val wrapper = s"""|{
                      |  "$$schema": "http://json-schema.org/draft-07/schema#",
                      |  "$$id": "file:///$wrapperFile",
                      |  "type": "object",
                      |  "title": "wrapper",
                      |  "additionalProperties": false,
                      |  "properties": {
                      |    "data": {
                      |      "$$ref": "nested.json#/properties/id"
                      |    }
                      |  }
                      |}""".stripMargin

    os.write.over(nestedFile, nested)
    os.write.over(wrapperFile, wrapper)

    val expectedNested = """|namespace nested
                            |
                            |structure Nested {
                            |    id: String
                            |}
                            |""".stripMargin

    val expectedWrapper = """|namespace wrapper
                             |
                             |structure Wrapper {
                             |    data: String
                             |}
                             |""".stripMargin

    val inOne = TestUtils.ConversionTestInput(
      NonEmptyList.of("nested.json"),
      nested,
      expectedNested
    )
    val inTwo = TestUtils.ConversionTestInput(
      NonEmptyList.of("wrapper.json"),
      wrapper,
      expectedWrapper
    )
    TestUtils.runConversionTest(inOne, inTwo)
  }

  /* .
   * \| wrapper.json
   * \| /bar
   * \| |-- nested.json
   */
  test("multiple files - child directory") {
    val dir = os.temp.dir()
    val nestedFile = dir / "bar" / "nested.json"
    val wrapperFile = dir / "wrapper.json"
    val nested = s"""|{
                     |  "$$schema": "http://json-schema.org/draft-07/schema#",
                     |  "$$id": "file:///$nestedFile",
                     |  "type": "object",
                     |  "title": "nested",
                     |  "additionalProperties": false,
                     |  "properties": {
                     |    "id": {
                     |      "type": "string"
                     |    }
                     |  }
                     |}""".stripMargin
    val wrapper = s"""|{
                      |  "$$schema": "http://json-schema.org/draft-07/schema#",
                      |  "$$id": "file:///$wrapperFile",
                      |  "type": "object",
                      |  "title": "wrapper",
                      |  "additionalProperties": false,
                      |  "properties": {
                      |    "data": {
                      |      "$$ref": "bar/nested.json"
                      |    }
                      |  }
                      |}""".stripMargin

    os.write.over(nestedFile, nested, createFolders = true)
    os.write.over(wrapperFile, wrapper)

    val expectedNested = """|namespace bar.nested
                            |
                            |structure Nested {
                            |    id: String,
                            |}
                            |""".stripMargin

    val expectedWrapper = """|namespace wrapper
                             |
                             |use bar.nested#Nested
                             |
                             |structure Wrapper {
                             |    data: Nested
                             |}
                             |""".stripMargin

    val inOne = TestUtils.ConversionTestInput(
      NonEmptyList.of("bar", "nested.json"),
      nested,
      expectedNested
    )
    val inTwo = TestUtils.ConversionTestInput(
      NonEmptyList.of("wrapper.json"),
      wrapper,
      expectedWrapper
    )
    TestUtils.runConversionTest(inOne, inTwo)
  }

  test("single file ref issue") {
    val dir = os.temp.dir()
    val nestedFile = dir / "nested.json"
    val nested = s"""|{
                     |  "$$schema": "http://json-schema.org/draft-07/schema#",
                     |  "$$id": "file:///$nestedFile",
                     |  "type": "object",
                     |  "title": "testOne",
                     |  "properties": {
                     |    "s": {
                     |      "$$ref": "#/$$defs/something"
                     |    },
                     |    "ids": {
                     |      "$$ref":  "#/$$defs/idList"
                     |    }
                     |  },
                     |  "additionalProperties": false,
                     |  "$$defs": {
                     |    "idList": {
                     |      "type": "array",
                     |      "items": {
                     |        "$$ref": "#/$$defs/id"
                     |      }
                     |    },
                     |    "something": {
                     |      "type": "object",
                     |      "properties": {
                     |        "a": {
                     |          "$$ref":  "#/$$defs/idList"
                     |        }
                     |      }
                     |    },
                     |    "id": {
                     |      "type": "string"
                     |    }
                     |  }
                     |}""".stripMargin

    os.write.over(nestedFile, nested)

    val expectedNested = """|namespace data.nested
                            |
                            |structure Something {
                            |    a: IdList
                            |}
                            |
                            |structure TestOne {
                            |    s: Something
                            |    ids: IdList
                            |}
                            |
                            |list IdList {
                            |    member: Id
                            |}
                            |string Id""".stripMargin

    val inOne = TestUtils.ConversionTestInput(
      NonEmptyList.of("data", "nested.json"),
      nested,
      expectedNested
    )
    TestUtils.runConversionTest(inOne)
  }
}
