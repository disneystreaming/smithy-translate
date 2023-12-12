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

import cats.syntax.all._
import cats.data.NonEmptyChain
import smithytranslate.compiler.internals.json_schema.JsonSchemaToIModel
import cats.data.NonEmptyList
import org.everit.json.schema.Schema
import org.json.JSONObject
import io.circe.Json
import smithytranslate.compiler.internals.json_schema._
import smithytranslate.compiler.internals.IModel
import smithytranslate.compiler.ToSmithyCompilerOptions
import smithytranslate.compiler.AbstractToSmithyCompiler
import JsonSchemaCompilerInput._
import cats.data.Chain
import smithytranslate.compiler.ToSmithyError
import smithytranslate.compiler.FileContents

/** Converts json schema to a smithy model.
  */
object JsonSchemaCompiler
    extends AbstractToSmithyCompiler[JsonSchemaCompilerInput] {

  protected def convertToInternalModel(
      opts: ToSmithyCompilerOptions,
      input: JsonSchemaCompilerInput
  ): (Chain[ToSmithyError], IModel) = {
    val prepared = input match {
      case UnparsedSpecs(specs) =>
        val parser: String => Schema =
          schemaString => LoadSchema(new JSONObject(schemaString))
        specs.map { case FileContents(path, content) =>
          val ns = NonEmptyChain.fromNonEmptyList(removeFileExtension(path))
          val schema = parser(content)
          val json = io.circe.jawn.parse(content) match {
            case Left(error)  => throw error
            case Right(value) => value
          }
          (ns, schema, json)
        }
      case ParsedSpec(path, rawJson, schema) =>
        val ns = NonEmptyChain.fromNonEmptyList(removeFileExtension(path))
        List((ns, schema, rawJson))
    }
    prepared.foldMap { case (ns, schema, rawJson) =>
      JsonSchemaToIModel.compile(ns, schema, rawJson)
    }
  }

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

}
