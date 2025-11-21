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

import cats.data.NonEmptyList
import smithytranslate.compiler.ToSmithyResult.Failure
import smithytranslate.compiler.ToSmithyResult.Success
import munit.Location
import smithytranslate.compiler.ToSmithyCompilerOptions
import smithytranslate.compiler.ToSmithyError
import smithytranslate.compiler.FileContents

final class DebugSpec extends munit.FunSuite {

  private def load(fileName: String, debug: Boolean) = {
    val content = scala.io.Source
      .fromResource(fileName)
      .getLines()
      .mkString("\n")

    val options = ToSmithyCompilerOptions(
      useVerboseNames = false,
      validateInput = false,
      validateOutput = true,
      transformers = List.empty,
      useEnumTraitSyntax = false,
      allowedRemoteBaseURLs = Set.empty,
      namespaceRemaps = Map.empty,
      debug = debug
    )

    val input = OpenApiCompilerInput.UnparsedSpecs(
      List(
        FileContents(NonEmptyList.of(fileName), content)
      )
    )
    OpenApiCompiler.compile(options, input)
  }

  private def testFilteredErrors(debug: Boolean, expectedCount: Int)(implicit
      loc: Location
  ) = {
    load("issue-23.json", debug) match {
      case Failure(ToSmithyError.SmithyValidationFailed(events), _) =>
        assertEquals(events.size, expectedCount)
      case Failure(cause, _) =>
        fail(
          s"expected a ValidatedResultException but got a ${cause.getClass().getSimpleName()}"
        )
      case Success(_, _) => fail("expected a failure")
    }
  }

  test("load with debug leaves validation events untouched") {
    testFilteredErrors(debug = true, expectedCount = 2)
  }
  test("load without debug filters validation events") {
    testFilteredErrors(debug = false, expectedCount = 2)
  }
}
