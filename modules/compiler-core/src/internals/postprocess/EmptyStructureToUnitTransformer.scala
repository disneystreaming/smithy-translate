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
    // for the purposes of this transformer, we don't care about references from operation shapes
    // we are just using these to see when we should convert operation input to Unit
    val allTargets = util.getAllTargets(model.definitions.filter {
      case _: OperationDef => false
      case _               => true
    })

    val structuresToRemove = model.definitions.flatMap {
      case s: Structure if isEmptyStructure(defs, s, allTargets) =>
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
      d: Definition,
      allTargets: Set[DefId],
      seen: Set[DefId] = Set.empty
  ): Boolean =
    d match {
      case s: Structure if !seen.contains(s.id) =>
        val hasNoHints = s.hints.isEmpty
        // we also want to remove even if it is top level as long as it is totally empty and is never referenced
        val topLevelNoRefs =
          s.hints.forall(_ == Hint.TopLevel) && !allTargets.contains(s.id)
        (s.localFields.isEmpty && s.parents.isEmpty && (hasNoHints || topLevelNoRefs)) || {
          val isHttpPayload =
            s.localFields.length == 1 && s.localFields.head.hints
              .contains(Hint.Body)
          val isHttpPayloadEmpty = s.localFields.headOption
            .flatMap(f =>
              defs
                .get(f.tpe)
            )
            .exists(isEmptyStructure(defs, _, allTargets, seen + s.id))
          isHttpPayload && isHttpPayloadEmpty
        }
      case _ => false
    }
}
