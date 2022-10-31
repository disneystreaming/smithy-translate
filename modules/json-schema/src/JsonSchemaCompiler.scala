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

package smithytranslate.json_schema

import software.amazon.smithy.model.{Model => SmithyModel}
import cats.syntax.all._
import cats.data.NonEmptyChain
import smithytranslate.json_schema.internals.JsonSchemaToIModel
import smithytranslate.openapi.internals.IModelPostProcessor
import smithytranslate.openapi.internals.IModelToSmithy
import cats.data.NonEmptyList
import software.amazon.smithy.build.TransformContext
import org.everit.json.schema.Schema
import org.json.JSONObject
import smithytranslate.openapi.OpenApiCompiler._
import io.circe.Json
import smithytranslate.json_schema.internals.LoadSchema

/** Converts json schema to a smithy model.
  */
object JsonSchemaCompiler {

  type Input = (NonEmptyChain[String], Schema, Json)

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
    val parseSchema: String => Schema = schemaString =>
      LoadSchema(new JSONObject(schemaString))
    val inputs =
      stringInputs.map { case (path, content) =>
        val ns = NonEmptyChain.fromNonEmptyList(removeFileExtension(path))
        val schema = parseSchema(content)
        val json = io.circe.jawn.parse(content) match {
          case Left(error)  => throw error
          case Right(value) => value
        }
        (ns, schema, json)
      }
    compile(opts, inputs: _*)
  }

  def compile(
      opts: Options,
      inputs: Input*
  ): Result[SmithyModel] = {
    val (errors0, smithy0) = inputs.toList
      .foldMap(JsonSchemaToIModel.compile.tupled)
      .map(IModelPostProcessor(opts.useVerboseNames))
      .map(new IModelToSmithy(opts.useEnumTraitSyntax))
    val errors = errors0.toList

    scala.util
      .Try(validate(smithy0))
      .toEither
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
