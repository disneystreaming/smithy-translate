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

package smithytranslate.compiler.internals

import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.{EnumShape, ShapeId, Shape => JShape}
import software.amazon.smithy.model.traits.{
  DocumentationTrait,
  LengthTrait,
  PatternTrait
}

import scala.jdk.CollectionConverters._

final class IModelToSmithyEnumSuite extends munit.FunSuite {

  private val enumId = DefId(Namespace(List("foo")), Name.arbitrary("Values"))
  private val shapeId = ShapeId.from("foo#Values")

  private def convert(
      values: Vector[String],
      hints: List[Hint] = Nil,
      useEnumTraitSyntax: Boolean = false
  ): JShape = {
    val compiler = new IModelToSmithy(useEnumTraitSyntax)
    val model = compiler(
      IModel(Vector(Enumeration(enumId, values, hints)), Vector.empty)
    )
    val validated = Model.assembler().addModel(model).assemble()
    assert(!validated.isBroken, validated.getValidationEvents.toString)
    model.expectShape(shapeId)
  }

  private def assertMembers(expected: Vector[(String, String)]): Unit = {
    val values = expected.map(_._2)
    val shape = convert(values).asInstanceOf[EnumShape]
    assertEquals(shape.getEnumValues.asScala.toMap, expected.toMap)
    assertEquals(shape.getEnumValues.size(), values.size)
  }

  test("preserve existing names when enum values do not collide") {
    assertMembers(
      Vector(
        "red" -> "red",
        "a_b" -> "a-b",
        "n3three" -> "3three",
        "MEMBER_3" -> "/",
        "docs" -> "/docs",
        "n1value" -> "/1value"
      )
    )
  }

  test("preserve valid names when sanitized values collide") {
    assertMembers(
      Vector(
        "a_b_2" -> "a-b",
        "a_b" -> "a_b",
        "a_b_1" -> "a_b_1",
        "ab_2" -> "a b",
        "AB" -> "AB",
        "ab_1" -> "ab_1"
      )
    )
  }

  test("preserve wire values that cannot form Smithy identifiers") {
    assertMembers(
      Vector(
        "MEMBER_0" -> "-",
        "MEMBER_1" -> "_",
        "MEMBER_2" -> "é",
        "MEMBER_3" -> "café",
        "MEMBER_4" -> "😀"
      )
    )
  }

  test("disambiguate case-insensitive names without taking reserved suffixes") {
    assertMembers(
      Vector(
        "red" -> "red",
        "RED_3" -> "RED",
        "red_1" -> "red_1",
        "Red_2" -> "Red_2",
        "red_4" -> "r e d"
      )
    )
  }

  test("disambiguate fallback names and digit prefixes") {
    assertMembers(
      Vector(
        "MEMBER_0_1" -> "!",
        "MEMBER_0" -> "MEMBER_0",
        "MEMBER_2_1" -> "/",
        "MEMBER_2" -> "MEMBER_2",
        "n5_1" -> "5",
        "n5" -> "n5"
      )
    )
  }

  List(false, true).foreach { useEnumTraitSyntax =>
    test(
      s"deduplicate enum values with useEnumTraitSyntax=$useEnumTraitSyntax"
    ) {
      val shape = convert(
        Vector("red", "red", "green"),
        useEnumTraitSyntax = useEnumTraitSyntax
      )
      val values = shape match {
        case enumShape: EnumShape =>
          enumShape.getEnumValues.values().asScala.toVector
        case _ =>
          shape
            .findTrait("smithy.api#enum")
            .get()
            .toNode
            .expectArrayNode()
            .getElements
            .asScala
            .map(_.expectObjectNode().expectStringMember("value").getValue)
            .toVector
      }
      assertEquals(values.sorted, Vector("green", "red"))
    }

    test(s"preserve enum hints with useEnumTraitSyntax=$useEnumTraitSyntax") {
      val shape = convert(
        Vector("red", "green"),
        List(
          Hint.Description("A color"),
          Hint.Length(Some(1L), Some(10L)),
          Hint.Pattern("[a-z]+")
        ),
        useEnumTraitSyntax
      )
      assertEquals(
        shape.expectTrait(classOf[DocumentationTrait]).getValue,
        "A color"
      )
      assertEquals(
        shape.expectTrait(classOf[LengthTrait]),
        LengthTrait.builder().min(1L).max(10L).build()
      )
      assertEquals(
        shape.expectTrait(classOf[PatternTrait]).getValue,
        "[a-z]+"
      )
    }
  }
}
