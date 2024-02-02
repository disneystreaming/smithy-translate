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

import java.util.stream.Collectors
import smithytranslate.closure.TransitiveModel
import smithytranslate.UUID
import software.amazon.smithy.build.{ProjectionTransformer, TransformContext}
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.loader.Prelude
import software.amazon.smithy.model.shapes._

import java.util
import scala.jdk.CollectionConverters._
import scala.collection.compat._

object ModelPreProcessor {

  object transformers {
    object Transitive {
      def apply(allowedNamespace: Option[String]) =
        new ProjectionTransformer() {
          def getName(): String = "transitive-filtering"
          def transform(x: TransformContext): Model = {
            val annotatedShapes = x
              .getModel()
              .getShapesWithTrait(classOf[alloy.proto.ProtoEnabledTrait])
              .asScala
              .map(_.getId())
              .filter(id => allowedNamespace.forall(_ == id.getNamespace()))
              .toList
            if (annotatedShapes.size < 1) {
              System.err.println(
                s"No shapes annotated with ${alloy.proto.ProtoEnabledTrait.ID} were found."
              )
            }
            TransitiveModel.compute(
              x.getModel(),
              annotatedShapes,
              captureTraits = true,
              captureMetadata = true,
              validateModel =
                false // model may be in invalid state since it is in a transient/intermediary state of the proto conversion
            )
          }
        }
    }

    /** This ProjectionTransformer is used to introduce shapes that are part of
      * the `smithytranslate` and used to replace Prelude shapes. These shapes
      * are:
      *   - BigInteger
      *   - BigDecimal
      *   - Timestamp
      * @param original
      * @return
      */
    val PreludeReplacements = new ProjectionTransformer() {
      // Prelude.getPreludeModel is not accessible
      private val preludeModel = Model.assembler().assemble().unwrap()
      private val addIfUsed = Map(
        // format: off
        (classOf[BigIntegerShape], (smithytranslate.BigInteger.shape, smithytranslate.BigInteger.target)),
        (classOf[BigDecimalShape], (smithytranslate.BigDecimal.shape, smithytranslate.BigDecimal.target)),
        (classOf[TimestampShape], (smithytranslate.Timestamp.shape, smithytranslate.Timestamp.target))
        // format: on
      )

      def getName(): String = "prelude-replacements"
      def transform(x: TransformContext): Model = {
        val m = x.getModel()
        val toAdd =
          addIfUsed.flatMap { case (clazz, (shape, preludeShapeId)) =>
            if (m.toSet(clazz).size() > 0) {
              List(shape, preludeModel.expectShape(preludeShapeId))
            } else List.empty
          }.toList

        m.toBuilder()
          .addShapes(toAdd.asJava)
          .build()
      }
    }

    /** Conflicts for enum happens at the value level on the protobuf side. Two
      * different enums in a same package can't have the same value. To catch
      * this, we build a map of EnumDefinition -> Boolean where the right side
      * is true if there is a conflict, false otherwise.
      *
      * To build the map, we select all EnumShapes. Then we build an
      * intermediate map of all the resolved protobuf enum name (see
      * #protoEnumName) by namespace. We use this to build the final lookup map
      * where we run (eagerly) a conflict check for each Value of the Enum.
      * found in the model.
      */

    val PreventEnumConflicts: ProjectionTransformer =
      new ProjectionTransformer() {

        def getName(): String = "prevent-enum-conflicts"
        def transform(x: TransformContext): Model = {
          val currentModel = x.getModel
          val enumsShapes: List[EnumShape] = currentModel
            .getEnumShapes()
            .asScala
            .filterNot(Prelude.isPreludeShape)
            .toList

          val intEnums: List[IntEnumShape] = currentModel
            .getIntEnumShapes()
            .asScala
            .filterNot(Prelude.isPreludeShape)
            .toList

          val allEnums: List[Shape] = enumsShapes ++ intEnums

          val allCombos = for {
            e <- allEnums
            memberName <- e.getMemberNames().asScala.toList
          } yield (e.getId().getNamespace(), memberName)

          val allRepeatedCombos =
            allCombos
              .groupBy(identity)
              .view
              .mapValues(_.size)
              .collect {
                case (k, v) if v > 1 => k
              }
              .toSet

          def hasConflict(member: MemberShape): Boolean = allRepeatedCombos(
            (member.getId().getNamespace(), member.getMemberName())
          )

          val newEnumShapes: List[Shape] = enumsShapes.map { enumShape =>
            val b = enumShape.toBuilder
            b.clearMembers()
            enumShape.members.asScala.foreach {
              case member if hasConflict(member) =>
                b.addMember(renameMember(member))
              case member =>
                b.addMember(member)
            }
            b.build()
          }

          val newIntEnumShapes = intEnums.map { intEnumShape =>
            val b = intEnumShape.toBuilder
            b.clearMembers()
            intEnumShape.members.asScala.foreach {
              case member if hasConflict(member) =>
                b.addMember(renameMember(member))
              case member =>
                b.addMember(member)
            }
            b.build()
          }

          val allShapes = newEnumShapes ++ newIntEnumShapes

          x.getTransformer()
            .replaceShapes(
              currentModel,
              allShapes.asJava
            )
        }

        def renameMember(member: MemberShape): MemberShape = {
          val name =
            s"${member.getId.getName().toUpperCase()}_${member.getMemberName}"
          member.toBuilder
            .id(member.getId.withMember(name))
            .build()
        }
      }

    /** Transforms UUID into a structure that produces the following protobuf
      * message:
      * ```proto
      * message UUID {
      *   int64 upper_bits = 1;
      *   int64 lower_bits = 2;
      * }
      * ```
      */
    val CompactUUID: ProjectionTransformer =
      new ProjectionTransformer() {

        def getName(): String = "compact-alloy-uuid"
        def transform(x: TransformContext): Model = {
          val uuidShapeId = ShapeId.fromParts("alloy", "UUID")
          val newUUIDShapeId = ShapeId.fromParts("smithytranslate", "UUID")

          /* Visitor to replace any reference to alloy#UUID in member shapes to
           * a custom alloy#CompactUUID shape.
           */
          val updateMemberShapes = new ShapeVisitor.Default[Shape]() {
            override protected def getDefault(shape: Shape): Shape =
              shape

            private def updateMember(shape: MemberShape): MemberShape = {
              if (shape.getTarget() == uuidShapeId) {
                shape.toBuilder().target(newUUIDShapeId).build()
              } else {
                shape
              }
            }

            override def structureShape(shape: StructureShape): Shape = {
              shape
                .toBuilder()
                .members(
                  shape
                    .getAllMembers()
                    .values()
                    .stream()
                    .map[MemberShape](updateMember)
                    .collect(Collectors.toList())
                )
                .build()

            }
            override def unionShape(shape: UnionShape): Shape = {
              shape
                .toBuilder()
                .members(
                  shape
                    .getAllMembers()
                    .values()
                    .stream()
                    .map[MemberShape](updateMember)
                    .collect(Collectors.toList())
                )
                .build()
            }
            override def listShape(shape: ListShape): Shape = {
              shape
                .toBuilder()
                .member(updateMember(shape.getMember()))
                .build()
            }
            override def mapShape(shape: MapShape): Shape = {
              shape
                .toBuilder()
                .key(updateMember(shape.getKey()))
                .value(updateMember(shape.getValue()))
                .build()
            }
          }
          val uuidUsage = x
            .getModel()
            .getMemberShapes()
            .stream()
            .filter { _.getTarget() == uuidShapeId }
            .count()
          if (uuidUsage > 0) {
            val updatedShapes: util.List[Shape] = x
              .getModel()
              .toSet()
              .stream()
              // remove reference to alloy#UUID
              .filter(_.getId() != uuidShapeId)
              .map[Shape] { _shape =>
                _shape.accept[Shape](updateMemberShapes)
              }
              .collect(Collectors.toList())
            Model
              .builder()
              .addShapes(updatedShapes)
              .addShape(UUID.shape)
              .build()
          } else {
            x.getModel()
          }
        }
      }

    def all(allowedNamespace: Option[String]): List[ProjectionTransformer] =
      Transitive(allowedNamespace) ::
        PreludeReplacements ::
        PreventEnumConflicts ::
        CompactUUID ::
        Nil
  }

  def apply(
      model: Model,
      transformers: List[ProjectionTransformer]
  ): Model = {
    transformers.foldLeft(model) { (acc, transformer) =>
      transformer.transform(TransformContext.builder().model(acc).build())
    }
  }
}
