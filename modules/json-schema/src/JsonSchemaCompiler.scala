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
import smithytranslate.compiler.internals.json_schema._
import smithytranslate.compiler.internals.IModel
import smithytranslate.compiler.ToSmithyCompilerOptions
import smithytranslate.compiler.AbstractToSmithyCompiler
import cats.data.Chain
import smithytranslate.compiler.ToSmithyError
import cats.catsParallelForId
import cats.Id
import smithytranslate.compiler.internals.NamespaceRemapper

/** Converts json schema to a smithy model.
  */
object JsonSchemaCompiler
    extends AbstractToSmithyCompiler[JsonSchemaCompilerInput] {

  protected def convertToInternalModel(
      opts: ToSmithyCompilerOptions,
      input: JsonSchemaCompilerInput
  ): (Chain[ToSmithyError], IModel) = {
    val remapper = new NamespaceRemapper(opts.namespaceRemaps)

    val (resolutionErrors, prepared) =
      CompilationUnitResolver
        .resolve[Id](input, opts.allowedRemoteBaseURLs, remapper)

    val (compilationErrors, result) =
      prepared
        .distinctBy(unit => (unit.namespace, unit.name.asRef))
        .foldMap(JsonSchemaToIModel.compile(_, remapper))

    (resolutionErrors ++ compilationErrors, result)
  }

}
