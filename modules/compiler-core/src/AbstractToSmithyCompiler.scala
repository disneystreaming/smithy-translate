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

package smithytranslate.compiler

import scala.jdk.OptionConverters._
import software.amazon.smithy.model.{Model => SmithyModel}
import cats.syntax.all._
import cats.data.Chain
import smithytranslate.compiler.internals.IModel
import smithytranslate.compiler.internals.IModelPostProcessor
import smithytranslate.compiler.internals.IModelToSmithy
import cats.data.NonEmptyList
import scala.jdk.CollectionConverters._
import software.amazon.smithy.model.validation.Severity
import software.amazon.smithy.model.validation.ValidatedResult
import software.amazon.smithy.build.TransformContext

/** Holds common logic that serves for the conversion of openapi/json-schema to smithy
  */
abstract class AbstractToSmithyCompiler[Input] protected[compiler] () {

  protected def convertToInternalModel(
      opts: ToSmithyCompilerOptions,
      input: Input
  ): (Chain[ToSmithyError], IModel)

  def compile(
      opts: ToSmithyCompilerOptions,
      input: Input
  ): ToSmithyResult[SmithyModel] = {
    val (errors0, smithy0) = convertToInternalModel(opts, input)
      .map(IModelPostProcessor(opts.useVerboseNames))
      .map(new IModelToSmithy(opts.useEnumTraitSyntax))
    val translationErrors = errors0.toList

    val assembled: ValidatedResult[SmithyModel] = validate(smithy0)
    val validationEvents = if (opts.debug) {
      assembled.getValidationEvents()
    } else {
      val errorEvents = assembled.getValidationEvents(Severity.ERROR)
      val dangerEvents = assembled.getValidationEvents(Severity.DANGER)
      val criticalErrors = new java.util.ArrayList(errorEvents)
      criticalErrors.addAll(dangerEvents)
      criticalErrors
    }
    val problematicSeverities = Set(Severity.DANGER, Severity.ERROR)
    val hasProblematicEvents = validationEvents
      .iterator()
      .asScala
      .map(_.getSeverity())
      .exists(problematicSeverities)

    val allErrors =
      if (hasProblematicEvents) {
        val smithyValidationFailed =
          ToSmithyError.SmithyValidationFailed(validationEvents.asScala.toList)
        smithyValidationFailed :: translationErrors
      } else {
        translationErrors
      }

    val transformedModel =
      assembled.getResult().toScala.map(transform(opts))

    (transformedModel, NonEmptyList.fromList(allErrors)) match {
      case (None, Some(nonEmptyErrors)) =>
        ToSmithyResult.Failure(nonEmptyErrors.head, nonEmptyErrors.tail)
      case (None, None) =>
        ToSmithyResult.Failure(
          ToSmithyError.SmithyValidationFailed(Nil),
          translationErrors
        )
      case (Some(model), None) =>
        ToSmithyResult.Success(translationErrors, model)
      case (Some(model), Some(nonEmptyErrors)) =>
        if (opts.validateOutput) {
          ToSmithyResult.Failure(nonEmptyErrors.head, nonEmptyErrors.tail)
        } else {
          ToSmithyResult.Success(allErrors, model)
        }
    }
  }

  private def validate(model: SmithyModel): ValidatedResult[SmithyModel] =
    SmithyModel.assembler().discoverModels().addModel(model).assemble()

  private def transform(opts: ToSmithyCompilerOptions)(model: SmithyModel): SmithyModel =
    opts.transformers.foldLeft(model)((m, t) =>
      t.transform(TransformContext.builder().model(m).build())
    )

}
