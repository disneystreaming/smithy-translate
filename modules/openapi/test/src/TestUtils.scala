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
import munit.Assertions
import cats.syntax.all._
import munit.Location
import cats.data.NonEmptyList
import smithytranslate.compiler.SmithyVersion
import smithytranslate.compiler.ToSmithyResult
import smithytranslate.compiler.ToSmithyCompilerOptions
import smithytranslate.compiler.FileContents
import smithytranslate.compiler.ModelWrapper

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
          transformers = List.empty,
          useEnumTraitSyntax = input0.smithyVersion == SmithyVersion.One,
          debug = true,
          allowedRemoteBaseURLs = Set.empty,
          namespaceRemaps = Map.empty
        ),
        OpenApiCompilerInput.UnparsedSpecs(
          inputs.map(i => FileContents(i.filePath, i.openapiSpec))
        )
      )

    // Test that model can be assembled successfully,
    // if it can't then there's no sense in making
    // assertions on it that might not even be correct
    val _ = result.map(r => Model.assembler.addModel(r).assemble.unwrap)

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
}
