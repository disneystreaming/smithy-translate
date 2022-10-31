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

package smithytranslate.cli.runners.openapi

import cats.data.NonEmptyList
import smithytranslate.openapi.OpenApiCompiler
import smithytranslate.cli.transformer.TranslateTransformer
import software.amazon.smithy.model.Model
import smithytranslate.json_schema.JsonSchemaCompiler

object ParseAndCompile {
  def openapi(
      inputPaths: NonEmptyList[os.Path],
      useVerboseNames: Boolean,
      failOnValidationErrors: Boolean,
      transformers: List[TranslateTransformer],
      useEnumTraitSyntax: Boolean
  ): OpenApiCompiler.Result[Model] = {
    val includedExtensions = List("yaml", "yml", "json")
    val inputs = getInputs(inputPaths, includedExtensions)
    val opts = OpenApiCompiler.Options(
      useVerboseNames,
      failOnValidationErrors,
      transformers,
      useEnumTraitSyntax
    )
    OpenApiCompiler.parseAndCompile(opts, inputs: _*)
  }

  def jsonSchema(
      inputPaths: NonEmptyList[os.Path],
      useVerboseNames: Boolean,
      failOnValidationErrors: Boolean,
      transformers: List[TranslateTransformer],
      useEnumTraitSyntax: Boolean
  ): OpenApiCompiler.Result[Model] = {
    val includedExtensions = List("json")
    val inputs = getInputs(inputPaths, includedExtensions)
    val opts = OpenApiCompiler.Options(
      useVerboseNames,
      failOnValidationErrors,
      transformers,
      useEnumTraitSyntax
    )
    JsonSchemaCompiler.parseAndCompile(opts, inputs: _*)
  }

  private def getInputs(
      paths: NonEmptyList[os.Path],
      includedExtensions: List[String]
  ): List[(NonEmptyList[String], String)] = {
    paths.toList.flatMap { path =>
      if (os.isDir(path)) {
        val openapiFiles = os
          .walk(path)
          .filter(p => includedExtensions.contains(p.ext))
        openapiFiles.map { in =>
          val subParts = in
            .relativeTo(path)
            .segments
            .toList
          val baseNs = path.segments.toList.lastOption.toList
          val nsPath =
            baseNs ++ subParts
          NonEmptyList.fromListUnsafe(nsPath) -> os
            .read(in)
        }.toList
      } else {
        List((NonEmptyList.of(path.last), os.read(path)))
      }
    }
  }
}
