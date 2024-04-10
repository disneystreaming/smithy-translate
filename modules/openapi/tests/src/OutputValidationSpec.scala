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
import smithytranslate.compiler.ToSmithyCompilerOptions
import smithytranslate.compiler.ToSmithyError
import smithytranslate.compiler.FileContents

final class OutputValidationSpec extends munit.FunSuite {

  test("Output should be validated when specified") {
    val spec = """|openapi: '3.0.'
                  |info:
                  |  title: test
                  |  version: '1.0'
                  |paths:
                  |  /{test}:
                  |    get:
                  |      operationId: test
                  |      responses:
                  |        '200':
                  |          content:
                  |            application/json:
                  |              schema:
                  |                type: object
                  |""".stripMargin

    val input = OpenApiCompilerInput.UnparsedSpecs(
      List(
        FileContents(NonEmptyList.of("input.yaml"), spec)
      )
    )
    def convert(validateOutput: Boolean) = OpenApiCompiler.compile(
      ToSmithyCompilerOptions(
        useVerboseNames = false,
        validateInput = false,
        validateOutput = validateOutput,
        List.empty,
        useEnumTraitSyntax = false,
        debug = false
      ),
      input
    )

    val resultExpectingSuccess = convert(validateOutput = false)
    assert(resultExpectingSuccess.isInstanceOf[Success[_]])

    val resultExpectingFailure = convert(validateOutput = true)
    resultExpectingFailure match {
      case Failure(ToSmithyError.SmithyValidationFailed(events), _) =>
        // Expecting a failure indicating that the "test" operation is invalid due to not having
        // an input member matching the `{test}` path segment.
        assertEquals(events.size, 1)
        assert(events.exists(_.getId() == "HttpLabelTrait"))
      case Failure(cause, _) =>
        fail(
          s"expected a SmithyValidationFailed but got a ${cause.getClass().getSimpleName()}"
        )
      case Success(_, _) =>
        fail("expected a failure")
    }
  }

}
