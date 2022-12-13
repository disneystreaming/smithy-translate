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

import sample_specs._
import smithytranslate.closure.ModelOps._
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.{ShapeId, StringShape}

class ClosureSpec extends munit.FunSuite {

  def inLineModel(src: String): Model = {
    Model
      .assembler()
      .discoverModels()
      .addUnparsedModel("/model.smithy", src)
      .assemble()
      .unwrap()
  }

  test("when root shape id is passed in ,expect to retain entire model") {
    val model1 = inLineModel(sampleSpec)
    val result = model1
      .transitiveClosure(
        List(ShapeId.from("example.weather#Weather")),
        captureTraits = true,
        captureMetadata = true
      )
      .check()

    assertEquals(
      result
        .getMetadataProperty("some_key")
        .flatMap(_.asStringNode().map(_.getValue)),
      java.util.Optional.of("some value")
    )
    assertEquals(result.prettyPrint, model1.prettyPrint)
  }

  test("specify leaf node , only leaf remains") {

    val leaf =
      """
        |namespace example.weather
        |@output
        |structure GetForecastOutput {
        |    chanceOfRain: Float
        |}
                 """.stripMargin
    val model1 = inLineModel(sampleSpec)
    val expected = Model
      .assembler()
      .addUnparsedModel("/model.smithy", leaf)
      .assemble()
      .unwrap()
    val result = model1
      .transitiveClosure(
        List(ShapeId.from("example.weather#GetForecastOutput")),
        captureTraits = true
      )
      .check()

    assertEquals(result.prettyPrint, expected.prettyPrint)
  }

  test("complex trait example") {

    val complexTraitModel = inLineModel(complexTrait)
    val expected = complexTraitModel.toBuilder
      .removeShape(ShapeId.from("example.city#Test"))
      .build
      .check()
    val result = complexTraitModel
      .transitiveClosure(List(ShapeId.from("example.city#location")), true)
      .check()
    assertEquals(result.prettyPrint, expected.prettyPrint)
  }

  test(
    "when capture traits is set to false , shapes marked as trait and their dependencies are removed"
  ) {

    val model = inLineModel(foospec)
    val expected = Model
      .assembler()
      .addUnparsedModel("/model.smithy", noTraitSpec)
      .assemble()
      .unwrap()
    val result = model
      .transitiveClosure(List(ShapeId.from("foo#MyString1")), false)
      .check()
    assertEquals(result.prettyPrint, expected.prettyPrint)
  }
  test(
    "targeting a trait , preserves trait regardless of traitcapture status"
  ) {
    val model = inLineModel(foospec)
    val result = model.transitiveClosure(List(ShapeId.from("foo#foo"))).check()
    val expected = Model
      .assembler()
      .addUnparsedModel("/model.smithy", traitOnlyRes)
      .assemble()
      .unwrap()
    assertEquals(result.prettyPrint, expected.prettyPrint)
  }

  test(
    "targeting all roots (multiple starting points) with traitCapture set to true , preserves all shapes"
  ) {
    val model = inLineModel(foospec)
    val result = model
      .transitiveClosure(
        List(ShapeId.from("foo#MyString1"), ShapeId.from("foo#MyString2")),
        true
      )
      .check()
    val expected = Model
      .assembler()
      .addUnparsedModel("/model.smithy", foospec)
      .assemble()
      .unwrap()
    assertEquals(result.prettyPrint, expected.prettyPrint)
  }
  test(
    "targeting multiple roots (multiple starting points) with traitCapture set to false "
  ) {

    val model = inLineModel(foospec)
    val result = model
      .transitiveClosure(
        List(ShapeId.from("foo#MyString1"), ShapeId.from("foo#MyString2"))
      )
      .check()
    val expected = Model
      .assembler()
      .addShape(StringShape.builder().id(ShapeId.from("foo#MyString1")).build())
      .addShape(StringShape.builder().id(ShapeId.from("foo#MyString2")).build())
      .assemble()
      .unwrap()
    assertEquals(result.prettyPrint, expected.prettyPrint)
  }

  test("protocolDefinition-referenced traits should be retained") {
    val model1 = inLineModel(protocolSpec)
    val result = model1
      .transitiveClosure(List(ShapeId.from("example.test#MyService")), true)
      .check()

    assertEquals(result.prettyPrint, model1.prettyPrint)
  }

  test("shapes targeted by idRefs in trait instances should be captured") {
    val model1 = inLineModel(idRefSpec)
    val result = model1
      .transitiveClosure(
        List(
          ShapeId.from("example.test#MyString"),
          ShapeId.from("example.test#MyString2")
        ),
        true
      )
      .check()

    assertEquals(result.prettyPrint, model1.prettyPrint)
  }

  test(
    "validate model should catch errors in the model"
  ) {

    val model = inLineModel(foospec)
      .toBuilder()
      .removeShape(ShapeId.fromParts("foo", "Bar")) // make model invalid
      .build()
    val result = util.Try(
      model
        .transitiveClosure(
          List(ShapeId.from("foo#MyString1")),
          captureTraits = true,
          validateModel = true
        )
    )
    assert(result.isFailure)
  }

  test(
    "should not catch errors in the model when validate is false"
  ) {

    val model = inLineModel(foospec)
      .toBuilder()
      .removeShape(ShapeId.fromParts("foo", "Bar")) // make model invalid
      .build()
    val result = util.Try(
      model
        .transitiveClosure(
          List(ShapeId.from("foo#MyString1")),
          captureTraits = true,
          validateModel = false
        )
    )
    assert(result.isSuccess)
  }

  test(
    "should handle specs with cycles ------------------"
  ) {
    val model = inLineModel(cycleSpec)
      .toBuilder()
      .build()
    
    val result = util.Try(
      model
        .transitiveClosure(
          List(ShapeId.from("example.test#SomeUnion")),
          captureTraits = true,
          validateModel = true
        )
    )
    assert(result.isSuccess)
  }
}
