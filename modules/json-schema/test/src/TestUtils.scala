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
      jsonSpec: String,
      smithySpec: String,
      errorSmithySpec: Option[String] = None,
      smithyVersion: SmithyVersion = SmithyVersion.Two
  )

  final case class ConversionResult(
      result: ToSmithyResult[ModelWrapper],
      expected: ModelWrapper
  )

  def runConversion(
      input0: ConversionTestInput,
      remaining: ConversionTestInput*
  ): ConversionResult = {
    val inputs = input0 +: remaining
    val result =
      JsonSchemaCompiler.compile(
        ToSmithyCompilerOptions(
          useVerboseNames = false,
          validateInput = false,
          validateOutput = false,
          List.empty,
          input0.smithyVersion == SmithyVersion.One,
          debug = true
        ),
        JsonSchemaCompilerInput.UnparsedSpecs(
          inputs
            .map(i => FileContents(i.filePath, i.jsonSpec))
            .toList
        )
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
      case ToSmithyResult.Failure(err, errors) =>
        errors.foreach(println)
        Assertions.fail("Validating model failed: ", err)
      case ToSmithyResult.Success(Nil, output) =>
        Assertions.assertEquals(output, expected)
      case ToSmithyResult.Success(warnings, _) =>
        warnings.foreach(println)
        Assertions.assertEquals(warnings, Nil)
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
}
