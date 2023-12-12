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

// If a member is marked as `required` but also has a `defaultValue`, we can
// get rid of the `required` annotation.
private[compiler] object DropRequiredWhenDefaultValue
    extends IModelPostProcessor {

  def apply(model: IModel): IModel = {
    val amendedDefs = model.definitions
      .map {
        case d: Structure =>
          val newFields = d.localFields.map { f =>
            val fieldToProcess = for {
              _ <- f.hints.find(_.isInstanceOf[Hint.DefaultValue])
              _ <- f.hints.find(_.isInstanceOf[Hint.Required.type])
            } yield f
            fieldToProcess
              .map(_.mapHints(_.filter(!_.isInstanceOf[Hint.Required.type])))
              .getOrElse(f)
          }
          d.copy(localFields = newFields)
        case other => other
      }
    IModel(amendedDefs, model.suppressions)
  }
}
