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

package smithytranslate.proto3.internals

import munit._
import ProtoIR._
import Statement._
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.StringShape
import software.amazon.smithy.model.shapes.StructureShape
import alloy.proto.ProtoEnabledTrait
import software.amazon.smithy.model.shapes.ShapeId
import scala.jdk.OptionConverters._

class CompilerSuite extends FunSuite {

  private val someStruct = TopLevelDef.MessageDef(
    Message(
      "Struct",
      List(
        MessageElement.FieldElement(
          Field(
            deprecated = false,
            Type.String,
            "value",
            1,
            None
          )
        )
      ),
      Nil,
      None
    )
  )

  test("compile a simple smithy model") {
    val namespace = "com.example"
    val model = {
      val mb = Model.builder()
      mb.addShape(
        StringShape
          .builder()
          .id(ShapeId.fromParts("com.example", "MyString"))
          .build()
      )
      mb.addShape(
        StructureShape
          .builder()
          .addTrait(new ProtoEnabledTrait())
          .id(s"$namespace#Struct")
          .addMember("value", ShapeId.fromParts("com.example", "MyString"))
          .build()
      )
      mb.build()
    }
    val sut = new Compiler(model, allShapes = true)
    val actual = sut.compile()
    val expected = List(
      OutputFile(
        List(
          "com",
          "example",
          "example.proto"
        ),
        CompilationUnit(
          Some(
            "com.example"
          ),
          List(
            TopLevelStatement(
              someStruct
            )
          ),
          List.empty
        )
      )
    )

    Assertions.assertEquals(actual, expected)
  }

  test("correctly choose file name - all caps") {
    namespaceTest("com.EXAMPLE", List("com", "EXAMPLE", "example.proto"))
  }

  test("correctly choose file name - underscore") {
    namespaceTest(
      "com.some_example",
      List("com", "some_example", "some_example.proto")
    )
  }

  test("correctly choose file name - leading underscore") {
    namespaceTest("com._example", List("com", "_example", "_example.proto"))
    namespaceTest("com._EXAMPLE", List("com", "_EXAMPLE", "_example.proto"))
    namespaceTest("com._Example", List("com", "_Example", "_example.proto"))
  }

  test("correctly choose file name - underscore and caps") {
    namespaceTest(
      "com.some_EXAMPLE",
      List("com", "some_EXAMPLE", "some_example.proto")
    )
    namespaceTest(
      "com.SOME_EXAMPLE",
      List("com", "SOME_EXAMPLE", "some_example.proto")
    )
    namespaceTest(
      "com.Some_Example",
      List("com", "Some_Example", "some_example.proto")
    )
    namespaceTest(
      "com.Some_OTHER_Example",
      List("com", "Some_OTHER_Example", "some_other_example.proto")
    )
  }

  test("hasProtoCompact returns true when PROTOBUF value present in protoOffsetDateTimeFormat trait") {
    val spec =
      """$version: "2"
        |
        |namespace example
        |
        |use alloy#OffsetDateTime
        |use alloy.proto#protoOffsetDateTimeFormat
        |
        |structure Timer {
        |  @protoOffsetDateTimeFormat("PROTOBUF")
        |  start: OffsetDateTime
        |}
        |""".stripMargin

    val assembledSpec = Model
      .assembler()
      .discoverModels()
      .addUnparsedModel("spec.smithy", spec)
      .assemble()
      .validate()
      .get()

    val timerShape = assembledSpec.expectShape(ShapeId.fromParts("example", "Timer"), classOf[StructureShape])

    val result = for {
      member <- timerShape.getMember("start").toScala
      target = assembledSpec.expectShape(member.getTarget)
    } yield Compiler.hasProtoCompact(member, target)

    assertEquals(
      result,
      Some(true)
    )
  }

  private def namespaceTest(namespace: String, expectedFilePath: List[String])(
      implicit loc: Location
  ): Unit = {
    val model = {
      val mb = Model.builder()
      mb.addShape(
        StringShape
          .builder()
          .id(ShapeId.fromParts("com.example", "MyString"))
          .build()
      )
      mb.addShape(
        StructureShape
          .builder()
          .addTrait(new ProtoEnabledTrait())
          .id(s"$namespace#Struct")
          .addMember("value", ShapeId.fromParts("com.example", "MyString"))
          .build()
      )
      mb.build()
    }
    val sut = new Compiler(model, allShapes = true)
    val actual = sut.compile()
    val expected = List(
      OutputFile(
        expectedFilePath,
        CompilationUnit(
          Some(
            namespace
          ),
          List(
            TopLevelStatement(
              someStruct
            )
          ),
          List.empty
        )
      )
    )

    Assertions.assertEquals(actual, expected)
  }

}
