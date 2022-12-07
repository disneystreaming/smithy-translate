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

  test("enum with commas") {
    val result =
      IdlParser.idlParser.parseAll(
        """|$version: "2.0"
           |
           |namespace test
           |
           |enum OtherEnum {
           |    V1 = "v1",
           |    V2 = "v2"
           |}
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
}
