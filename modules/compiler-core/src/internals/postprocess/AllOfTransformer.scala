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

private[compiler] object AllOfTransformer extends IModelPostProcessor {

  // returns all Ids that are ever referenced as a target
  private def getAllTargets(
      allShapes: Vector[Definition]
  ): Set[DefId] = {
    allShapes.flatMap {
      case s: Structure =>
        s.localFields.map(_.tpe)
      case s: SetDef =>
        Vector(s.member)
      case l: ListDef =>
        Vector(l.member)
      case m: MapDef =>
        Vector(m.key, m.value)
      case u: Union =>
        u.alts.map(_.tpe)
      case n: Newtype =>
        Vector(n.target)
      case _: Enumeration => Vector.empty
      case o: OperationDef =>
        val allRefs = o.input.toVector ++ o.output.toVector ++ o.errors
        allRefs
      case _: ServiceDef => Vector.empty
    }.toSet
  }

  // Flatten all-of hierarchy down such that all fields are flattened into a "bottom" Definition
  // "bottom" definitions would be ones that are TopLevel or are referenced as a target
  private def flattenMixins(
      defs: Vector[Definition],
      allTargets: Set[DefId]
  ): Vector[Definition] = {
    val defsById = defs.map(d => d.id -> d).toMap
    // get all parent fields, including parents of parents
    def getAllParentFieldsLoop(
        str: Structure,
        id: DefId,
        seen: Set[DefId] = Set.empty
    ): Vector[Field] = {
      // this means we are in a loop (cyclic mixin references)
      if (seen.contains(str.id)) {
        throw new IllegalArgumentException(
          "Detected cycle in mixins which is not allowed in the Smithy IDL"
        )
      } else {
        str.parents.flatMap(defsById.get).flatMap {
          case s: Structure =>
            s.localFields.map(f =>
              f.copy(id = f.id.copy(modelId = id))
            ) ++ getAllParentFieldsLoop(s, id, seen + str.id)
          case _ => Vector.empty
        }
      }
    }

    def moveParentFieldsDown(str: Structure): Structure = {
      val prnts = str.parents.flatMap(defsById.get)
      val parentFields = getAllParentFieldsLoop(str, str.id)
      // retain any parents that are top level OR are referenced as targets
      // these will be used to create mixins later and remove fields from "child"
      // structures as appropriate
      val topLevelParents =
        prnts
          .filter(p =>
            p.hints.contains(Hint.TopLevel) || allTargets.contains(p.id)
          )
          .map(_.id)
      // We are adding ALL parent fields here, the ones that should come from mixins will be
      // removed later on
      str.copy(
        localFields = str.localFields ++ parentFields,
        parents = topLevelParents
      )
    }

    defs.flatMap {
      case str: Structure =>
        val strIsReferenced = allTargets.contains(str.id)
        if (str.hints.contains(Hint.TopLevel) || strIsReferenced)
          Some(moveParentFieldsDown(str))
        else None
      case other => Some(other)
    }
  }

  private def process(defs: Vector[Definition]): Vector[Definition] = {
    val defsById = defs.map(d => d.id -> d).toMap
    val allTargets = getAllTargets(defs)
    def processStruct(str: Structure): Structure = {
      val parents = str.parents.flatMap(defsById.get)
      // Remove fields from children that are also defined on parents (these will be brought in by mixins)
      val newFields = str.localFields.flatMap { field =>
        val parentHasSameField = parents.exists {
          case s: Structure if s.localFields.contains(field) => true
          case _                                             => false
        }
        if (parentHasSameField) None else Some(field)
      }
      // Add mixin hints for parents
      val hasMixinHints = str.localFields.flatMap { field =>
        val newMixins = parents.flatMap {
          case s: Structure if s.localFields.contains(field) => Some(s.id)
          case _                                             => None
        }
        newMixins.map(Hint.HasMixin(_))
      }
      // Add Mixin hint if this particular structure is used as a mixin
      val isUsedAsMixin = defs.exists {
        case s: Structure if s.parents.contains(str.id) => true
        case _                                          => false
      }
      val isMixinHint = if (isUsedAsMixin) List(Hint.IsMixin) else List.empty
      str.copy(
        localFields = newFields,
        hints = str.hints ++ hasMixinHints ++ isMixinHint
      )
    }

    val flattened = flattenMixins(defs, allTargets)

    val intermediate = flattened.map {
      case s: Structure => processStruct(s)
      case other        => other
    }

    // Top level mixins are ones that need to exist as a Shape that can be referenced AND
    // as a mixin. Shapes that are top level OR that have references fall into this category
    def splitTopLevelMixins(str: Structure): List[Structure] = {
      val isTopLevel = str.hints.contains(Hint.TopLevel)
      val isMixin = str.hints.contains(Hint.IsMixin)
      val hasReferences = allTargets.contains(str.id)
      if (isTopLevel && isMixin && hasReferences) {
        val newMixinId =
          str.id.copy(name = str.id.name :+ Segment.Arbitrary(ci"Mixin"))
        List(
          // First, create a structure that is NOT a mixin, but references the new mixin we are creating
          str.copy(
            localFields = Vector.empty,
            hints = str.hints.filterNot(_ == Hint.IsMixin) :+ Hint.HasMixin(
              newMixinId
            )
          ),
          // Second, create the mixin with all of the fields
          str.copy(
            id = newMixinId,
            localFields = str.localFields
              .map(f => f.copy(id = f.id.copy(modelId = newMixinId)))
          )
        )
      } else List(str)
    }

    intermediate.flatMap {
      case s: Structure => splitTopLevelMixins(s)
      case other        => List(other)
    }
  }

  private def transform(in: IModel): Vector[Definition] = {
    val allShapes = in.definitions
    process(allShapes)
  }

  def apply(in: IModel): IModel = {
    IModel(transform(in), in.suppressions)
  }
}
