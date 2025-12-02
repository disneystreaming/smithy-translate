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

package smithytranslate.runners

import software.amazon.smithy.model.Model
import software.amazon.smithy.build.transforms.FilterSuppressions
import software.amazon.smithy.model.node.{Node, ObjectNode}
import software.amazon.smithy.model.shapes.SmithyIdlModelSerializer
import software.amazon.smithy.build.TransformContext

import scala.jdk.CollectionConverters._

object SmithyModelUtils {

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

  def getSmithyJsonFiles(model: Model): Map[os.SubPath, String] = {
    val serializer =
      software.amazon.smithy.model.shapes.ModelSerializer.builder().build()

    val jsonString = Node.prettyPrintJson(serializer.serialize(filter(model)))

    Map(os.SubPath("result.json") -> jsonString)
  }

  def getSmithyFiles(model: Model): Map[os.SubPath, String] =
    SmithyIdlModelSerializer
      .builder()
      .build()
      .serialize(filter(model))
      .asScala
      .toMap
      .filterNot { case (path, _) =>
        path.toString.endsWith("smithytranslate.smithy") ||
        path.toString.endsWith("proto.smithy") ||
        path.toString.endsWith("alloy.smithy")
      }
      .map { in =>
        val subPath =
          in._1.toString
            .split('.')
            .dropRight(1)
            .mkString(
              "",
              "/",
              ".smithy"
            ) // convert e.g. my.namespace.test.smithy to my/namespace/test.smithy
        (os.SubPath(subPath), in._2)
      }
}
