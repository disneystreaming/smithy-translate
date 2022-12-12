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
package smithytranslate
package formatter
import parsers.ControlParser.control_section
import smithytranslate.formatter.parsers.IdlParser
import smithytranslate.formatter.parsers.MetadataParser.metadata_section
import munit.Location

final class ParserSpec extends munit.FunSuite {
  private def assertEitherIsRight[T, R](result: Either[T, R])(implicit
      loc: Location
  ) = {
    assert(
      result.isRight,
      s"Failed with ${result.swap.getOrElse(fail("Unable to extract error from either"))}"
    )
  }

  val metadataStatement: String =
    """metadata greeting = "hello"
    metadata "stringList" = ["a", "b", "c"]
    """.stripMargin
  val controlStatement: String = """$version: "1.0"
    """.stripMargin

  test("Parse a metadata statement") {
    val result = metadata_section.parseAll(metadataStatement)
    assertEitherIsRight(result)
    assert(result.exists(_.metadata.size == 2))
  }

  test("Parse a control statement") {
    val result = control_section.parseAll(controlStatement)
    assertEitherIsRight(result)
    assert(result.exists(_.list.size == 1))
  }

  test("both") {
    val result =
      IdlParser.idlParser.parseAll(controlStatement + metadataStatement)
    assertEitherIsRight(result)
    assert(
      result.exists(res =>
        res.metadata.metadata.size == 2 && res.control.list.size == 1
      )
    )
  }

  test("complex metadata") {
    val result =
      IdlParser.idlParser.parseAll("""|$version: "2.0"
                                      |
                                      |metadata somePieceOfData = { name: "examples.hello", entryPoints: true }
                                      |metadata other = { name: "examples.hello" }
                                      |metadata noComma = { name: "examples.hello" entryPoints: true }
                                      |metadata noComma = {
                                      |name: "examples.hello"
                                      |entryPoints: true
                                      |}
                                      |
                                      |namespace examples.hello
                                      |""".stripMargin)
    assertEitherIsRight(result)
  }

  /*
@readonly
@http(method: "GET", uri: "/filmography/{actorId}")
///Get the [Filmography] for the specified actor
operation GetFilmography {
  input: ActorInput,
  output: Filmography
}
   */

  test("operation") {
    val result =
      IdlParser.idlParser.parseAll(
        """|$version: "2.0"
           |
           |namespace test
           |
           |operation GetFilmography {
           |  input: ActorInput
           |  output: Filmography
           |}
           |""".stripMargin
      )
    assertEitherIsRight(result)
  }

  test("map") {
    val result =
      IdlParser.idlParser.parseAll(
        """|$version: "2.0"
           |
           |namespace test
           |
           |map Test {key: String value: String}
           |""".stripMargin
      )
    assertEitherIsRight(result)
  }

  test("trait") {
    val result =
      IdlParser.idlParser.parseAll(
        """|$version: "2.0"
           |
           |namespace test
           |
           |@http(method: "GET", uri: "/filmography/{actorId}")
           |structure GetFilmography {
           |  input: String,
           |}
           |""".stripMargin
      )
    assertEitherIsRight(result)
  }

  test("enums") {
    val result =
      IdlParser.idlParser.parseAll(
        """|$version: "2.0"
           |
           |namespace test
           |
           |// no spaces
           |enum OtherEnum {
           |    V1 ="v1",
           |    V2 = "v2"
           |}
           |
           |// with commas
           |enum OtherEnum {
           |    V1 = "v1",
           |    V2 = "v2"
           |}
           |
           |enum A { FOO BAR BAZ }
           |""".stripMargin
      )
    assertEitherIsRight(result)
  }

  test("parse without trailing") {
    val result =
      IdlParser.idlParser.parseAll(
        """|$version: "2.0"
           |
           |namespace test
           |
           |structure Personalization {
           |  name: String
           |}""".stripMargin
      )
    assertEitherIsRight(result)
  }

  test("list w/o a member") {
    val result =
      IdlParser.idlParser.parseAll(
        """|$version: "2.0"
           |
           |namespace test
           |
           |@mixin
           |list MixinList {
           |    member: String
           |}
           |
           |list MixedList with [MixinList] {
           |}
           |
           |""".stripMargin
      )
    assertEitherIsRight(result)
  }

  test("map w/o members") {
    val result =
      IdlParser.idlParser.parseAll(
        """|$version: "2.0"
           |
           |namespace test
           |
           |@mixin
           |map MixinMap {
           |    key: String
           |    value: String
           |}
           |
           |map MixedMap with [MixinMap] {}
           |
           |""".stripMargin
      )
    assertEitherIsRight(result)
  }

  test("parse structure body with trailing space") {
    // trailing white space on `value: String` is important
    val result =
      IdlParser.idlParser.parseAll(
        """|$version: "2.0"
           |
           |namespace test
           |
           |structure GetSetInput {
           |    value: String 
           |}
           |""".stripMargin
      )
    assertEitherIsRight(result)
  }

  test("empty triple doc break") {
    val result =
      IdlParser.idlParser.parseAll(
        """|$version: "2.0"
           |
           |namespace test
           |
           |///
           |structure GetSetInput {
           |    value: String
           |}
           |""".stripMargin
      )
    assertEitherIsRight(result)
  }

  test("structure with multiple mixins") {
    val result =
      IdlParser.idlParser.parseAll(
        """|$version: "2.0"
           |
           |namespace test
           |
           |structure MyStruct with [
           |    Mixin1
           |    Mixin2
           |] {}
           |""".stripMargin
      )
    assertEitherIsRight(result)
  }

  test("format test - large documentation trait") {
    val tq = "\"\"\""
    val src = s"""|$$version: "2.0"
                  |
                  |namespace test
                  |
                  |@documentation(${tq}
                  |  value
                  |${tq})
                  |string Value
                  |
                  |""".stripMargin
    val result = IdlParser.idlParser.parseAll(src)
    assertEitherIsRight(result)
  }

  // format: off
  val parsables = Seq(
    "just output" ->               """|operation OpName {
                                      |  output: ShapeId
                                      |}""".stripMargin,
    "just input" ->                """|operation OpName {
                                      |  input: ShapeId
                                      |}""".stripMargin,
    "just errors" ->               """|operation OpName {
                                      |  errors: [ShapeId]
                                      |}""".stripMargin,
    "input/errors" ->              """|operation OpName {
                                      |  input: ShapeId
                                      |  errors: [ShapeId]
                                      |}""".stripMargin,
    "errors/input" ->              """|operation OpName {
                                      |  errors: [ShapeId]
                                      |  input: ShapeId
                                      |}""".stripMargin,
    "output/errors" ->             """|operation OpName {
                                      |  output: ShapeId
                                      |  errors: [ShapeId]
                                      |}""".stripMargin,
    "errors/output" ->             """|operation OpName {
                                      |  errors: [ShapeId]
                                      |  output: ShapeId
                                      |}""".stripMargin,
    "input/output" ->              """|operation OpName {
                                      |  input: ShapeId
                                      |  output: ShapeId
                                      |}""".stripMargin,
    "output/input" ->              """|operation OpName {
                                      |  output: ShapeId
                                      |  input: ShapeId
                                      |}""".stripMargin,
    "input/output/errors" ->       """|operation OpName {
                                      |  input: ShapeId
                                      |  output: ShapeId
                                      |  errors: [ShapeId]
                                      |}""".stripMargin,
    "output/input/errors" ->       """|operation OpName {
                                      |  output: ShapeId
                                      |  input: ShapeId
                                      |  errors: [ShapeId]
                                      |}""".stripMargin,
    "errors/input/output" ->       """|operation OpName {
                                      |  errors: [ShapeId]
                                      |  input: ShapeId
                                      |  output: ShapeId
                                      |}""".stripMargin,
    "errors/output/input" ->       """|operation OpName {
                                      |  errors: [ShapeId]
                                      |  output: ShapeId
                                      |  input: ShapeId
                                      |}""".stripMargin
  )
  // format: on
  parsables.map { case (name, op) =>
    test(s"operation with $name can be parsed") {
      val result = IdlParser.idlParser.parseAll(
        s"""|$$version: "2.0"
           |
           |namespace test
           |
           |$op
           |""".stripMargin
      )
      assertEitherIsRight(result)
    }
  }
}
