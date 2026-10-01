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
import scala.jdk.CollectionConverters._
import software.amazon.smithy.model.shapes.EnumShape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.EnumTrait
import smithytranslate.compiler.ToSmithyResult.Failure
import smithytranslate.compiler.ToSmithyResult.Success
import smithytranslate.compiler.ToSmithyCompilerOptions
import smithytranslate.compiler.ToSmithyError
import smithytranslate.compiler.FileContents

final class OutputValidationSpec extends munit.FunSuite {

  private def convert(
      spec: String,
      validateOutput: Boolean,
      validateInput: Boolean = false,
      useEnumTraitSyntax: Boolean = false
  ) = OpenApiCompiler.compile(
    ToSmithyCompilerOptions(
      useVerboseNames = false,
      validateInput = validateInput,
      validateOutput = validateOutput,
      transformers = List.empty,
      useEnumTraitSyntax = useEnumTraitSyntax,
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

  TestUtils.allVersions.foreach { version =>
    List(false, true).foreach { useEnumTraitSyntax =>
      test(
        s"$version preserves colliding enum values (enum trait: $useEnumTraitSyntax)"
      ) {
        val spec = s"""|openapi: '$version'
                       |info: {title: test, version: '1.0'}
                       |paths: {}
                       |components:
                       |  schemas:
                       |    Value:
                       |      type: string
                       |      enum: [a-b, a_b, a_b_1, untouched]
                       |""".stripMargin
        convert(
          spec,
          validateOutput = true,
          validateInput = true,
          useEnumTraitSyntax = useEnumTraitSyntax
        ) match {
          case Success(errors, model) =>
            assertEquals(errors, Nil)
            if (useEnumTraitSyntax) {
              val values = model
                .expectShape(ShapeId.from("input#Value"))
                .expectTrait(classOf[EnumTrait])
                .getValues
                .asScala
                .map(_.getValue)
                .toList: @annotation.nowarn(
                "msg=class EnumTrait in package traits is deprecated"
              )
              assertEquals(values, List("a-b", "a_b", "a_b_1", "untouched"))
            } else {
              val values = model
                .expectShape(ShapeId.from("input#Value"), classOf[EnumShape])
                .getEnumValues
                .asScala
                .toMap
              assertEquals(
                values,
                Map(
                  "a_b" -> "a-b",
                  "a_b_2" -> "a_b",
                  "a_b_1" -> "a_b_1",
                  "untouched" -> "untouched"
                )
              )
            }
          case Failure(cause, _) =>
            fail("Expected successful enum conversion", cause)
        }
      }
    }
  }

  List(
    (
      "multiple types",
      "type: [string, 'null']",
      "type (null and multiple types)"
    ),
    (
      "formatted enum",
      "type: string, format: date, enum: ['2026-09-17']",
      "enum with format or x-format"
    )
  ).foreach { case (name, schema, keyword) =>
    test(s"OpenAPI 3.1 $name fails when output validation is enabled") {
      val spec = s"""|openapi: '3.1.0'
                     |info:
                     |  title: test
                     |  version: '1.0'
                     |paths: {}
                     |components:
                     |  schemas:
                     |    Value: {$schema}
                     |""".stripMargin
      val restriction = ToSmithyError.Restriction(
        s"Unsupported OpenAPI 3.1 schema keywords: $keyword."
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

}
