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
package postprocess

// If a shape is annotated with ContentTypeLabel, annotate all the members that point to it
// with ContentTypeLabel and remove the ContentTypeLabel annotation on the shape
//
// This is useful for schemas coming from components/apiRequests
object ContentTypeShiftTransformer extends IModelPostProcessor {
  def apply(model: IModel): IModel = {

    val contentTypes = model.definitions.flatMap { d =>
      d.hints.find(_.isInstanceOf[Hint.ContentTypeLabel]).map(d.id -> _)
    }.toMap

    val amendedDefinitions = model.definitions
      .map { case d =>
        d.mapHints(_.filterNot(_.isInstanceOf[Hint.ContentTypeLabel]))
      }
      .map {
        case s @ Structure(_, localFields, _, _) =>
          val amendedFields = localFields.map {
            case Field(id, tpe, hints) if (contentTypes.keySet(tpe)) =>
              Field(id, tpe, contentTypes(tpe) :: hints)
            case other => other
          }
          s.copy(localFields = amendedFields)
        case other => other
      }
    IModel(amendedDefinitions, model.suppressions)
  }
}
