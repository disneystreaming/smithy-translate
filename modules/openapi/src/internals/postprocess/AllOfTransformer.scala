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

import cats.syntax.all._
import scala.annotation.tailrec

object AllOfTransformer extends IModelPostProcessor {

  private case class State(
      fieldsToAdd: Vector[Field],
      defsToRemove: Set[DefId],
      isDocument: Boolean,
      hints: List[Hint]
  ) {
    def addFields(f: Vector[Field]) = copy(fieldsToAdd = fieldsToAdd ++ f)
    def addRemoveDef(d: DefId) = copy(defsToRemove = defsToRemove + d)
    def setIsDocument = copy(isDocument = true)
    def withHints(hints: List[Hint]) = copy(hints = this.hints ++ hints)
  }

  private object State {
    def empty: State =
      State(Vector.empty, Set.empty, isDocument = false, List.empty)
  }

  private val DocumentPrimitive =
    DefId(Namespace(List("smithy", "api")), Name.stdLib("Document"))

  @tailrec
  private def getAllParentFields(
      newId: DefId,
      allShapes: Vector[Definition],
      parents: Vector[Definition],
      state: State = State.empty
  ): State = parents.toList match {
    case parent :: tail =>
      parent match {
        case Structure(id, local, newParents, hints) =>
          val updatedLocal =
            local.map(f => f.copy(id = f.id.copy(modelId = newId)))
          val newState = state.addFields(updatedLocal).withHints(hints)
          val finalState =
            if (!hints.contains(Hint.TopLevel)) newState.addRemoveDef(id)
            else newState
          getAllParentFields(
            newId,
            allShapes,
            tail.toVector ++ allShapes.filter(s => newParents.contains(s.id)),
            finalState
          )
        case Newtype(_, DocumentPrimitive, hints) =>
          val newState = state.setIsDocument.withHints(hints)
          getAllParentFields(newId, allShapes, tail.toVector, newState)
        case _ => getAllParentFields(newId, allShapes, tail.toVector, state)
      }
    case Nil => state
  }

  private def transform(in: IModel): Vector[Definition] = {
    val allShapes = in.definitions
    val (remove, result) = allShapes
      .map {
        case s @ Structure(id, localFields, parents, _) if parents.nonEmpty =>
          val parentDefs =
            allShapes.filter(definition => parents.contains(definition.id))
          val State(fieldsToAdd, defsToRemove, isDocument, hints) =
            getAllParentFields(id, allShapes, parentDefs)
          val newStruct = s.copy(
            localFields = localFields ++ fieldsToAdd,
            parents = Vector.empty,
            hints = s.hints ++ hints
          )
          val finalShape =
            if (isDocument) Newtype(id, DocumentPrimitive, hints)
            else newStruct
          (
            defsToRemove,
            finalShape
          )
        case other => (Set.empty, other)
      }
      .unzip
      .leftMap(_.flatten)

    result.filterNot(d => remove.contains(d.id))
  }

  def apply(in: IModel): IModel = {
    IModel(transform(in), in.suppressions)
  }
}
