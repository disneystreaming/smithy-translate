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
import software.amazon.smithy.model.shapes.LongShape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.RangeTrait
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.FloatShape
final class ConstraintSpec extends munit.FunSuite {

  test("length - string") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    MyString:
                     |      type: string
                     |      minLength: 3
                     |      maxLength: 20
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |@length(min: 3, max: 20)
                      |string MyString
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("length - lists") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    MyArray:
                     |      type: array
                     |      minItems: 1
                     |      maxItems: 10
                     |      items:
                     |        type: string
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |@length(min: 1, max: 10)
                      |list MyArray {
                      |    member: String
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("length - set") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    MySet:
                     |      type: array
                     |      minItems: 1
                     |      maxItems: 10
                     |      uniqueItems: true
                     |      items:
                     |        type: string
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |@length(min: 1, max: 10)
                      |@uniqueItems
                      |list MySet {
                      |    member: String
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(
      openapiString,
      expectedString
    )
  }

  test("length - map") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    MyMap:
                     |      type: object
                     |      minItems: 1
                     |      maxItems: 10
                     |      additionalProperties:
                     |        type: string
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |@length(min: 1, max: 10)
                      |map MyMap {
                      |    key: String,
                      |    value: String
                      |}
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("range - long") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    MyLong:
                     |      type: integer
                     |      format: int64
                     |      minimum: 3
                     |      maximum: 20
                     |""".stripMargin

    val rangeTrait = RangeTrait
      .builder()
      .min(new java.math.BigDecimal(3))
      .max(new java.math.BigDecimal(20))
      .build()
    val myLong = LongShape
      .builder()
      .id(ShapeId.fromParts("foo", "MyLong"))
      .addTrait(rangeTrait)
      .build()
    val expectedModel = Model.builder().addShape(myLong).build()

    // Tested using this function since the ModelAssembler automatically adds box traits
    // to primitive shapes when loading them from string, but not when loading them using the
    // builders like above.
    TestUtils.runConversionTestWithModel(openapiString, expectedModel)
  }

  test("range - exclusive") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    MyLong:
                     |      type: integer
                     |      format: int64
                     |      minimum: 3
                     |      exclusiveMinimum: true
                     |      maximum: 20
                     |      exclusiveMaximum: true
                     |""".stripMargin

    val rangeTrait = RangeTrait
      .builder()
      .min(new java.math.BigDecimal(4))
      .max(new java.math.BigDecimal(19))
      .build()
    val myLong = LongShape
      .builder()
      .id(ShapeId.fromParts("foo", "MyLong"))
      .addTrait(rangeTrait)
      .build()
    val expectedModel = Model.builder().addShape(myLong).build()

    // Tested using this function since the ModelAssembler automatically adds box traits
    // to primitive shapes when loading them from string, but not when loading them using the
    // builders like above.
    TestUtils.runConversionTestWithModel(openapiString, expectedModel)
  }

  test("range - exclusive on decimal type") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    MyFloat:
                     |      type: number
                     |      format: float
                     |      minimum: 3
                     |      exclusiveMinimum: true
                     |      maximum: 20
                     |      exclusiveMaximum: true
                     |""".stripMargin

    val rangeTrait = RangeTrait
      .builder()
      .min(new java.math.BigDecimal(3))
      .max(new java.math.BigDecimal(20))
      .build()
    val myFloat = FloatShape
      .builder()
      .id(ShapeId.fromParts("foo", "MyFloat"))
      .addTrait(rangeTrait)
      .build()
    val expectedModel = Model.builder().addShape(myFloat).build()

    // Tested using this function since the ModelAssembler automatically adds box traits
    // to primitive shapes when loading them from string, but not when loading them using the
    // builders like above.
    TestUtils.runConversionTestWithModel(openapiString, expectedModel)
    val input = TestUtils.ConversionTestInput(
      NonEmptyList.of("foo.smithy"),
      openapiString,
      expectedModel,
      None,
      OpenApiCompiler.SmithyVersion.Two
    )
    val TestUtils.ConversionResult(
      OpenApiCompiler.Success(errors, output),
      expected
    ) =
      TestUtils.runConversion(input)
    val expectedErrors = List(
      ModelError.Restriction(
        "Unable to automatically account for exclusiveMin/Max on decimal type Float"
      )
    )
    assertEquals(output, expected)
    assertEquals(errors, expectedErrors)
  }

  test("pattern") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    MyString:
                     |      type: string
                     |      pattern: '^\d{3}-\d{2}-\d{4}$'
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |@pattern("^\\d{3}-\\d{2}-\\d{4}$")
                      |string MyString
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

  test("sensitive") {
    val openapiString = """|openapi: '3.0.'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    MyString:
                     |      type: string
                     |      format: password
                     |""".stripMargin

    val expectedString = """|namespace foo
                      |
                      |@sensitive
                      |string MyString
                      |""".stripMargin

    TestUtils.runConversionTest(openapiString, expectedString)
  }

}
