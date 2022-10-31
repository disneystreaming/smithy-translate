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

import software.amazon.smithy.build.TransformContext
import software.amazon.smithy.build.transforms.FilterSuppressions
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.node.ObjectNode
import software.amazon.smithy.model.shapes.SmithyIdlModelSerializer

import scala.jdk.CollectionConverters._

// In order to have nice comparisons from munit reports.
class ModelWrapper(val model: Model) {
  override def equals(obj: Any): Boolean = obj match {
    case wrapper: ModelWrapper =>
      model == wrapper.model
    case _ => false
  }

  private def filter(model: Model): Model = {
    val filterSuppressions: Model => Model = m =>
      new FilterSuppressions().transform(
        TransformContext
          .builder()
          .model(m)
          .settings(
            ObjectNode.builder().withMember("removeUnused", true).build()
          )
          .build()
      )
    (filterSuppressions)(model)
  }

  override def toString() =
    SmithyIdlModelSerializer
      .builder()
      .build()
      .serialize(filter(model))
      .asScala
      .map(in => s"${in._1.toString.toUpperCase}:\n\n${in._2}")
      .mkString("\n")
}

object ModelWrapper {
  def apply(model: Model): ModelWrapper =
    new ModelWrapper(model)
}
