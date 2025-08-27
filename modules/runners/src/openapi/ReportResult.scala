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

package smithytranslate.runners.openapi

import smithytranslate.compiler.openapi.OpenApiCompiler
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.SmithyIdlModelSerializer
import scala.jdk.CollectionConverters._
import software.amazon.smithy.build.transforms.FilterSuppressions
import software.amazon.smithy.build.TransformContext
import software.amazon.smithy.model.node.{Node, ObjectNode}
import smithytranslate.compiler.ToSmithyResult

final case class ReportResult(outputPath: os.Path, outputJson: Boolean) {

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

  private def getSmithyJsonFiles(model: Model): Map[os.Path, os.Source] = {
    val serializer =
      software.amazon.smithy.model.shapes.ModelSerializer.builder().build()

    val jsonString = Node.prettyPrintJson(serializer.serialize(filter(model)))
    val path = outputPath / "result.json"

    Map(path -> jsonString)
  }

  private def getSmithyFiles(model: Model): Map[os.Path, os.Source] =
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
        val path = outputPath / os.SubPath(subPath)
        (path, os.Source.WritableSource(in._2))
      }

  def apply(result: ToSmithyResult[Model], debug: Boolean): Unit = {
    result match {
      case ToSmithyResult.Failure(error, modelErrors) =>
        val message = if (modelErrors.isEmpty) {
          "An error occurred while importing your Open API resources."
        } else {
          val errorsSummary = modelErrors.map(_.getMessage).mkString("\n")
          s"""|Failed to validate the produced Smithy model. The following is a list of
              |error messages followed by the validation exception from Smithy:
              |$errorsSummary""".stripMargin
        }
        System.err.println(message)
        if (debug) {
          error.printStackTrace(System.err)
        } else {
          System.err.println(error.getMessage())
        }
      case ToSmithyResult.Success(modelErrors, model) =>
        modelErrors.foreach(e => System.err.println(e.getMessage()))
        val smithyFiles =
          if (outputJson) getSmithyJsonFiles(model) else getSmithyFiles(model)
        smithyFiles.foreach { in =>
          System.err.println(
            s"Writing ${in._1}"
          )
          os.write.over(in._1, in._2, createFolders = true)
        }
    }
  }

}
