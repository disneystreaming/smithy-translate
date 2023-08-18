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

object TaggedUnionTransformer extends IModelPostProcessor {

  private case class TaggedUnionInfo(
      taggedFields: Vector[Field],
      isTagged: Boolean
  ) {
    def addFields(fs: Vector[Field]): TaggedUnionInfo =
      copy(taggedFields = taggedFields ++ fs)
    def setNotTagged = copy(isTagged = false)
  }
  private object TaggedUnionInfo {
    def empty: TaggedUnionInfo = TaggedUnionInfo(Vector.empty, isTagged = true)
  }

  private def getFieldsFromAllParentsTransitively(
      target: Definition,
      allDefinitions: Vector[Definition]
  ): Vector[Field] = {
    target match {
      case s: Structure =>
        val allParents = allDefinitions.filter(d => s.parents.contains(d.id))
        allParents.flatMap { parent =>
          getFieldsFromAllParentsTransitively(
            parent,
            allDefinitions
          )
        } ++ s.localFields
      case _ => Vector.empty
    }
  }

  private def getInfoForTargets(
      targets: Vector[Definition],
      allDefinitions: Vector[Definition]
  ): TaggedUnionInfo = {
    targets.foldLeft(TaggedUnionInfo.empty) { (info, t) =>
      val allFields =
        getFieldsFromAllParentsTransitively(t, allDefinitions)
      val singleLocal = allFields.size == 1
      val allLocalRequired =
        allFields.forall(_.hints.contains(Hint.Required))
      val noParents = true // parents.isEmpty
      if (singleLocal && allLocalRequired && noParents)
        info.addFields(allFields)
      else info.setNotTagged
    }
  }

  private def getTaggedUnionInfo(
      alts: Vector[Alternative],
      allShapes: Vector[Definition]
  ): TaggedUnionInfo = {
    val altTargetDefinitions =
      allShapes.filter(definition => alts.exists(_.tpe == definition.id))
    getInfoForTargets(altTargetDefinitions, allShapes)
  }

  private def transform(in: IModel): Vector[Definition] = {
    val toUpdate =
      in.definitions.flatMap {
        case u @ Union(id, altNames, UnionKind.Untagged, _) =>
          val info = getTaggedUnionInfo(altNames, in.definitions)
          val taggedFields = info.taggedFields
          val allFieldNamesUnique = taggedFields
            .distinctBy(_.id.name.segments.last.value)
            .size == taggedFields.size
          if (info.isTagged && taggedFields.nonEmpty && allFieldNamesUnique) {
            val newUnion = u.copy(
              kind = UnionKind.Tagged,
              alts = taggedFields.map(f =>
                Alternative(
                  MemberId(id, f.id.memberName),
                  f.tpe,
                  Nil
                )
              )
            )
            Set(newUnion)
          } else {
            val newUnion = u.copy(
              alts = u.alts.map(alt =>
                alt.copy(id =
                  alt.id.copy(memberName = alt.tpe.name.segments.last)
                )
              )
            )
            Set(newUnion)
          }
        case _ => Set.empty
      }
    val toAddMap = toUpdate.map(d => d.id -> d).toMap
    in.definitions
      .map(d => toAddMap.getOrElse(d.id, d))
  }

  def apply(in: IModel): IModel = {
    IModel(transform(in), in.suppressions)
  }
}
