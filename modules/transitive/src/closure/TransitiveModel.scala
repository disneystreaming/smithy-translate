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

import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes._
import scala.jdk.CollectionConverters.SetHasAsScala
import software.amazon.smithy.model.neighbor.Walker
import software.amazon.smithy.model.neighbor.NeighborProvider
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._
import software.amazon.smithy.model.traits.TraitDefinition
import software.amazon.smithy.utils.ToSmithyBuilder

object TransitiveModel {

  def compute(
      model: Model,
      entryPoints: List[ShapeId],
      captureTraits: Boolean,
      captureMetadata: Boolean,
      validateModel: Boolean
  ): Model =
    computeWithVisited(
      model = model,
      entryPoints = entryPoints,
      captureTraits = captureTraits,
      captureMetadata = captureMetadata,
      validateModel = validateModel,
      visitedShapes0 = Set.empty
    )
  private[closure] def computeWithVisited(
      model: Model,
      entryPoints: List[ShapeId],
      captureTraits: Boolean,
      captureMetadata: Boolean,
      validateModel: Boolean,
      visitedShapes0: Set[Shape]
  ): Model = {
    val walker = new Walker(
      if (captureTraits)
        NeighborProvider
          .withTraitRelationships(model, NeighborProvider.of(model))
      else NeighborProvider.of(model)
    )

    val entrypointTraits: List[Shape] =
      entryPoints.flatMap(
        model.getShape(_).toScala.filter(_.hasTrait(classOf[TraitDefinition]))
      )

    val entryPointsShapes = entryPoints.map(model.expectShape(_))
    val visitedShapes = visitedShapes0
    val closure =
      entryPointsShapes
        .map(walker.walkShapes)
        .map(_.asScala.toSet)
        .fold[Set[Shape]](Set.empty)(_ ++ _)

    val allShapes =
      (closure ++ entrypointTraits).filter(s => !visitedShapes.contains(s))

    val idRefShapesVisitResult =
      allShapes
        .flatMap(_.getAllTraits().asScala)
        .foldLeft(VisitResult.empty(visitedShapes)) {
          case (previousVisitResult, (shapeId, trt)) =>
            previousVisitResult ++ IdRefVisitor.visit(
              model = model,
              captureTraits = captureTraits,
              captureMetadata = captureMetadata,
              shapeId = shapeId,
              trt = trt,
              validateModel = validateModel,
              includeStartingTrait = false,
              visitedShapes = previousVisitResult.visited
            )
        }

    val allShapesFinal =
      (if (captureTraits) allShapes
       else
         allShapes.map(clearTraitsFromShape)) ++ idRefShapesVisitResult.visited

    VisitResult(
      entryPointsShapes.toSet ++ allShapesFinal /*inefficient?*/ ++ idRefShapesVisitResult.visited
    ).buildModel(
      initialModel = model,
      validateModel = validateModel,
      captureMetadata = captureMetadata
    )
  }
  private def clearTraitsFromShape(shape: Shape): Shape = {
    shape match {
      case s: ToSmithyBuilder[_] =>
        s.toBuilder match {
          case s: AbstractShapeBuilder[_, _] =>
            s.clearTraits()
            s.build().asInstanceOf[Shape]
          case _ => shape
        }
      case _ => shape
    }

  }
}
