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

  List(
    ("mixed values", "type: string\nenum: [red, 1]", "enum"),
    ("empty string value", "type: string\nenum: [red, '']", "enum"),
    ("null-only enum", "type: string\nenum: [null]", "enum"),
    ("enum containing null", "type: string\nenum: [red, null]", "enum"),
    ("untyped enum containing null", "enum: [red, null]", "enum"),
    ("nullable enum", "type: [string, 'null']\nenum: [red, null]", "type"),
    (
      "reference sibling",
      "$ref: '#/components/schemas/Other'\nenum: [red]",
      "$ref siblings"
    ),
    (
      "formatted enum",
      "type: string\nformat: date\nenum: ['2026-09-17']",
      "enum with format"
    ),
    ("enum default", "type: string\nenum: [red]\ndefault: red", "default")
  ).foreach { case (name, schema, keyword) =>
    test(
      s"OpenAPI 3.1 unsupported $name reports a restriction and fails validation"
    ) {
      val indented = schema.linesIterator.map("      " + _).mkString("\n")
      val spec = s"""|openapi: '3.1.0'
                     |info:
                     |  title: enum validation
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Other:
                     |      type: string
                     |      enum: [red, green]
                     |    Value:
                     |$indented
                     |""".stripMargin
      convert(spec, validateOutput = false, validateInput = true) match {
        case Success(errors, _) =>
          assert(errors.exists {
            case error: ToSmithyError.Restriction =>
              error.message.contains(keyword)
            case _ => false
          })
        case Failure(cause, _) => fail("Expected partial output", cause)
      }
      convert(spec, validateOutput = true, validateInput = true) match {
        case Failure(cause: ToSmithyError.Restriction, _) =>
          assert(cause.message.contains(keyword))
        case Failure(cause, _) => fail("Expected a restriction failure", cause)
        case Success(_, _)     => fail("Expected a restriction failure")
      }
    }
  }

  private def convert(
      spec: String,
      validateOutput: Boolean,
      validateInput: Boolean = false
  ) = OpenApiCompiler.compile(
    ToSmithyCompilerOptions(
      useVerboseNames = false,
      validateInput = validateInput,
      validateOutput = validateOutput,
      transformers = List.empty,
      useEnumTraitSyntax = false,
      debug = false,
      allowedRemoteBaseURLs = Set.empty,
      namespaceRemaps = Map.empty
    ),
    OpenApiCompilerInput.UnparsedSpecs(
      List(FileContents(NonEmptyList.of("input.yaml"), spec))
    )
  )

  TestUtils.allVersions.foreach { version =>
    test(s"$version output should be validated when specified") {
      val spec = s"""|openapi: '$version'
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
                     |                type: string
                     |""".stripMargin

      val resultExpectingSuccess = convert(spec, validateOutput = false)
      assert(resultExpectingSuccess.isInstanceOf[Success[_]])

      val resultExpectingFailure = convert(spec, validateOutput = true)
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

  test("OpenAPI 3.1 multiple types fail when output validation is enabled") {
    val spec = """|openapi: '3.1.0'
                  |info:
                  |  title: test
                  |  version: '1.0'
                  |paths: {}
                  |components:
                  |  schemas:
                  |    NullableString:
                  |      type: [string, 'null']
                  |""".stripMargin
    val restriction = ToSmithyError.Restriction(
      "Unsupported OpenAPI 3.1 schema keywords: type (null and multiple types)."
    )

    convert(spec, validateOutput = false, validateInput = true) match {
      case Success(errors, _) => assertEquals(errors, List(restriction))
      case Failure(cause, _)  => fail("Expected partial output", cause)
    }
    convert(spec, validateOutput = true, validateInput = true) match {
      case Failure(cause: ToSmithyError.Restriction, errors) =>
        assertEquals(cause, restriction)
        assertEquals(errors, Nil)
      case Failure(cause, _) => fail("Expected a restriction failure", cause)
      case Success(_, _)     => fail("Expected a restriction failure")
    }
  }

}
