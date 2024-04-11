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

import org.typelevel.ci._

// Removes empty structures and changes shapes which target
// them to instead target Unit
private[compiler] object EmptyStructureToUnitTransformer
    extends IModelPostProcessor {

  def apply(model: IModel): IModel = {
    val defs = model.definitions.map(d => d.id -> d).toMap

    val structuresToRemove = model.definitions.flatMap {
      case s: Structure if isEmptyStructure(defs, s) =>
        Some(s.id)
      case _ => None
    }.toSet
    val amendedDefs = model.definitions.flatMap {
      case s: Structure if structuresToRemove(s.id) => None
      case op: OperationDef =>
        val changeInput: OperationDef => OperationDef = o =>
          if (o.input.exists(structuresToRemove)) o.copy(input = Some(unit))
          else o
        val changeOutput: OperationDef => OperationDef = o =>
          if (o.output.exists(structuresToRemove)) o.copy(output = Some(unit))
          else o
        Some(changeInput.andThen(changeOutput)(op))
      case other => Some(other)
    }
    IModel(amendedDefs, model.suppressions)
  }

  private val unit =
    DefId(
      Namespace(List("smithy", "api")),
      Name(Segment.StandardLib(ci"Unit"))
    )

  // consider empty if has no fields OR if has one field with Body hint (httpPayload)
  private def isEmptyStructure(
      defs: Map[DefId, Definition],
      d: Definition
  ): Boolean =
    d match {
      case s: Structure =>
        (s.localFields.isEmpty && s.parents.isEmpty && s.hints.isEmpty) || {
          def isHttpPayload =
            s.localFields.length == 1 && s.localFields.head.hints
              .contains(Hint.Body)
          def isHttpPayloadEmpty = defs
            .get(s.localFields.head.tpe)
            .exists(isEmptyStructure(defs, _))
          isHttpPayload && isHttpPayloadEmpty
        }
      case _ => false
    }
}
