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

package smithytranslate.closure

import scala.collection.mutable
import software.amazon.smithy.model.shapes.*
import software.amazon.smithy.model.node.{Node, ObjectNode}
import software.amazon.smithy.model.traits.IdRefTrait

import scala.jdk.OptionConverters.*
import scala.jdk.CollectionConverters.*
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.traits.Trait

private[closure] final class IdRefVisitor(
    model: Model,
    value: Node,
    captureTraits: Boolean,
    isInsideIdRefMember: Boolean = false,
    visitedShapes: mutable.Set[Shape]
) extends ShapeVisitor[Set[Shape]] {
  def blobShape(shape: BlobShape): Set[Shape] =
    Set.empty

  def booleanShape(shape: BooleanShape): Set[Shape] =
    Set.empty

  private def visitSeqShape(member: MemberShape): Set[Shape] =
    value.asArrayNode().toScala match {
      case None => Set.empty
      case Some(value) =>
        value
          .getElements()
          .asScala
          .toSet
          .flatMap { value: Node =>
            member.accept(
              new IdRefVisitor(
                model = model,
                value = value,
                captureTraits = captureTraits,
                isInsideIdRefMember = false,
                visitedShapes = visitedShapes
              )
            )
          }
    }

  def listShape(shape: ListShape): Set[Shape] =
    visitSeqShape(shape.getMember())

//  override def setShape(shape: SetShape): Set[Shape =
//    visitSeqShape(shape.getMember())

  def mapShape(shape: MapShape): Set[Shape] =
    visitSeqShape(shape.getValue())

  def byteShape(shape: ByteShape): Set[Shape] =
    Set.empty

  def shortShape(shape: ShortShape): Set[Shape] =
    Set.empty

  def integerShape(shape: IntegerShape): Set[Shape] =
    Set.empty

  def longShape(shape: LongShape): Set[Shape] =
    Set.empty

  def floatShape(shape: FloatShape): Set[Shape] =
    Set.empty

  def documentShape(shape: DocumentShape): Set[Shape] =
    Set.empty

  def doubleShape(shape: DoubleShape): Set[Shape] =
    Set.empty

  def bigIntegerShape(shape: BigIntegerShape): Set[Shape] =
    Set.empty

  def bigDecimalShape(shape: BigDecimalShape): Set[Shape] =
    Set.empty

  def operationShape(shape: OperationShape): Set[Shape] =
    Set.empty

  def resourceShape(shape: ResourceShape): Set[Shape] =
    Set.empty

  def serviceShape(shape: ServiceShape): Set[Shape] =
    Set.empty

  def stringShape(shape: StringShape): Set[Shape] = {
    if (isInsideIdRefMember || shape.hasTrait(classOf[IdRefTrait])) {
      value.asStringNode().toScala match {
        case None => Set.empty
        case Some(stringNode) =>
          val shapes = {
            model.getShape(ShapeId.from(stringNode.getValue())).toScala.toList
          }
          val shapesToVisit = shapes.filterNot(visitedShapes.contains)
          val stringNodeShapes = TransitiveModel
            .computeWithVisited(
              model = model,
              entryPoints = shapesToVisit.map(_.getId),
              captureTraits = captureTraits,
              visitedShapes = visitedShapes
            )
            .toSet
          stringNodeShapes ++ shapes
      }
    } else {
      Set.empty
    }
  }

  private def visitNamedMembersShape(
      members: Map[String, MemberShape]
  ): Set[Shape] =
    value.asObjectNode().toScala.toSet.flatMap { obj: ObjectNode =>
      val entries: Map[String, Node] = obj.getStringMap().asScala.toMap
      entries.flatMap { case (name, node) =>
        members.get(name) match {
          case None => Set.empty[Shape]
          case Some(member) =>
            member.accept(
              new IdRefVisitor(
                model = model,
                value = node,
                captureTraits = captureTraits,
                isInsideIdRefMember = false,
                visitedShapes = visitedShapes
              )
            )
        }
      }
    }

  def structureShape(shape: StructureShape): Set[Shape] =
    visitNamedMembersShape(shape.getAllMembers().asScala.toMap)

  def unionShape(shape: UnionShape): Set[Shape] =
    visitNamedMembersShape(shape.getAllMembers().asScala.toMap)

  def memberShape(shape: MemberShape): Set[Shape] = {
    val newVisitor = new IdRefVisitor(
      model,
      value,
      captureTraits,
      isInsideIdRefMember = shape.hasTrait(classOf[IdRefTrait]),
      // IdRefs have a selector of :test(string, member > string)
      // so we need to check for the trait in both of those places
      visitedShapes
    )
    model
      .getShape(shape.getTarget())
      .toScala
      .toSet
      .flatMap { shape: Shape =>
        shape.accept(newVisitor)
      }
  }

  def timestampShape(shape: TimestampShape): Set[Shape] =
    Set.empty

}

object IdRefVisitor {
  def visit(
      model: Model,
      captureTraits: Boolean,
      shapeId: ShapeId,
      trt: Trait,
      visitedShapes: mutable.Set[Shape]
  ): Set[Shape] = {
    model
      .getShape(shapeId)
      .toScala
      .toSet
      .flatMap { s0: Shape =>
        val result = s0.accept(
          new IdRefVisitor(
            model = model,
            value = trt.toNode,
            captureTraits = captureTraits,
            isInsideIdRefMember = false,
            visitedShapes = visitedShapes
          )
        )
        result
      }
  }
}
