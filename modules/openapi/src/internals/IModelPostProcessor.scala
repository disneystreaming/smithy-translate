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

package smithytranslate.openapi.internals

import postprocess._

trait IModelPostProcessor extends (IModel => IModel)

object IModelPostProcessor {
  private[this] val defaultTransformers: List[IModelPostProcessor] = List(
    ExternalMemberRefTransformer,
    NewtypeTransformer,
    FixMissingTargetsTransformer,
    AllOfTransformer,
    TaggedUnionTransformer,
    DiscriminatedTransformer,
    RequirementShiftTransformer,
    ContentTypeShiftTransformer,
    ReorientDefaultValueTransformer,
    DropRequiredWhenDefaultValue
  )

  private[this] def transform(
      in: IModel,
      allTransformers: List[IModelPostProcessor]
  ): IModel =
    allTransformers.foldLeft(in)((vd, transformer) => transformer(vd))

  def apply(useVerboseNames: Boolean)(in: IModel): IModel = {
    val nameTransform =
      if (useVerboseNames) Nil else List(SimplifyNameTransformer)
    val allTransformers = defaultTransformers ++ nameTransform
    this.transform(in, allTransformers)
  }
}
