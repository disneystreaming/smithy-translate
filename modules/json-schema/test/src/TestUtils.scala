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

package smithytranslate.compiler.json_schema

import cats.data.NonEmptyList
import cats.syntax.all._
import munit.Assertions
import munit.Location
import smithytranslate.compiler.ModelWrapper
import smithytranslate.compiler.FileContents
import smithytranslate.compiler.SmithyVersion
import smithytranslate.compiler.ToSmithyCompilerOptions
import smithytranslate.compiler.ToSmithyResult
import software.amazon.smithy.model.Model

object TestUtils {

  final case class ConversionTestInput(
      filePath: NonEmptyList[String],
      jsonSpec: Option[String],
      smithySpec: String,
      errorSmithySpec: Option[String],
      smithyVersion: SmithyVersion
  )

  object ConversionTestInput {
    def apply(
        filePath: NonEmptyList[String],
        jsonSpec: String,
        smithySpec: String,
        errorSmithySpec: Option[String] = None,
        smithyVersion: SmithyVersion = SmithyVersion.Two
    ): ConversionTestInput = {
      ConversionTestInput(
        filePath,
        Some(jsonSpec),
        smithySpec,
        errorSmithySpec,
        smithyVersion
      )
    }
  }

  final case class ConversionResult(
      result: ToSmithyResult[ModelWrapper],
      expected: ModelWrapper
  )

  def runConversion(
      opts: ToSmithyCompilerOptions,
      input0: ConversionTestInput,
      remaining: ConversionTestInput*
  ): ConversionResult = {
    val inputs = input0 +: remaining
    val result =
      JsonSchemaCompiler.compile(
        opts,
        JsonSchemaCompilerInput.UnparsedSpecs(
          inputs // gather only specs that have a json input
            .toList
            .mapFilter(input => input.jsonSpec.map((input.filePath, _)))
            .map { case (path, content) => FileContents(path, content) }
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

  def runConversionTest(inputs: NonEmptyList[ConversionTestInput])(implicit
      loc: Location
  ): Unit = runConversionTest(inputs.head, inputs.tail: _*)

  def runConversionTest(
      input0: ConversionTestInput,
      remaining: ConversionTestInput*
  )(implicit
      loc: Location
  ): Unit = runConversionTestWithOpts(
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
    input0,
    remaining: _*
  )

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

  def runConversionTestWithOpts(
      opts: ToSmithyCompilerOptions,
      inputs: NonEmptyList[ConversionTestInput]
  )(implicit
      loc: Location
  ): Unit = runConversionTestWithOpts(opts, inputs.head, inputs.tail: _*)

  def runConversionTestWithOpts(
      opts: ToSmithyCompilerOptions,
      input0: ConversionTestInput,
      remaining: ConversionTestInput*
  )(implicit
      loc: Location
  ): Unit = {
    val ConversionResult(res, expected) = runConversion(
      opts,
      input0,
      remaining: _*
    )
    res match {
      case ToSmithyResult.Success(Nil, output) =>
        Assertions.assertEquals(output, expected)
      case ToSmithyResult.Success(errs, _) =>
        Assertions.fail(
          "Model assembled with errors: \n\t"
            + errs.map(_.getMessage).mkString("\n").replaceAll("\n", "\n\t")
        )
      case ToSmithyResult.Failure(err, errors) =>
        errors.foreach(println)
        Assertions.fail("Validating model failed: ", err)
    }
  }
}
