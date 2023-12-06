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

package smithytranslate.openapi

import io.swagger.v3.oas.models.OpenAPI
import software.amazon.smithy.model.{Model => SmithyModel}
import cats.syntax.all._
import cats.data.NonEmptyChain
import smithytranslate.openapi.internals.OpenApiToIModel
import smithytranslate.openapi.internals.IModelPostProcessor
import smithytranslate.openapi.internals.IModelToSmithy
import io.swagger.parser.OpenAPIParser
import cats.data.NonEmptyList
import scala.jdk.CollectionConverters._
import software.amazon.smithy.build.ProjectionTransformer
import software.amazon.smithy.build.TransformContext
import cats.Functor
import smithytranslate.openapi.OpenApiCompiler.SmithyVersion.One
import smithytranslate.openapi.OpenApiCompiler.SmithyVersion.Two
import software.amazon.smithy.model.validation.ValidatedResultException
import software.amazon.smithy.model.validation.Severity
import java.util.stream.Collectors

/** Converts openapi to a smithy model.
  */
object OpenApiCompiler {

  // Either the smithy validation fails, in which case we get a left with
  // the list of errors. Or smithy validation works and we get a pair of
  // errors (that still pass smithy validation) and a smithy model
  sealed trait Result[+A]
  case class Failure[A](cause: Throwable, errors: List[ModelError])
      extends Result[A]
  case class Success[A](error: List[ModelError], value: A) extends Result[A]

  object Result {
    implicit val functor: Functor[Result] = new Functor[Result]() {

      override def map[A, B](fa: Result[A])(f: A => B): Result[B] = fa match {
        case Failure(cause, errors) => Failure(cause, errors)
        case Success(error, value)  => Success(error, f(value))
      }

    }
  }

  sealed abstract class SmithyVersion extends Product with Serializable {
    override def toString(): String = this match {
      case One => "1.0"
      case Two => "2.0"
    }
  }
  object SmithyVersion {
    case object One extends SmithyVersion
    case object Two extends SmithyVersion

    def fromString(string: String): Either[String, SmithyVersion] = {
      val versionOne = Set("1", "1.0")
      val versionTwo = Set("2", "2.0")
      if (versionOne(string)) Right(SmithyVersion.One)
      else if (versionTwo(string)) Right(SmithyVersion.Two)
      else
        Left(
          s"expected one of ${versionOne ++ versionTwo}, but got '$string'"
        )
    }
  }

  final case class Options(
      useVerboseNames: Boolean,
      failOnValidationErrors: Boolean,
      transformers: List[ProjectionTransformer],
      useEnumTraitSyntax: Boolean,
      debug: Boolean
  ) {
    val debugModelValidationError: Throwable => Throwable = {
      case ex: ValidatedResultException if !debug =>
        new ValidatedResultException(
          ex.getValidationEvents()
            .stream()
            .filter(err =>
              err.getSeverity == Severity.ERROR || err.getSeverity == Severity.DANGER
            )
            .collect(Collectors.toList())
        )
      case ex: Throwable => ex
    }
  }

  type Input = (NonEmptyList[String], Either[List[String], OpenAPI])

  private def removeFileExtension(
      path: NonEmptyList[String]
  ): NonEmptyList[String] = {
    val lastSplit = path.last.split('.')
    val newLast =
      if (lastSplit.size > 1) lastSplit.dropRight(1) else lastSplit
    NonEmptyList.fromListUnsafe(
      path.toList.dropRight(1) :+ newLast.mkString(".")
    )
  }

  def parseAndCompile(
      opts: Options,
      stringInputs: (NonEmptyList[String], String)*
  ): Result[SmithyModel] = {
    val parser = new OpenAPIParser()
    val openapiInputs =
      stringInputs.map(
        _.bimap(
          removeFileExtension,
          in => {
            val result = parser.readContents(in, null, null)

            if (
              opts.failOnValidationErrors && !result.getMessages().isEmpty()
            ) {
              Left(result.getMessages().asScala.toList)
            } else {
              // in some cases, the validation error is important enough that
              // parsing fails and `getOpenAPI` returns null. in this case
              // Left is returned with the error messages (even if failOnValidationErrors is false)
              Option(result.getOpenAPI())
                .toRight(result.getMessages().asScala.toList)
            }
          }
        )
      )
    compile(opts, openapiInputs: _*)
  }

  def compile(
      opts: Options,
      inputs: Input*
  ): Result[SmithyModel] = {
    val (errors0, smithy0) = inputs.toList
      .map(_.leftMap(NonEmptyChain.fromNonEmptyList))
      .foldMap { case (c, e) => OpenApiToIModel.compile(c, e) }
      .map(IModelPostProcessor(opts.useVerboseNames))
      .map(new IModelToSmithy(opts.useEnumTraitSyntax))
    val errors = errors0.toList

    scala.util
      .Try(validate(smithy0))
      .toEither
      .leftMap(opts.debugModelValidationError)
      .map(transform(opts))
      .fold(
        err => Failure(err, errors),
        model => Success(errors.toList, model)
      )
  }

  private def validate(model: SmithyModel): SmithyModel =
    SmithyModel.assembler().discoverModels().addModel(model).assemble().unwrap()

  private def transform(opts: Options)(model: SmithyModel): SmithyModel =
    opts.transformers.foldLeft(model)((m, t) =>
      t.transform(TransformContext.builder().model(m).build())
    )

}
