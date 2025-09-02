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

import software.amazon.smithy.build.ProjectionTransformer
import cats.data.NonEmptyChain
import cats.data.Chain

final case class ToSmithyCompilerOptions(
    useVerboseNames: Boolean,
    validateInput: Boolean,
    validateOutput: Boolean,
    transformers: List[ProjectionTransformer],
    useEnumTraitSyntax: Boolean,
    debug: Boolean,
    allowedRemoteBaseURLs: Set[String],
    namespaceRemaps: Map[NonEmptyChain[String], Chain[String]]
)

object ToSmithyCompilerOptions {
  def apply(
    useVerboseNames: Boolean,
    validateInput: Boolean,
    validateOutput: Boolean,
    transformers: List[ProjectionTransformer],
    useEnumTraitSyntax: Boolean,
    debug: Boolean,
  ): ToSmithyCompilerOptions =
    ToSmithyCompilerOptions(
      useVerboseNames,
      validateInput,
      validateOutput,
      transformers,
      useEnumTraitSyntax,
      debug,
      Set.empty,
      Map.empty
    )
}
