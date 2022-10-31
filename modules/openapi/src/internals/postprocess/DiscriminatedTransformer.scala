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

import UnionKind._

object DiscriminatedTransformer extends IModelPostProcessor {

  private case class UnionInfo(
      discriminator: String,
      altNames: Vector[Alternative]
  )

  private val getUnionInfo: PartialFunction[Definition, UnionInfo] = {
    case Union(_, altNames, Discriminated(field), _) =>
      UnionInfo(field, altNames)
  }

  private def updateDefinition(
      info: UnionInfo
  ): PartialFunction[Definition, Definition] = { case s: Structure =>
    val newFields =
      s.localFields.filter(_.id.memberName.value.toString != info.discriminator)
    s.copy(localFields = newFields)
  }

  private def doesContainDefinition(definition: Definition)(
      u: UnionInfo
  ): Boolean =
    u.altNames.exists(alt => definition.id == alt.tpe)

  def apply(in: IModel): IModel = {
    val unionInfo = in.definitions.flatMap(getUnionInfo.lift)
    val newDefs = in.definitions
      .map { definition =>
        unionInfo
          .find(doesContainDefinition(definition))
          .flatMap(updateDefinition(_).lift(definition))
          .getOrElse(definition)
      }
      .map {
        case Union(id, alts, d @ Discriminated(_), hints) =>
          val newAlts = alts.map { alt =>
            alt.copy(id = alt.id.copy(memberName = alt.tpe.name.segments.last))
          }
          Union(id, newAlts, d, hints)
        case other => other
      }
    IModel(newDefs, in.suppressions)
  }

}
