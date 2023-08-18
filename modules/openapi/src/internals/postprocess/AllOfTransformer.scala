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
import org.typelevel.ci._
import scala.collection.mutable.ListBuffer

object AllOfTransformer extends IModelPostProcessor {

  private val DocumentPrimitive =
    DefId(Namespace(List("smithy", "api")), Name.stdLib("Document"))

  private implicit val defIdEq: Eq[DefId] = Eq.fromUniversalEquals

  // Checks if a given shape is ever referenced outside of being used as a mixin
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

  private case class ParentHasOtherRefsResult(
      newDef: Structure,
      newMixin: Structure,
      newParent: Structure
  )

  private def parentHasOtherReferencesCase(
      parent: Structure,
      newDef: Structure
  ): ParentHasOtherRefsResult = {
    val newMixinParentId =
      parent.id.copy(name = parent.id.name :+ Segment.Arbitrary(ci"Mixin"))

    val newFields =
      parent.localFields
        .map(f => f.copy(id = f.id.copy(modelId = newMixinParentId)))

    // 1. Make a new structure, identical to the parent, postfixed with `Mixin` that has all fields
    val newMixin =
      parent.copy(
        id = newMixinParentId,
        localFields = newFields,
        hints = parent.hints :+ Hint.IsMixin
      )

    // 2. Make existing parent structure have 0 fields and instead mixin the structure made in previous step
    val newParent =
      parent.copy(
        localFields = Vector.empty,
        hints = List(Hint.HasMixin(newMixinParentId))
      )

    // 3. Update so this structure mixes in new structure instead of bringing in all fields
    val nd =
      newDef.copy(hints = newDef.hints :+ Hint.HasMixin(newMixinParentId))
    ParentHasOtherRefsResult(nd, newMixin, newParent)
  }

  private case class TopLevelParentNoRefsResult(
      newDef: Structure,
      newParent: Structure
  )
  private def topLevelParentNoReferences(
      parent: Structure,
      newDef: Structure
  ): TopLevelParentNoRefsResult = {
    // 1. Make structure parent a mixin
    val newParent = parent.copy(hints = parent.hints :+ Hint.IsMixin)
    // 2. Apply as mixin to this structure instead of moving all fields in
    val nd = newDef.copy(hints = newDef.hints :+ Hint.HasMixin(parent.id))
    TopLevelParentNoRefsResult(nd, newParent)
  }

  private case class NonTopLevelParentNoRefsResult(
      newDef: Structure,
      removeParent: Structure
  )
  private def nonTopLevelParentNoReferences(
      parent: Structure,
      newDef: Structure
  ): NonTopLevelParentNoRefsResult = {
    // Move fields down from parent and remove parent if parent is not a top level
    // shape (meaning it is a fabricated AllOf shape created by the OpenApi => IModel transform)
    val newFields =
      parent.localFields.map(f => f.copy(id = f.id.copy(modelId = newDef.id)))
    val remove = parent
    val nd = newDef.copy(localFields = newFields)
    NonTopLevelParentNoRefsResult(nd, remove)
  }

  private def moveParentFieldsAndCreateMixins(
      all: Vector[Definition]
  ): Vector[Definition] = {
    val allShapes: ListBuffer[Definition] = ListBuffer.from(all)
    def replace(d: Definition) = {
      remove(d)
      add(d)
    }
    def remove(d: Definition) =
      allShapes.remove(allShapes.indexWhere(_.id === d.id))
    def add(d: Definition) =
      allShapes += d

    def revertMixinHints(newDef: Structure) = {
      allShapes.filter(shp => newDef.parents.contains(shp.id)).foreach {
        case par: Structure =>
          replace(
            par.copy(hints = par.hints.filterNot(_ == Hint.IsMixin))
          )
        case _ => ()
      }
    }

    all.foreach {
      case struct: Structure =>
        val parents = struct.parents.flatMap(p => allShapes.find(_.id === p))
        var isDocument = false
        var newDef: Structure = struct
        parents.foreach {
          case Newtype(_, DocumentPrimitive, _) =>
            isDocument = true
          case parent: Structure =>
            // FOR EACH structure parent, detect if they are used in any contexts outside of this structure
            val parentHasOtherReferences =
              isReferencedAsTarget(parent, all)
            val parentIsTopLevel = parent.hints.contains(Hint.TopLevel)

            if (parentHasOtherReferences) {
              val result = parentHasOtherReferencesCase(parent, newDef)
              newDef = result.newDef
              add(result.newMixin)
              replace(result.newParent)
            } else {
              if (parentIsTopLevel) {
                val result = topLevelParentNoReferences(parent, newDef)
                newDef = result.newDef
                replace(result.newParent)
              } else {
                val result = nonTopLevelParentNoReferences(parent, newDef)
                newDef = result.newDef
                remove(result.removeParent)
              }
            }
          case other => other
        }
        if (isDocument) {
          // if a document then no need to consider any of the parents a mixin (the mixin would not ever be used)
          // so we retroactively remove any `IsMixin` hints we added above
          revertMixinHints(newDef)
        }
        val n: Definition =
          if (isDocument)
            Newtype(
              newDef.id,
              DocumentPrimitive,
              newDef.hints
            )
          else newDef
        replace(
          n
        )
      case other => Vector(other)
    }
    allShapes.toVector
  }

  private def transform(in: IModel): Vector[Definition] = {
    val allShapes = in.definitions
    moveParentFieldsAndCreateMixins(allShapes)
  }

  def apply(in: IModel): IModel = {
    IModel(transform(in), in.suppressions)
  }
}
