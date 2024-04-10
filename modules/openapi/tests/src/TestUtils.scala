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

package smithytranslate.compiler.openapi

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
import software.amazon.smithy.model.transform.ModelTransformer
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.traits.Trait
import software.amazon.smithy.model.traits.BoxTrait
import scala.compat.java8.FunctionConverters._
import smithytranslate.compiler.SmithyVersion
import smithytranslate.compiler.ToSmithyResult
import smithytranslate.compiler.ToSmithyCompilerOptions
import smithytranslate.compiler.FileContents

object TestUtils {

  sealed trait ExpectedOutput extends Product with Serializable
  object ExpectedOutput {
    final case class StringOutput(str: String) extends ExpectedOutput
    final case class ModelOutput(model: Model) extends ExpectedOutput
  }

  final case class ConversionTestInput(
      filePath: NonEmptyList[String],
      openapiSpec: String,
      smithySpec: ExpectedOutput,
      errorSmithySpec: Option[String],
      smithyVersion: SmithyVersion
  )

  object ConversionTestInput {
    def apply(
        filePath: NonEmptyList[String],
        openapiSpec: String,
        smithySpec: String,
        errorSmithySpec: Option[String] = None,
        smithyVersion: SmithyVersion = SmithyVersion.Two
    ): ConversionTestInput =
      new ConversionTestInput(
        filePath,
        openapiSpec,
        ExpectedOutput.StringOutput(smithySpec),
        errorSmithySpec,
        smithyVersion
      )

    def apply(
        filePath: NonEmptyList[String],
        openapiSpec: String,
        smithyModel: Model,
        errorSmithySpec: Option[String],
        smithyVersion: SmithyVersion
    ): ConversionTestInput =
      new ConversionTestInput(
        filePath,
        openapiSpec,
        ExpectedOutput.ModelOutput(smithyModel),
        errorSmithySpec,
        smithyVersion
      )
  }

  final case class ConversionResult(
      result: ToSmithyResult[ModelWrapper],
      expected: ModelWrapper
  )

  def runConversion(
      input0: ConversionTestInput,
      remaining: ConversionTestInput*
  ): ConversionResult = {
    val inputs = (input0 +: remaining).toList

    val result =
      OpenApiCompiler.compile(
        ToSmithyCompilerOptions(
          useVerboseNames = false,
          validateInput = false,
          validateOutput = false,
          List.empty,
          input0.smithyVersion == SmithyVersion.One,
          debug = true
        ),
        OpenApiCompilerInput.UnparsedSpecs(
          inputs.map(i => FileContents(i.filePath, i.openapiSpec))
        )
      )

    val resultW = result.map(ModelWrapper(_))
    val assembler = Model
      .assembler()
      .discoverModels()

    inputs.foreach { i =>
      i.smithySpec match {
        case TestUtils.ExpectedOutput.StringOutput(str) =>
          val name = i.filePath.mkString_("/") + ".smithy"
          val spec = s"""|$$version: "${i.smithyVersion}"
                         |
                         |${str}""".stripMargin
          assembler.addUnparsedModel(name, spec)
        case TestUtils.ExpectedOutput.ModelOutput(model) =>
          assembler.addModel(model)
      }
      i.errorSmithySpec.foreach(
        assembler.addUnparsedModel("error.smithy", _)
      )
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
      case ToSmithyResult.Failure(err, errors) =>
        errors.foreach(println)
        Assertions.fail("Validating model failed: ", err)
      case ToSmithyResult.Success(_, output) =>
        Assertions.assertEquals(output, expected)
    }
  }

  def runConversionTest(
      openapiSpec: String,
      smithySpec: String,
      smithyVersion: SmithyVersion = SmithyVersion.Two
  )(implicit
      loc: Location
  ): Unit = {
    TestUtils.runConversionTest(
      ConversionTestInput(
        NonEmptyList.one("foo.yaml"),
        openapiSpec,
        smithySpec,
        smithyVersion = smithyVersion
      )
    )
  }

  def runConversionTestWithModel(
      openapiSpec: String,
      smithyModel: Model,
      smithyVersion: SmithyVersion = SmithyVersion.Two
  )(implicit
      loc: Location
  ): Unit = {
    TestUtils.runConversionTest(
      ConversionTestInput(
        NonEmptyList.one("foo.yaml"),
        openapiSpec,
        smithyModel,
        errorSmithySpec = None,
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

    override def toString() = {
      SmithyIdlModelSerializer
        .builder()
        .build()
        .serialize(filter(model))
        .asScala
        .map(in => s"${in._1.toString.toUpperCase}:\n\n${in._2}")
        .mkString("\n")
    }
  }

  object ModelWrapper {
    def apply(model: Model): ModelWrapper = {
      // Remove all box traits because they are applied inconsistently depending on if you
      // load from Java model or from unparsed string model
      @annotation.nowarn("msg=class BoxTrait in package traits is deprecated")
      val noBoxModel = ModelTransformer
        .create()
        .filterTraits(
          model,
          ((_: Shape, trt: Trait) => trt.toShapeId() != BoxTrait.ID).asJava
        )
      new ModelWrapper(noBoxModel)
    }
  }
}
