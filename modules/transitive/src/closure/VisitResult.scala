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
import software.amazon.smithy.model.shapes.{Shape}

private[closure] case class VisitResult(
    visited: Set[Shape]
) {

  def +(shape: Shape): VisitResult = copy(visited = visited + shape)

  def ++(other: VisitResult): VisitResult =
    VisitResult(visited ++ other.visited)

//  def ++(other: List[Shape]): VisitResult =
//    VisitResult(output ++ other, visited)

//  def prepend(init: Shape): VisitResult =
//    VisitResult(init :: output, visited)

  def buildModel(
      initialModel: Model,
      validateModel: Boolean,
      captureMetadata: Boolean
  ): Model = {
    if (validateModel) {
      val assembler = Model.assembler()
      assembler
        .addShapes(
          visited.toList: _*
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
        .addShapes(visited.toList: _*)
        .metadata(
          if (captureMetadata) initialModel.getMetadata()
          else java.util.Collections.emptyMap()
        )
        .build()
    }
  }
}

object VisitResult {
  def empty(visited: Set[Shape]): VisitResult =
    VisitResult(visited)
}
