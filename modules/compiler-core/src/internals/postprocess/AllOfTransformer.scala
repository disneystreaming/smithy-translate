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

  private val DocumentPrimitive =
    DefId(Namespace(List("smithy", "api")), Name.stdLib("Document"))

  // get all parent fields, including parents of parents
  private def getAllParentFieldsLoop(
      str: Structure,
      id: DefId,
      defsById: Map[DefId, Definition],
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
          ) ++ getAllParentFieldsLoop(s, id, defsById, seen + str.id)
        case _ => Vector.empty
      }
    }
  }

  // Flatten all-of hierarchy down such that all fields are flattened into a "bottom" Definition
  // "bottom" definitions would be ones that are TopLevel or are referenced as a target
  private def flattenMixins(
      defs: Vector[Definition],
      defsById: Map[DefId, Definition],
      allTargets: Set[DefId]
  ): Vector[Definition] = {
    def moveParentFieldsDown(str: Structure): Structure = {
      val prnts = str.parents.flatMap(defsById.get)
      val parentFields = getAllParentFieldsLoop(str, str.id, defsById)
      // retain any parents that are top level OR are referenced as targets
      // these will be used to create mixins later and remove fields from "child"
      // structures as appropriate
      val topLevelParents =
        prnts
          .filter(p =>
            p.hints.contains(Hint.TopLevel) || allTargets.contains(p.id)
          )
          .map(_.id)
      val documentParents = prnts.collectFirst {
        case Newtype(_, DocumentPrimitive, _) => DocumentPrimitive
      }.toList
      // We are adding ALL parent fields here, the ones that should come from mixins will be
      // removed later on
      str.copy(
        localFields = str.localFields ++ parentFields,
        parents = topLevelParents ++ documentParents
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

  private def isTransitivelyADocument(
      s: Structure,
      defsById: Map[DefId, Definition]
  ): Boolean = {
    def loop(parentIds: Vector[DefId], soFar: Boolean): Boolean =
      soFar || (parentIds.nonEmpty && {
        val parents = parentIds.flatMap(defsById.get)
        val containsDocument = parents.exists {
          case Newtype(_, DocumentPrimitive, _) => true
          case _                                => false
        }
        val newParents = parents.flatMap {
          case p: Structure => p.parents
          case _            => Vector.empty
        }
        loop(newParents, containsDocument)
      })
    loop(s.parents, false)
  }

  private def toMixinId(d: DefId): DefId =
    d.copy(name = d.name :+ Segment.Arbitrary(ci"Mixin"))

  // Top level mixins are ones that need to exist as a Shape that can be referenced AND
  // as a mixin. Shapes that are top level OR that have references fall into this category
  private def splitTopLevelMixins(
      str: Structure,
      allTargets: Set[DefId]
  ): List[Structure] = {
    val isTopLevel = str.hints.contains(Hint.TopLevel)
    val isMixin = str.hints.contains(Hint.IsMixin)
    val hasReferences = allTargets.contains(str.id)
    if (isTopLevel && isMixin && hasReferences) {
      val newMixinId =
        toMixinId(str.id)
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

  private def process(defs: Vector[Definition]): Vector[Definition] = {
    val defsById = defs.map(d => d.id -> d).toMap
    val allTargets = util.getAllTargets(defs)
    def processStruct(str: Structure): Structure = {
      val parents = str.parents.flatMap(defsById.get)
      // Remove fields from children that are also defined on parents (these will be brought in by mixins)
      val allParentFields = getAllParentFieldsLoop(str, str.id, defsById)
      val newFields = str.localFields.flatMap { field =>
        val parentHasSameField =
          allParentFields.exists(
            _.id.name.segments.last == field.id.name.segments.last
          )
        if (parentHasSameField) None else Some(field)
      }
      // Add mixin hints for parents
      val hasMixinHints = str.localFields.flatMap { field =>
        val newMixins = parents.flatMap {
          case parent: Structure =>
            val localParentHasField = parent.localFields.exists(
              _.id.name.segments.last == field.id.name.segments.last
            )
            val parentParents =
              getAllParentFieldsLoop(parent, parent.id, defsById)
            val parentParentsHaveField =
              parentParents.exists(
                _.id.name.segments.last == field.id.name.segments.last
              )
            if (localParentHasField || parentParentsHaveField)
              Some(parent.id)
            else None
          case _ => None
        }
        newMixins.map(Hint.HasMixin(_))
      }
      // Add Mixin hint if this particular structure is used as a mixin
      val isUsedAsMixin = defs.exists {
        case s: Structure
            // if s is a document transitively, then it is NOT a reason to treat this
            // as a mixin because s will be rendered as a document shape and thus will not
            // use this as a mixin in any case.
            if s.parents
              .contains(str.id) && !isTransitivelyADocument(s, defsById) =>
          true
        case _ => false
      }
      val isMixinHint = if (isUsedAsMixin) List(Hint.IsMixin) else List.empty
      str.copy(
        localFields = newFields,
        hints = str.hints ++ hasMixinHints ++ isMixinHint
      )
    }

    val flattened = flattenMixins(defs, defsById, allTargets)

    val intermediate = flattened.map {
      case s: Structure => processStruct(s)
      case other        => other
    }

    val (splitIds, result) = intermediate.map {
      case s: Structure =>
        val split = splitTopLevelMixins(s, allTargets)
        if (split.size > 1) Some(s.id) -> split else None -> split
      case other => None -> List(other)
    }.unzip

    // Need to remap mixin references for any cases where splitting happened in the previous step
    val mixinRemappings: Map[DefId, DefId] =
      splitIds.flatten.map(id => id -> toMixinId(id)).toMap

    val resultWithRemappedMixins = result.flatten.map(_.mapHints(_.map {
      case Hint.HasMixin(mixinId) =>
        Hint.HasMixin(mixinRemappings.getOrElse(mixinId, mixinId))
      case other => other
    }))

    resultWithRemappedMixins.map {
      case s: Structure =>
        val isDocument = isTransitivelyADocument(s, defsById)
        if (isDocument) {
          Newtype(
            s.id,
            DocumentPrimitive,
            s.hints.filterNot(h =>
              h.isInstanceOf[Hint.HasMixin] || h == Hint.IsMixin
            )
          )
        } else s
      case other => other
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
