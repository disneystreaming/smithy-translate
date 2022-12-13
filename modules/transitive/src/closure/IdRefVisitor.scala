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

import software.amazon.smithy.model.shapes._
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.traits.IdRefTrait
import scala.jdk.OptionConverters._
import scala.jdk.CollectionConverters._
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.traits.Trait

private[closure] final class IdRefVisitor(
    model: Model,
    value: Node,
    captureTraits: Boolean,
    captureMetadata: Boolean,
    validateModel: Boolean,
    isInsideIdRefMember: Boolean = false,
    visitedShapes: Set[Shape]
) extends ShapeVisitor[VisitResult] {
  def blobShape(shape: BlobShape): VisitResult =
    VisitResult.empty(visitedShapes)

  def booleanShape(shape: BooleanShape): VisitResult =
    VisitResult.empty(visitedShapes)

  private def visitSeqShape(member: MemberShape): VisitResult =
    value.asArrayNode().toScala match {
      case None => VisitResult.empty(visitedShapes)
      case Some(value) =>
        value
          .getElements()
          .asScala
          .toList
          .foldLeft(VisitResult.empty(visitedShapes)) {
            (previousVisitedResult, value) =>
              previousVisitedResult ++ member.accept(
                new IdRefVisitor(
                  model = model,
                  value = value,
                  captureTraits = captureTraits,
                  captureMetadata = captureMetadata,
                  validateModel = validateModel,
                  isInsideIdRefMember = false,
                  visitedShapes = previousVisitedResult.visited
                )
              )
          }
    }

  def listShape(shape: ListShape): VisitResult =
    visitSeqShape(shape.getMember())

//  override def setShape(shape: SetShape): VisitResult =
//    visitSeqShape(shape.getMember())

  def mapShape(shape: MapShape): VisitResult =
    visitSeqShape(shape.getValue())

  def byteShape(shape: ByteShape): VisitResult =
    VisitResult.empty(visitedShapes)

  def shortShape(shape: ShortShape): VisitResult =
    VisitResult.empty(visitedShapes)

  def integerShape(shape: IntegerShape): VisitResult =
    VisitResult.empty(visitedShapes)

  def longShape(shape: LongShape): VisitResult =
    VisitResult.empty(visitedShapes)

  def floatShape(shape: FloatShape): VisitResult =
    VisitResult.empty(visitedShapes)

  def documentShape(shape: DocumentShape): VisitResult =
    VisitResult.empty(visitedShapes)

  def doubleShape(shape: DoubleShape): VisitResult =
    VisitResult.empty(visitedShapes)

  def bigIntegerShape(shape: BigIntegerShape): VisitResult =
    VisitResult.empty(visitedShapes)

  def bigDecimalShape(shape: BigDecimalShape): VisitResult =
    VisitResult.empty(visitedShapes)

  def operationShape(shape: OperationShape): VisitResult =
    VisitResult.empty(visitedShapes)

  def resourceShape(shape: ResourceShape): VisitResult =
    VisitResult.empty(visitedShapes)

  def serviceShape(shape: ServiceShape): VisitResult =
    VisitResult.empty(visitedShapes)

  def stringShape(shape: StringShape): VisitResult = {
    if (isInsideIdRefMember || shape.hasTrait(classOf[IdRefTrait])) {
      value.asStringNode().toScala match {
        case None => VisitResult.empty(visitedShapes)
        case Some(stringNode) =>
          val shapes = {
            model.getShape(ShapeId.from(stringNode.getValue())).toScala.toList
          }
          val stringNodeShapes = TransitiveModel
            .computeWithVisited(
              model = model,
              entryPoints = shapes.map(_.getId),
              captureTraits = captureTraits,
              captureMetadata = captureMetadata,
              validateModel = validateModel,
              visitedShapes0 = visitedShapes
            )
            .toSet()
            .asScala
            .toSet
          VisitResult(stringNodeShapes ++ shapes)
      }
    } else {
      VisitResult.empty(visitedShapes)
    }
  }

  private def visitNamedMembersShape(
      members: Map[String, MemberShape]
  ): VisitResult =
    value.asObjectNode().toScala.fold(VisitResult.empty(visitedShapes)) { obj =>
      val entries: Map[String, Node] = obj.getStringMap().asScala.toMap
      entries.foldLeft(VisitResult.empty(visitedShapes)) {
        case (previousVisitedResult, (name, node)) =>
          members.get(name) match {
            case None => previousVisitedResult
            case Some(member) =>
              previousVisitedResult ++ member.accept(
                new IdRefVisitor(
                  model = model,
                  value = node,
                  captureTraits = captureTraits,
                  captureMetadata = captureMetadata,
                  validateModel = validateModel,
                  isInsideIdRefMember = false,
                  visitedShapes = previousVisitedResult.visited
                )
              )
          }
      }
    }

  def structureShape(shape: StructureShape): VisitResult =
    visitNamedMembersShape(shape.getAllMembers().asScala.toMap)

  def unionShape(shape: UnionShape): VisitResult =
    visitNamedMembersShape(shape.getAllMembers().asScala.toMap)

  def memberShape(shape: MemberShape): VisitResult = {
    val newVisitor = new IdRefVisitor(
      model,
      value,
      captureTraits,
      captureMetadata,
      validateModel,
      isInsideIdRefMember = shape.hasTrait(classOf[IdRefTrait]),
      // IdRefs have a selector of :test(string, member > string)
      // so we need to check for the trait in both of those places
      visitedShapes
    )
    model
      .getShape(shape.getTarget())
      .toScala
      .fold(VisitResult.empty(visitedShapes)) {
        _.accept(newVisitor)
      }
  }

  def timestampShape(shape: TimestampShape): VisitResult =
    VisitResult.empty(visitedShapes)

}

object IdRefVisitor {
  def visit(
      model: Model,
      captureTraits: Boolean,
      captureMetadata: Boolean,
      shapeId: ShapeId,
      trt: Trait,
      validateModel: Boolean,
      includeStartingTrait: Boolean = false,
      visitedShapes: Set[Shape]
  ): VisitResult = {
    model
      .getShape(shapeId)
      .toScala
      .fold(VisitResult.empty(visitedShapes)) { s0 =>
        val result = s0.accept(
          new IdRefVisitor(
            model = model,
            value = trt.toNode,
            captureTraits = captureTraits,
            captureMetadata = captureMetadata,
            validateModel = validateModel,
            isInsideIdRefMember = false,
            visitedShapes = visitedShapes
          )
        )
        if (includeStartingTrait) result + s0 else result
      }
  }
}
