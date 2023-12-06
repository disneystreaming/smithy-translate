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
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes._
import software.amazon.smithy.model.neighbor.Walker
import software.amazon.smithy.model.neighbor.NeighborProvider
import scala.jdk.OptionConverters._
import scala.jdk.CollectionConverters._
import software.amazon.smithy.model.traits.TraitDefinition
import software.amazon.smithy.utils.ToSmithyBuilder

object TransitiveModel {

  def compute(
      model: Model,
      entryPoints: List[ShapeId],
      captureTraits: Boolean,
      captureMetadata: Boolean,
      validateModel: Boolean
  ): Model = {
    val shapes = computeWithVisited(
      model = model,
      entryPoints = entryPoints,
      captureTraits = captureTraits,
      visitedShapes = mutable.Set.empty
    )
    createModelFromShapes(
      initialModel = model,
      shapes = shapes,
      validateModel = validateModel,
      captureMetadata = captureMetadata
    )
  }

  private[closure] def computeWithVisited(
      model: Model,
      entryPoints: List[ShapeId],
      captureTraits: Boolean,
      visitedShapes: mutable.Set[Shape]
  ): List[Shape] = {
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
    val closure =
      entryPointsShapes
        .map(walker.walkShapes)
        .map(_.asScala.toSet)
        .fold[Set[Shape]](Set.empty)(_ ++ _)

    val allShapes =
      (closure ++ entrypointTraits).filter(s => !visitedShapes.contains(s))
    visitedShapes ++= allShapes

    val idRefShapesVisitResult = allShapes
      .flatMap(_.getAllTraits().asScala)
      .flatMap { case (_, trt) =>
        IdRefVisitor.visit(
          model = model,
          captureTraits = captureTraits,
          trt = trt,
          visitedShapes = visitedShapes
        )
      }

    val allShapesFinal =
      (if (captureTraits) allShapes
       else allShapes.map(clearTraitsFromShape)) ++ idRefShapesVisitResult

    allShapesFinal.toList
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

  private def createModelFromShapes(
      initialModel: Model,
      shapes: List[Shape],
      validateModel: Boolean,
      captureMetadata: Boolean
  ): Model = {
    if (validateModel) {
      val assembler = Model.assembler()
      assembler
        .addShapes(
          shapes: _*
        )
      if (captureMetadata) {
        initialModel.getMetadata().forEach { case (k, v) =>
          val _ = assembler.putMetadata(k, v)
        }
      }

      assembler
        .assemble()
        .unwrap()
    } else {
      Model
        .builder()
        .addShapes(shapes: _*)
        .metadata(
          if (captureMetadata) initialModel.getMetadata()
          else java.util.Collections.emptyMap()
        )
        .build()
    }
  }
}
