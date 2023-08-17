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
import cats.kernel.Eq

object AllOfTransformer extends IModelPostProcessor {

  private case class State(
      fieldsToAdd: Vector[Field],
      defsToRemove: Set[DefId],
      isDocument: Boolean,
      hints: List[Hint],
      newOrUpdateDefs: Set[Definition]
  ) {
    def addFields(f: Vector[Field]) = copy(fieldsToAdd = fieldsToAdd ++ f)
    def addRemoveDef(d: DefId) = copy(defsToRemove = defsToRemove + d)
    def setIsDocument = copy(isDocument = true)
    def withHints(hints: List[Hint]) = copy(hints = this.hints ++ hints)
    def addOrUpdateDef(d: Definition) =
      copy(newOrUpdateDefs = newOrUpdateDefs + d)
  }

  private object State {
    def empty: State =
      State(Vector.empty, Set.empty, isDocument = false, List.empty, Set.empty)
  }

  private val DocumentPrimitive =
    DefId(Namespace(List("smithy", "api")), Name.stdLib("Document"))

  private implicit val defIdEq: Eq[DefId] = Eq.fromUniversalEquals

  private def isReferencedAsTarget(
      shape: Definition,
      allShapes: Vector[Definition]
  ): Boolean = {
    @tailrec
    def loop(
        remainingShapes: List[Definition],
        isReferenced: Boolean = false
    ): Boolean =
      remainingShapes match {
        case _ if isReferenced => isReferenced
        case Nil               => isReferenced
        case (s: Shape) :: tail =>
          val hasRef = s match {
            case s: Structure =>
              s.localFields.exists(_.tpe === shape.id)
            case s: SetDef =>
              s.member === shape.id
            case l: ListDef =>
              l.member === shape.id
            case m: MapDef =>
              m.key === shape.id || m.value === shape.id
            case u: Union =>
              u.alts.exists(_.tpe === shape.id)
            case n: Newtype =>
              n.target === shape.id
            case _: Enumeration => false
            case o: OperationDef =>
              val allRefs = o.input.toVector ++ o.output.toVector ++ o.errors
              allRefs.contains_(shape.id)
            case _: ServiceDef => false
          }
          loop(tail, hasRef)
        case _ :: tail => loop(tail, isReferenced)
      }

    loop(allShapes.toList)
  }

  // FOR EACH structure parent, detect if they are used in any contexts outside of this structure
  // IF NOT:
  //   - Make structure parent a mixin
  //   - Apply as mixin to this structure instead of moving all fields in
  // IF YES:
  //   - Make a new structure, identical to the parent, postfixed with `Mixin` that has all fields
  //   - Make existing parent structure have 0 fields and instead mixin the structure made in previous step
  //   - Update so this structure mixes in new structure instead of bringing in all fields
  @tailrec
  private def getAllParentFields(
      newId: DefId,
      allShapes: Vector[Definition],
      parents: Vector[Definition],
      state: State = State.empty
  ): State = parents.toList match {
    case parent :: tail =>
      val parentHasOtherReferences = isReferencedAsTarget(parent, allShapes)
      parent match {
        case s: Structure if !parentHasOtherReferences =>
          state
            .addOrUpdateDef(s.mapHints(_ :+ Hint.IsMixin))
            .withHints(List(Hint.HasMixin(s.id)))
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
    val (remove, newOrUpdate, result) = allShapes.map {
      case s @ Structure(id, localFields, parents, _) if parents.nonEmpty =>
        val parentDefs =
          allShapes.filter(definition => parents.contains(definition.id))
        val State(
          fieldsToAdd,
          defsToRemove,
          isDocument,
          hints,
          newOrUpdateDefs
        ) =
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
          newOrUpdateDefs,
          finalShape
        )
      case other => (Set.empty, Set.empty, other)
    }.unzip3

    val updateOrAdd = newOrUpdate.flatten
    val toRemove = remove.flatten ++ updateOrAdd.map(_.id)
    result.filterNot(d => toRemove.contains(d.id)) ++ updateOrAdd.toVector
  }

  def apply(in: IModel): IModel = {
    IModel(transform(in), in.suppressions)
  }
}
