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

package smithytranslate.openapi

import cats.data.NonEmptyList
final class MultiFileSpec extends munit.FunSuite {

  /* .
   * \|-- foo.yaml
   * \|-- bar.yaml
   */
  test("multiple files - same directory") {
    val fooYml = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Object:
                     |      type: object
                     |      properties:
                     |        l:
                     |          type: integer
                     |""".stripMargin
    val barYml = """|openapi: '3.0.'
                   |info:
                   |  title: test
                   |  version: '1.0'
                   |paths: {}
                   |components:
                   |  schemas:
                   |    Test:
                   |      type: object
                   |      properties:
                   |        s:
                   |          type: string
                   |""".stripMargin

    val expectedFoo = """|namespace foo
                      |
                      |structure Object {
                      |    l: Integer,
                      |}
                      |""".stripMargin

    val expectedBar = """|namespace bar
                             |
                             |structure Test {
                             |    s: String,
                             |}
                             |""".stripMargin

    val inOne = TestUtils.ConversionTestInput(
      NonEmptyList.of("foo.yaml"),
      fooYml,
      expectedFoo
    )
    val inTwo = TestUtils.ConversionTestInput(
      NonEmptyList.of("bar.yaml"),
      barYml,
      expectedBar
    )
    TestUtils.runConversionTest(inOne, inTwo)
  }

  /* .
   * \|-- /foo
   * \| |-- bar.yaml
   * \| |-- /baz
   * \| | |-- bin.yaml
   */
  test("multiple files - child directory") {
    val barYml = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Object:
                     |      type: object
                     |      properties:
                     |        l:
                     |          type: integer
                     |""".stripMargin
    val binYml = """|openapi: '3.0.'
                   |info:
                   |  title: test
                   |  version: '1.0'
                   |paths: {}
                   |components:
                   |  schemas:
                   |    Test:
                   |      type: object
                   |      properties:
                   |        s:
                   |          type: string
                   |""".stripMargin

    val expectedBar = """|namespace foo.bar
                      |
                      |structure Object {
                      |    l: Integer,
                      |}
                      |""".stripMargin

    val expectedBin = """|namespace foo.baz.bin
                             |
                             |structure Test {
                             |    s: String,
                             |}
                             |""".stripMargin

    val inOne = TestUtils.ConversionTestInput(
      NonEmptyList.of("foo", "bar.yaml"),
      barYml,
      expectedBar
    )
    val inTwo = TestUtils.ConversionTestInput(
      NonEmptyList.of("foo", "baz", "bin.yaml"),
      binYml,
      expectedBin
    )
    TestUtils.runConversionTest(inOne, inTwo)
  }

  /* .
   * \|-- /foo
   * \| |-- bar.yaml
   * \| |-- /baz
   * \| | |-- bin.yaml
   */
  test("multiple files - import from child directory") {
    val barYml = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Object:
                     |      type: object
                     |      properties:
                     |        t:
                     |          $ref: './baz/bin.yaml#/components/schemas/Test'
                     |""".stripMargin
    val binYml = """|openapi: '3.0.'
                   |info:
                   |  title: test
                   |  version: '1.0'
                   |paths: {}
                   |components:
                   |  schemas:
                   |    Test:
                   |      type: object
                   |      properties:
                   |        s:
                   |          type: string
                   |""".stripMargin

    val expectedBar = """|namespace foo.bar
                      |
                      |use foo.baz.bin#Test
                      |
                      |structure Object {
                      |    t: Test,
                      |}
                      |""".stripMargin

    val expectedBin = """|namespace foo.baz.bin
                             |
                             |structure Test {
                             |    s: String,
                             |}
                             |""".stripMargin

    val inOne = TestUtils.ConversionTestInput(
      NonEmptyList.of("foo", "bar.yaml"),
      barYml,
      expectedBar
    )
    val inTwo = TestUtils.ConversionTestInput(
      NonEmptyList.of("foo", "baz", "bin.yaml"),
      binYml,
      expectedBin
    )
    TestUtils.runConversionTest(inOne, inTwo)
  }

  /* .
   * \|-- /foo
   * \| |-- bar.yaml
   * \| |-- /baz
   * \| | |-- bin.yaml
   */
  test("multiple files - import from child directory - file scheme") {
    val barYml = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Object:
                     |      type: object
                     |      properties:
                     |        t:
                     |          $ref: 'file://foo/baz/bin.yaml#/components/schemas/Test'
                     |""".stripMargin
    val binYml = """|openapi: '3.0.'
                   |info:
                   |  title: test
                   |  version: '1.0'
                   |paths: {}
                   |components:
                   |  schemas:
                   |    Test:
                   |      type: object
                   |      properties:
                   |        s:
                   |          type: string
                   |""".stripMargin

    val expectedBar = """|namespace foo.bar
                      |
                      |use foo.baz.bin#Test
                      |
                      |structure Object {
                      |    t: Test,
                      |}
                      |""".stripMargin

    val expectedBin = """|namespace foo.baz.bin
                             |
                             |structure Test {
                             |    s: String,
                             |}
                             |""".stripMargin

    val inOne = TestUtils.ConversionTestInput(
      NonEmptyList.of("foo", "bar.yaml"),
      barYml,
      expectedBar
    )
    val inTwo = TestUtils.ConversionTestInput(
      NonEmptyList.of("foo", "baz", "bin.yaml"),
      binYml,
      expectedBin
    )
    TestUtils.runConversionTest(inOne, inTwo)
  }

  /* .
   * \|-- /foo
   * \| |-- bar.yaml
   * \| |-- /baz
   * \| | |-- bin.yaml
   */
  test("multiple files - import from parent directory") {
    val binYml = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Object:
                     |      type: object
                     |      properties:
                     |        t:
                     |          $ref: '../bar.yaml#/components/schemas/Test'
                     |""".stripMargin
    val barYml = """|openapi: '3.0.'
                   |info:
                   |  title: test
                   |  version: '1.0'
                   |paths: {}
                   |components:
                   |  schemas:
                   |    Test:
                   |      type: object
                   |      properties:
                   |        s:
                   |          type: string
                   |""".stripMargin

    val expectedBin = """|namespace foo.baz.bin
                      |
                      |use foo.bar#Test
                      |
                      |structure Object {
                      |    t: Test,
                      |}
                      |""".stripMargin

    val expectedBar = """|namespace foo.bar
                             |
                             |structure Test {
                             |    s: String,
                             |}
                             |""".stripMargin

    val inOne = TestUtils.ConversionTestInput(
      NonEmptyList.of("foo", "bar.yaml"),
      barYml,
      expectedBar
    )
    val inTwo = TestUtils.ConversionTestInput(
      NonEmptyList.of("foo", "baz", "bin.yaml"),
      binYml,
      expectedBin
    )
    TestUtils.runConversionTest(inOne, inTwo)
  }

  /* .
   * \|-- /foo
   * \| |-- bar.yaml
   * \| |-- /baz
   * \| | |-- bin.yaml
   */
  test("multiple files - import from too far up") {
    val binYml = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Object:
                     |      type: object
                     |      properties:
                     |        t:
                     |          $ref: '../../../bar.yaml#/components/schemas/Test'
                     |""".stripMargin
    val barYml = """|openapi: '3.0.'
                   |info:
                   |  title: test
                   |  version: '1.0'
                   |paths: {}
                   |components:
                   |  schemas:
                   |    Test:
                   |      type: object
                   |      properties:
                   |        s:
                   |          type: string
                   |""".stripMargin

    val expectedBar = """|namespace foo.baz.bin
                      |
                      |use error#T
                      |
                      |structure Object {
                      |    t: T,
                      |}
                      |""".stripMargin

    val expectedBin = """|namespace foo.bar
                             |
                             |structure Test {
                             |    s: String,
                             |}
                             |""".stripMargin

    val expectedError = """|namespace error
                         |
                         |use smithytranslate#errorMessage
                         |
                         |@errorMessage("Ref ../../../bar.yaml#/components/schemas/Test goes too far up")
                         |structure T {}
                         |""".stripMargin

    val inOne = TestUtils.ConversionTestInput(
      NonEmptyList.of("foo", "bar.yaml"),
      barYml,
      expectedBar,
      Some(expectedError)
    )
    val inTwo = TestUtils.ConversionTestInput(
      NonEmptyList.of("foo", "baz", "bin.yaml"),
      binYml,
      expectedBin
    )
    val TestUtils.ConversionResult(
      OpenApiCompiler.Success(errors, output),
      expectedModel
    ) =
      TestUtils.runConversion(inOne, inTwo)
    val expectedErrors = List(
      ModelError.Restriction(
        "Ref ../../../bar.yaml#/components/schemas/Test goes too far up"
      )
    )
    assertEquals(errors, expectedErrors)
    assertEquals(output, expectedModel)
  }

  /* .
   * \|-- foo.yaml
   * \|-- bar.yaml
   */
  test("multiple files - property ref") {
    val fooYml = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Object:
                     |      type: object
                     |      properties:
                     |        l:
                     |          $ref: bar.yaml#/components/schemas/Test/properties/s
                     |""".stripMargin
    val barYml = """|openapi: '3.0.'
                   |info:
                   |  title: test
                   |  version: '1.0'
                   |paths: {}
                   |components:
                   |  schemas:
                   |    Test:
                   |      type: object
                   |      properties:
                   |        s:
                   |          type: string
                   |""".stripMargin

    val expectedFoo = """|namespace foo
                         |
                         |structure Object {
                         |    l: String
                         |}
                         |""".stripMargin

    val expectedBar = """|namespace bar
                         |
                         |structure Test {
                         |    s: String
                         |}
                         |""".stripMargin

    val inOne = TestUtils.ConversionTestInput(
      NonEmptyList.of("foo.yaml"),
      fooYml,
      expectedFoo
    )
    val inTwo = TestUtils.ConversionTestInput(
      NonEmptyList.of("bar.yaml"),
      barYml,
      expectedBar
    )
    TestUtils.runConversionTest(inOne, inTwo)
  }

  /* .
   * \|-- foo.yaml
   * \|-- bar.yaml
   */
  test("multiple files - property ref object type") {
    val fooYml = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Object:
                     |      type: object
                     |      properties:
                     |        l:
                     |          $ref: bar.yaml#/components/schemas/Test/properties/s
                     |""".stripMargin
    val barYml = """|openapi: '3.0.'
                   |info:
                   |  title: test
                   |  version: '1.0'
                   |paths: {}
                   |components:
                   |  schemas:
                   |    Test:
                   |      type: object
                   |      properties:
                   |        s:
                   |          $ref: '#/components/schemas/Bar'
                   |    Bar:
                   |      type: object
                   |      properties:
                   |        b:
                   |          type: string
                   |""".stripMargin

    val expectedFoo = """|namespace foo
                         |
                         |use bar#Bar
                         |
                         |structure Object {
                         |    l: Bar
                         |}
                         |""".stripMargin

    val expectedBar = """|namespace bar
                         |
                         |structure Bar {
                         |  b: String
                         |}
                         |
                         |structure Test {
                         |    s: Bar
                         |}
                         |""".stripMargin

    val inOne = TestUtils.ConversionTestInput(
      NonEmptyList.of("foo.yaml"),
      fooYml,
      expectedFoo
    )
    val inTwo = TestUtils.ConversionTestInput(
      NonEmptyList.of("bar.yaml"),
      barYml,
      expectedBar
    )
    TestUtils.runConversionTest(inOne, inTwo)
  }

}
