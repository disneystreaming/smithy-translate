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

package smithytranslate.compiler.internals.openapi

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import java.util.Collections.singleton
import java.util.Collections.singletonList
import scala.jdk.CollectionConverters._
import smithytranslate.compiler.internals.Primitive
import smithytranslate.compiler.internals.Primitive._
import smithytranslate.compiler.openapi.TestUtils.OpenApiVersion

final class SchemaReaderSpec extends munit.FunSuite {
  private def reader(version: OpenApiVersion): SchemaReader =
    new SchemaReader(new OpenAPI().openapi(version.value))

  private val reader30 = reader(OpenApiVersion.V3_0)
  private val reader31 = reader(OpenApiVersion.V3_1)

  private def generic(dataType: String): Schema[AnyRef] = {
    val schema = new Schema[AnyRef]()
    schema.setType(dataType)
    schema
  }

  private val primitives: List[(String, Primitive)] = List(
    "string" -> PString,
    "integer" -> PInt,
    "number" -> PDouble,
    "boolean" -> PBoolean
  )

  primitives.foreach { case (dataType, expected) =>
    test(s"OpenAPI 3.0 generic $dataType uses type without types") {
      val schema = generic(dataType)
      assertEquals(schema.getTypes, null)
      assertEquals(reader30.unapply(schema), Some(expected))
    }
  }

  test("OpenAPI 3.0 setType remains authoritative when types is stale") {
    val schema = generic("string")
    schema.setTypes(singleton("string"))
    schema.setType("integer")

    assertEquals(schema.getTypes, singleton("string"))
    assertEquals(reader30.unapply(schema), Some(PInt))
    schema.setEnum(Vector[AnyRef]("red").asJava)
    assertEquals(reader30.StringEnum.unapply(schema), None)
    schema.setType("string")
    schema.setTypes(singleton("integer"))
    assertEquals(reader30.StringEnum.unapply(schema), Some(Vector("red")))
  }

  test("OpenAPI 3.1 types remains authoritative when type conflicts") {
    val schema = generic("integer")
    schema.setTypes(singleton("string"))

    assertEquals(reader31.unapply(schema), Some(PString))
    schema.setEnum(Vector[AnyRef]("red").asJava)
    assertEquals(reader31.StringEnum.unapply(schema), Some(Vector("red")))
  }

  test("OpenAPI 3.1 does not infer types from the OpenAPI 3.0 type field") {
    val schema = generic("string")
    assertEquals(reader31.unapply(schema), None)
    schema.setEnum(Vector[AnyRef]("red").asJava)
    assertEquals(reader31.StringEnum.unapply(schema), None)
    schema.setTypes(singleton("integer"))
    assertEquals(reader31.StringEnum.unapply(schema), None)
  }

  List("byte", "binary").foreach { format =>
    test(
      s"$format retains the 3.0 bytes mapping and explicit 3.1 restriction"
    ) {
      val schema = generic("string")
      schema.setTypes(singleton("string"))
      schema.setFormat(format)

      assertEquals(reader30.unapply(schema), Some(PBytes))
      assertEquals(reader30.restriction(schema), None)
      assertEquals(reader31.unapply(schema), None)
      assert(reader31.restriction(schema).exists(_.message.contains("format")))
      schema.setEnum(Vector[AnyRef]("Ymx1ZQ==").asJava)
      assertEquals(reader30.StringEnum.unapply(schema), None)
    }
  }

  private val nonPrimitiveSchemas: List[(String, Schema[AnyRef] => Unit)] =
    List(
      "$ref" -> (_.set$ref("#/components/schemas/Other")),
      "allOf" -> (_.setAllOf(singletonList[Schema[_]](generic("string")))),
      "oneOf" -> (_.setOneOf(singletonList[Schema[_]](generic("string")))),
      "anyOf" -> (_.setAnyOf(singletonList[Schema[_]](generic("string"))))
    )

  nonPrimitiveSchemas.foreach { case (keyword, addKeyword) =>
    test(s"OpenAPI 3.0 typed schema with $keyword is not a plain scalar") {
      val schema = generic("string")
      addKeyword(schema)

      assertEquals(reader30.unapply(schema), None)
    }
  }

  List[(String, Schema[AnyRef] => Unit)](
    "multiple types" -> (_.setTypes(Set("string", "integer").asJava)),
    "null" -> (_.setTypes(singleton("null"))),
    "$ref siblings" -> (_.set$ref("#/components/schemas/Other")),
    "oneOf" -> (_.setOneOf(singletonList[Schema[_]](generic("string"))))
  ).foreach { case (keyword, modify) =>
    test(s"OpenAPI 3.1 reports unsupported scalar $keyword") {
      val schema = generic("string")
      schema.setTypes(singleton("string"))
      modify(schema)
      assert(reader31.restriction(schema).exists(_.message.contains(keyword)))
    }
  }

  List[(String, Schema[AnyRef] => Unit)](
    "$schema" -> (_.set$schema("https://example.com/dialect")),
    "$id" -> (_.set$id("https://example.com/schema"))
  ).foreach { case (keyword, modify) =>
    test(s"OpenAPI 3.1 rejects $keyword on parent objects too") {
      val parent = generic("object")
      parent.setTypes(singleton("object"))
      parent.addProperty("value", generic("string"))
      modify(parent)
      assert(reader31.restriction(parent).exists(_.message.contains(keyword)))
    }
  }

  List(
    "https://spec.openapis.org/oas/3.1/dialect/base" -> true,
    "https://json-schema.org/draft/2020-12/schema" -> true,
    "https://example.com/dialect" -> false
  ).foreach { case (dialect, supported) =>
    test(s"OpenAPI 3.1 document dialect: $dialect") {
      val openApi = new OpenAPI()
        .openapi(OpenApiVersion.V3_1.value)
        .jsonSchemaDialect(dialect)
      val schema = generic("string")
      schema.setTypes(singleton("string"))
      assertEquals(
        new SchemaReader(openApi).restriction(schema).isEmpty,
        supported
      )
    }
  }

  test("OpenAPI 3.1 reports a missing schema") {
    assertEquals(reader31.unapply(null), None)
    assertEquals(reader31.StringEnum.unapply(null), None)
    assert(reader31.restriction(null).isDefined)
  }

  test("OpenAPI 3.1 empty enum arrays fall back to the primitive") {
    val schema = generic("string")
    schema.setTypes(singleton("string"))
    schema.setEnum(Vector.empty[AnyRef].asJava)
    assertEquals(reader31.StringEnum.unapply(schema), None)
    assertEquals(reader31.restriction(schema), None)
    assertEquals(reader31.unapply(schema), Some(PString))
  }

  test("OpenAPI 3.1 diagnoses enum siblings on otherwise untyped references") {
    val schema = new Schema[AnyRef]()
    schema.set$ref("#/components/schemas/Other")
    schema.setEnum(Vector[AnyRef]("red").asJava)
    assert(
      reader31.restriction(schema).exists(_.message.contains("$ref siblings"))
    )
  }
}
