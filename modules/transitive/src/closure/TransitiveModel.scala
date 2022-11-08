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
import closure.IdRefVisitor

object TransitiveModel {
  def compute(
      model: Model,
      entryPoints: List[ShapeId],
      captureTraits: Boolean,
      captureMetadata: Boolean,
      validateModel: Boolean
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
    val closure =
      entryPointsShapes
        .map(walker.walkShapes)
        .map(_.asScala.toSet)
        .fold[Set[Shape]](Set.empty)(_ ++ _)

    val allShapes = (closure ++ entrypointTraits)

    val idRefShapes = allShapes.flatMap(_.getAllTraits().asScala).flatMap {
      case (shapeId, trt) =>
        IdRefVisitor.visit(model, captureTraits, shapeId, trt, validateModel)
    }

    val allShapesFinal =
      (if (captureTraits) allShapes
       else allShapes.map(clearTraitsFromShape)) ++ idRefShapes

    if (validateModel) {
      val assembler = Model.assembler()
      assembler
        .addShapes(
          allShapesFinal.toList: _*
        )
      if (captureMetadata) {
        model.getMetadata().forEach { case (k, v) =>
          val _ = assembler.putMetadata(k, v)
        }
      }

      assembler
        .assemble()
        .unwrap()
    } else {
      Model
        .builder()
        .addShapes(allShapesFinal.toList: _*)
        .metadata(
          if (captureMetadata) model.getMetadata()
          else java.util.Collections.emptyMap()
        )
        .build()
    }
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
