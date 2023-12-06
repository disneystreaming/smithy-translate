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

package smithytranslate.json_schema

import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.SmithyIdlModelSerializer
import scala.jdk.CollectionConverters._
import munit.Assertions
import software.amazon.smithy.build.transforms.FilterSuppressions
import software.amazon.smithy.build.TransformContext
import cats.syntax.all._
import munit.Location
import cats.data.NonEmptyList
import software.amazon.smithy.model.node._
import smithytranslate.openapi.OpenApiCompiler
import smithytranslate.openapi.OpenApiCompiler.SmithyVersion

object TestUtils {

  final case class ConversionTestInput(
      filePath: NonEmptyList[String],
      jsonSpec: String,
      smithySpec: String,
      errorSmithySpec: Option[String] = None,
      smithyVersion: SmithyVersion = SmithyVersion.Two
  )

  final case class ConversionResult(
      result: OpenApiCompiler.Result[ModelWrapper],
      expected: ModelWrapper
  )

  def runConversion(
      input0: ConversionTestInput,
      remaining: ConversionTestInput*
  ): ConversionResult = {
    val inputs = input0 +: remaining
    val result =
      JsonSchemaCompiler.parseAndCompile(
        OpenApiCompiler.Options(
          useVerboseNames = false,
          failOnValidationErrors = false,
          List.empty,
          input0.smithyVersion == SmithyVersion.One,
          debug = true
        ),
        inputs.map(i => i.filePath -> i.jsonSpec): _*
      )
    val resultW = result.map(ModelWrapper(_))

    val assembler = Model
      .assembler()
      .discoverModels()
    inputs.foreach { i =>
      val name = i.filePath.mkString_("/") + ".smithy"
      val spec = s"""|$$version: "${i.smithyVersion}"
                     |
                     |${i.smithySpec}""".stripMargin
      assembler.addUnparsedModel(name, spec)
      i.errorSmithySpec.foreach(assembler.addUnparsedModel("error.smithy", _))
    }
    val expected = ModelWrapper(
      assembler
        .assemble()
        .unwrap()
    )
    ConversionResult(resultW, expected)
  }

  def runConversionTest(
      input0: ConversionTestInput,
      remaining: ConversionTestInput*
  )(implicit
      loc: Location
  ): Unit = {
    val ConversionResult(res, expected) = runConversion(
      input0,
      remaining: _*
    )
    res match {
      case OpenApiCompiler.Failure(err, errors) =>
        errors.foreach(println)
        Assertions.fail("Validating model failed: ", err)
      case OpenApiCompiler.Success(_, output) =>
        Assertions.assertEquals(output, expected)
    }
  }

  def runConversionTest(
      jsonSpec: String,
      smithySpec: String,
      smithyVersion: SmithyVersion = SmithyVersion.Two
  )(implicit
      loc: Location
  ): Unit = {
    TestUtils.runConversionTest(
      ConversionTestInput(
        NonEmptyList.one("foo.yaml"),
        jsonSpec,
        smithySpec,
        smithyVersion = smithyVersion
      )
    )
  }

  // In order to have nice comparisons from munit reports.
  class ModelWrapper(val model: Model) {
    override def equals(obj: Any): Boolean = obj match {
      case wrapper: ModelWrapper =>
        reorderMetadata(model) == reorderMetadata(wrapper.model)
      case _ => false
    }

    private def reorderMetadata(model: Model): Model = {
      implicit val nodeOrd: Ordering[Node] = (x: Node, y: Node) =>
        x.hashCode() - y.hashCode()

      implicit val nodeStringOrd: Ordering[StringNode] = {
        val ord = Ordering[String]
        (x: StringNode, y: StringNode) =>
          ord.compare(x.getValue(), y.getValue())
      }
      def goNode(n: Node): Node = n match {
        case array: ArrayNode =>
          val elements = array.getElements().asScala.toList.sorted
          Node.arrayNode(elements: _*)
        case obj: ObjectNode =>
          Node.objectNode(
            obj.getMembers().asScala.toSeq.sortBy(_._1).toMap.asJava
          )
        case other => other
      }
      def go(metadata: Map[String, Node]): Map[String, Node] = {
        val keys = metadata.keySet.toVector.sorted
        keys.map { k =>
          k -> goNode(metadata(k))
        }.toMap
      }

      val builder = model.toBuilder()
      val newMeta = go(model.getMetadata().asScala.toMap)
      builder.clearMetadata()
      builder.metadata(newMeta.asJava)
      builder.build()
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
}
