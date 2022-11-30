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

package closure

import software.amazon.smithy.model.shapes._
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.traits.IdRefTrait
import scala.jdk.OptionConverters._
import scala.jdk.CollectionConverters._
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.traits.Trait
import smithytranslate.closure.TransitiveModel

final class IdRefVisitor(
    model: Model,
    value: Node,
    captureTraits: Boolean,
    validateModel: Boolean,
    isInsideIdRefMember: Boolean = false
) extends ShapeVisitor[List[Shape]] {
  def blobShape(shape: BlobShape): List[Shape] = List.empty

  def booleanShape(shape: BooleanShape): List[Shape] = List.empty

  private def visitSeqShape(member: MemberShape): List[Shape] =
    value.asArrayNode().toScala match {
      case None => List.empty
      case Some(value) =>
        value
          .getElements()
          .asScala
          .toList
          .flatMap(value =>
            member.accept(
              new IdRefVisitor(model, value, captureTraits, validateModel)
            )
          )
    }

  def listShape(shape: ListShape): List[Shape] =
    visitSeqShape(shape.getMember())

  @annotation.nowarn("msg=class SetShape in package shapes is deprecated")
  override def setShape(shape: SetShape): List[Shape] =
    visitSeqShape(shape.getMember())

  def mapShape(shape: MapShape): List[Shape] =
    visitSeqShape(shape.getValue())

  def byteShape(shape: ByteShape): List[Shape] = List.empty

  def shortShape(shape: ShortShape): List[Shape] = List.empty

  def integerShape(shape: IntegerShape): List[Shape] = List.empty

  def longShape(shape: LongShape): List[Shape] = List.empty

  def floatShape(shape: FloatShape): List[Shape] = List.empty

  def documentShape(shape: DocumentShape): List[Shape] = List.empty

  def doubleShape(shape: DoubleShape): List[Shape] = List.empty

  def bigIntegerShape(shape: BigIntegerShape): List[Shape] = List.empty

  def bigDecimalShape(shape: BigDecimalShape): List[Shape] = List.empty

  def operationShape(shape: OperationShape): List[Shape] = List.empty

  def resourceShape(shape: ResourceShape): List[Shape] = List.empty

  def serviceShape(shape: ServiceShape): List[Shape] = List.empty

  def stringShape(shape: StringShape): List[Shape] = {
    if (isInsideIdRefMember || shape.hasTrait(classOf[IdRefTrait])) {
      value.asStringNode().toScala match {
        case None => List.empty
        case Some(stringNode) =>
          val shapes =
            model.getShape(ShapeId.from(stringNode.getValue())).toScala.toList
          TransitiveModel
            .compute(
              model,
              shapes.map(_.getId),
              captureTraits,
              captureMetadata = false,
              validateModel
            )
            .toSet()
            .asScala
            .toList ++ shapes
      }
    } else {
      List.empty
    }
  }

  private def visitNamedMembersShape(
      members: Map[String, MemberShape]
  ): List[Shape] =
    value.asObjectNode().toScala.toList.flatMap { obj =>
      val entries: Map[String, Node] = obj.getStringMap().asScala.toMap
      entries.flatMap { case (name, node) =>
        members.get(name) match {
          case None => List.empty
          case Some(member) =>
            member.accept(
              new IdRefVisitor(model, node, captureTraits, validateModel)
            )
        }
      }
    }

  def structureShape(shape: StructureShape): List[Shape] =
    visitNamedMembersShape(shape.getAllMembers().asScala.toMap)

  def unionShape(shape: UnionShape): List[Shape] =
    visitNamedMembersShape(shape.getAllMembers().asScala.toMap)

  def memberShape(shape: MemberShape): List[Shape] = {
    val newVisitor = new IdRefVisitor(
      model,
      value,
      captureTraits,
      validateModel,
      isInsideIdRefMember = shape.hasTrait(classOf[IdRefTrait])
      // IdRefs have a selector of :test(string, member > string)
      // so we need to check for the trait in both of those places
    )
    model
      .getShape(shape.getTarget())
      .toScala
      .toList
      .flatMap(_.accept(newVisitor))
  }

  def timestampShape(shape: TimestampShape): List[Shape] = List.empty

}

object IdRefVisitor {
  def visit(
      model: Model,
      captureTraits: Boolean,
      shapeId: ShapeId,
      trt: Trait,
      validateModel: Boolean,
      includeStartingTrait: Boolean = false
  ): List[Shape] = {
    model
      .getShape(shapeId)
      .toScala
      .toList
      .flatMap { s0 =>
        val s = if (includeStartingTrait) List(s0) else Nil
        s ++ s0.accept(
          new IdRefVisitor(
            model,
            trt.toNode,
            captureTraits,
            validateModel
          )
        )
      }
  }
}
