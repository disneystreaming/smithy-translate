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
import software.amazon.smithy.model.shapes.{Shape, ShapeId}

import scala.jdk.CollectionConverters._
import scala.collection.SortedSet
import scala.collection.immutable.TreeSet

object ModelOps {
  implicit class ModelOps(model: Model) {
    def check(): Model = Model.assembler().addModel(model).assemble().unwrap()

    def prettyPrint: String = ModelWrapper(model).toString()

    def toShapeSet: SortedSet[Shape] = {
      // Ordering the shapes in an effort to retain the original definition ordering
      type Location = (String, Int, String)
      def loc(x: Shape): Location =
        (
          x.getSourceLocation().getFilename(),
          x.getSourceLocation().getLine(),
          x.getId().toString()
        )

      implicit val shapeOrdering: Ordering[Shape] =
        Ordering[Location].on[Shape](loc)
      model.shapes().iterator().asScala.foldLeft(new TreeSet[Shape])(_ + _)
    }

    def debug: Model = {
      println(model.prettyPrint)
      model
    }

    def transitiveClosure(
        shapeIds: List[ShapeId],
        captureTraits: Boolean = false,
        captureMetadata: Boolean = false,
        validateModel: Boolean = true
    ): Model = {
      TransitiveModel.compute(
        model = model,
        entryPoints = shapeIds,
        captureTraits = captureTraits,
        captureMetadata = captureMetadata,
        validateModel = validateModel
      )
    }
  }
}
