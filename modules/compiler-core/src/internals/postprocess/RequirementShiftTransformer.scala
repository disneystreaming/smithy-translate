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

package smithytranslate.compiler.internals
package postprocess

// If a shape is annotated as Required, annotate all the members that point to it as required
// and remove the required on the shape
//
// This is useful for schemas coming from components/apiRequests which contain a "required"
// property
private[compiler] object RequirementShiftTransformer
    extends IModelPostProcessor {
  def apply(model: IModel): IModel = {

    val isRequired = model.definitions.collect {
      case d if d.hints.contains(Hint.Required) => d.id
    }.toSet

    val amendedDefinitions = model.definitions
      .map { case d =>
        d.mapHints(_.filterNot(_ == Hint.Required))
      }
      .map {
        case s @ Structure(_, localFields, _, _) =>
          val amendedFields = localFields.map {
            case Field(id, tpe, hints) if (isRequired(tpe)) =>
              Field(id, tpe, Hint.Required :: hints)
            case other => other
          }
          s.copy(localFields = amendedFields)
        case other => other
      }
    IModel(amendedDefinitions, model.suppressions)
  }
}
