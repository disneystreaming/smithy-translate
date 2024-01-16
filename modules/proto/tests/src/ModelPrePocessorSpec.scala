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

package smithyproto.proto3

import munit._
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes._
import software.amazon.smithy.build.ProjectionTransformer
import software.amazon.smithy.build.TransformContext

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters.RichOptional

class ModelPrePocessorSpec extends FunSuite {
  private def transitiveIsApplied(
      allowedNamespace: Option[String]
  )(implicit loc: Location) = {
    val smithy = s"""|namespace test
                     |
                     |use alloy.proto#protoEnabled
                     |
                     |@protoEnabled
                     |structure Test {
                     |  s: String
                     |}
                     |
                     |/// This one should not be converted
                     |structure Other {
                     |  i: Integer
                     |}
                     |""".stripMargin
    checkTransformer(
      smithy,
      ModelPreProcessor.transformers.Transitive(allowedNamespace)
    ) { case (original, transformed) =>
      val removed = Set(
        ShapeId.from("test#Other$i"),
        ShapeId.from("test#Other")
      )
      removed.foreach { sId =>
        assert(original.getShape(sId).isPresent())
        assert(transformed.getShape(sId).isEmpty())
      }
      val kept = Set(ShapeId.from("test#Test$s"), ShapeId.from("test#Test"))
      kept.foreach { sId =>
        assert(original.getShape(sId).isPresent())
        assert(transformed.getShape(sId).isPresent())
      }
    }
  }

  test("apply Transitive on protoEnabled w/o an allowed namespace") {
    transitiveIsApplied(None)
  }

  test("apply Transitive on protoEnabled w/ an allowed namespace") {
    transitiveIsApplied(Some("test"))
  }

  test("apply Transitive does nothing if namespace is excluded") {
    val smithy = s"""|namespace test
                     |
                     |use alloy.proto#protoEnabled
                     |
                     |@protoEnabled
                     |structure Test {
                     |  s: String
                     |}
                     |
                     |/// This one should not be converted
                     |structure Other {
                     |  i: Integer
                     |}
                     |""".stripMargin
    checkTransformer(
      smithy,
      ModelPreProcessor.transformers.Transitive(Some("other_ns"))
    ) { case (original, transformed) =>
      val removed = Set(
        ShapeId.from("test#Test$s"),
        ShapeId.from("test#Test"),
        ShapeId.from("test#Other$i"),
        ShapeId.from("test#Other")
      )
      removed.foreach { sId =>
        assert(original.getShape(sId).isPresent())
        assert(transformed.getShape(sId).isEmpty())
      }
    }
  }

  test("smithytranslate UUID is not included if alloy#UUID is not used") {
    val smithy = s"""|namespace test
                     |
                     |structure Test {
                     |  @required
                     |  id: String
                     |}
                     |""".stripMargin
    checkTransformer(
      smithy,
      ModelPreProcessor.transformers.CompactUUID
    ) { case (original, transformed) =>
      assert(original.getShape(ShapeId.from("alloy#UUID")).isPresent())
      assert(
        transformed.getShape(ShapeId.from("smithytranslate#UUID")).isEmpty()
      )
    }
  }

  test("alloy#UUID is converted to smithytranslate#UUID") {
    val smithy = s"""|namespace test
                     |
                     |use alloy#UUID
                     |
                     |structure Test {
                     |  @required
                     |  id: UUID
                     |}
                     |""".stripMargin
    checkTransformer(
      smithy,
      ModelPreProcessor.transformers.CompactUUID
    ) { case (original, transformed) =>
      val removed = Set(
        ShapeId.from("alloy#UUID")
      )
      removed.foreach { sId =>
        assert(original.getShape(sId).isPresent())
        assert(transformed.getShape(sId).isEmpty())
      }
      assert(
        transformed.getShape(ShapeId.from("smithytranslate#UUID")).isPresent()
      )
    }
  }

  def testPreludeReplacements(
      name: String,
      smithyShape: String,
      present: Set[ShapeId],
      absent: Set[ShapeId]
  ) = {

    test(
      s"PreludeReplacements - $name"
    ) {
      val smithy = s"""|namespace test
                       |
                       |use alloy.proto#protoEnabled
                       |
                       |@protoEnabled
                       |structure Test {
                       |  s: $smithyShape
                       |}
                       |""".stripMargin
      checkTransformer(
        smithy,
        ModelPreProcessor.transformers.Transitive(None)
      ) { case (_, pruned) =>
        val resultModel = process(
          pruned,
          ModelPreProcessor.transformers.PreludeReplacements
        )

        resultModel
          .shapes()
          .iterator()
          .asScala
          .filter(_.getId().getNamespace() != "smithy.api")
          .foreach(println)

        present.foreach { sId =>
          assert(resultModel.getShape(sId).isPresent(), sId)
        }

        absent.foreach { sId =>
          assert(resultModel.getShape(sId).isEmpty(), sId)
        }
      }
    }
  }

  testPreludeReplacements(
    "keep big int",
    "BigInteger",
    present = Set(
      ShapeId.from("smithytranslate#BigInteger")
    ),
    absent = Set(
      ShapeId.from("smithytranslate#BigDecimal"),
      ShapeId.from("smithytranslate#Timestamp"),
      ShapeId.from("smithytranslate#Document")
    )
  )

  testPreludeReplacements(
    "keep timestamp",
    "Timestamp",
    present = Set(
      ShapeId.from("smithytranslate#Timestamp")
    ),
    absent = Set(
      ShapeId.from("smithytranslate#BigDecimal"),
      ShapeId.from("smithytranslate#BigInteger"),
      ShapeId.from("smithytranslate#Document")
    )
  )

  testPreludeReplacements(
    "keep big decimal",
    "BigDecimal",
    present = Set(
      ShapeId.from("smithytranslate#BigDecimal")
    ),
    absent = Set(
      ShapeId.from("smithytranslate#Timestamp"),
      ShapeId.from("smithytranslate#BigInteger"),
      ShapeId.from("smithytranslate#Document")
    )
  )

  testPreludeReplacements(
    "keep document",
    "Document",
    present = Set(
      ShapeId.from("smithytranslate#Document"),
      ShapeId.from("smithytranslate#DNull"),
      ShapeId.from("smithytranslate#DObject"),
      ShapeId.from("smithytranslate#DArray"),
      ShapeId.from("smithytranslate#DBoolean"),
      ShapeId.from("smithytranslate#DNumber"),
      ShapeId.from("smithytranslate#DString")
    ),
    absent = Set(
      ShapeId.from("smithytranslate#Timestamp"),
      ShapeId.from("smithytranslate#BigInteger"),
      ShapeId.from("smithytranslate#BigDecimal")
    )
  )

  test("apply PreventEnumConflicts") {
    val smithy =
      """|$version: "2"
                     |namespace test
                     |
                     |enum Enum1 {
                     |  VUNIQUE1
                     |  VCONFLICT
                     |}
                     |
                     |enum Enum2{
                     |  VUNIQUE2
                     |  VCONFLICT
                     |}
                     |
                     |enum Enum3{
                     |  VUNIQUE3
                     |  @enumValue("VCONFLICT")
                     |  VCONFLICT3
                     |}
                     |
                     |intEnum Enum4{
                     |  @enumValue(1)
                     |  VUNIQUE4
                     |  @enumValue(2)
                     |  VCONFLICT3
                     |}
                     |
                     |enum NoConflict{
                     |  V1
                     |  V2
                     |  }
                     |""".stripMargin
    checkTransformer(
      smithy,
      ModelPreProcessor.transformers.PreventEnumConflicts
    ) { case (original, transformed) =>
      def getEnumNames(m: Model, shapeId: ShapeId): List[String] = {
        m.getShape(shapeId)
          .toScala
          .toList
          .collect {
            case shape: EnumShape =>
              shape.getMemberNames.asScala.toList
            case shape: IntEnumShape =>
              shape.getMemberNames.asScala.toList
          }
          .flatten
      }

      assertEquals(
        getEnumNames(original, ShapeId.from("test#Enum1")),
        List("VUNIQUE1", "VCONFLICT")
      )
      assertEquals(
        getEnumNames(original, ShapeId.from("test#Enum2")),
        List("VUNIQUE2", "VCONFLICT")
      )
      assertEquals(
        getEnumNames(original, ShapeId.from("test#Enum3")),
        List("VUNIQUE3", "VCONFLICT3")
      )

      assertEquals(
        getEnumNames(transformed, ShapeId.from("test#Enum1")),
        List("VUNIQUE1", "ENUM1_VCONFLICT")
      )
      assertEquals(
        getEnumNames(transformed, ShapeId.from("test#Enum2")),
        List("VUNIQUE2", "ENUM2_VCONFLICT")
      )
      assertEquals(
        getEnumNames(transformed, ShapeId.from("test#NoConflict")),
        List("V1", "V2")
      )
      assertEquals(
        getEnumNames(transformed, ShapeId.from("test#Enum3")),
        List("VUNIQUE3", "ENUM3_VCONFLICT3")
      )
      assertEquals(
        getEnumNames(transformed, ShapeId.from("test#Enum4")),
        List("VUNIQUE4", "ENUM4_VCONFLICT3")
      )
    }
  }

  test("apply PreventEnumConflicts - across namespace") {
    val smithyTest =
      """|$version: "2"
         |namespace test
         |
         |enum Enum1 {
         |  VCONFLICT
         |}
         |
         |enum Enum2 {
         |  VCONFLICT
         |}
         |""".stripMargin

    val other =
      """|$version: "2"
            |namespace a.ns
            |
            |enum OtherEnum {
            |  VCONFLICT
            |}
            |""".stripMargin
    val original = buildModel(smithyTest, other)
    val transformed =
      process(original, ModelPreProcessor.transformers.PreventEnumConflicts)
    def getEnumNames(m: Model, shapeId: ShapeId): List[String] = {
      m.getShape(shapeId)
        .toScala
        .toList
        .collect {
          case shape: EnumShape =>
            shape.getMemberNames.asScala.toList
          case shape: IntEnumShape =>
            shape.getMemberNames.asScala.toList
        }
        .flatten
    }

    assertEquals(
      getEnumNames(original, ShapeId.from("test#Enum1")),
      List("VCONFLICT")
    )

    assertEquals(
      getEnumNames(transformed, ShapeId.from("test#Enum1")),
      List("ENUM1_VCONFLICT")
    )

    assertEquals(
      getEnumNames(original, ShapeId.from("test#Enum2")),
      List("VCONFLICT")
    )

    assertEquals(
      getEnumNames(transformed, ShapeId.from("test#Enum2")),
      List("ENUM2_VCONFLICT")
    )

    assertEquals(
      getEnumNames(original, ShapeId.from(s"a.ns#OtherEnum")),
      List("VCONFLICT")
    )

    assertEquals(
      getEnumNames(transformed, ShapeId.from(s"a.ns#OtherEnum")),
      List("VCONFLICT")
    )
  }

  private def checkTransformer(src: String, t: ProjectionTransformer)(
      check: (Model, Model) => Unit
  ): Unit = {
    val original = buildModel(src)
    val transformed = process(original, t)
    check(original, transformed)
  }

  private def buildModel(srcs: String*): Model = {
    val assembler = Model
      .assembler()
      .discoverModels()

    srcs.zipWithIndex.foreach { case (s, i) =>
      assembler.addUnparsedModel(s"inlined-in-test.$i.smithy", s)
    }

    assembler.assemble().unwrap()
  }

  private def process(m: Model, t: ProjectionTransformer): Model = {
    t.transform(TransformContext.builder().model(m).build())
  }
}
