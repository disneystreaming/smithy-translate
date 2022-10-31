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
import smithytranslate.openapi.internals.Hint
import scala.annotation.tailrec

/** Removes "NewType" definitions by dereferencing them all in structures/union
  * definitions (could probably be do with more shapes)
  */
object NewtypeTransformer extends IModelPostProcessor {
  private def shouldRemainNewtype(hints: List[Hint]): Boolean = hints.exists {
    case _: Hint.Length           => true
    case _: Hint.Range            => true
    case _: Hint.Pattern          => true
    case _: Hint.OpenApiExtension => true
    case Hint.Sensitive           => true
    case _: Hint.Timestamp        => true
    case Hint.TopLevel            => true
    case _                        => false
  }
  private case class DerefResult(id: DefId, hints: List[Hint])
  def apply(model: IModel): IModel = {
    val newtypes = model.definitions.collect {
      case Newtype(id, target, hints) =>
        id -> (target, hints)
    }.toMap

    @tailrec
    def dereference(
        id: DefId,
        hintsSoFar: List[Hint] = Nil
    ): DerefResult =
      newtypes.get(id) match {
        case Some((underlying, ntHints))
            if !ntHints.contains(Hint.TopLevel) && id != underlying =>
          dereference(underlying, hintsSoFar ++ ntHints)
        case _ =>
          DerefResult(id, hintsSoFar)
      }

    def createOne(d: Definition) = (Nil, Vector(d))
    def createNone = (Nil, Vector.empty[Definition])

    // Dereferencing newtypes in structures and unions
    val (removeDefs, newDefs) = model.definitions.map {
      case n @ Newtype(_, _, h) if shouldRemainNewtype(h) => createOne(n)
      case _: Newtype                                     => createNone
      case Structure(id, localFields, parents, hints) =>
        val (remove, newFields) = localFields.map {
          case Field(id, tpe, hints) =>
            val DerefResult(t, h) = dereference(tpe)
            val maybeRemove = if (t != tpe && h.nonEmpty) Some(tpe) else None
            (maybeRemove, Field(id, t, hints ++ h))
        }.unzip
        val newParents = parents.map(dereference(_).id)
        (remove.flatten, Vector(Structure(id, newFields, newParents, hints)))
      case Union(id, alts, kind, hints) =>
        val newAlts = alts.map { case Alternative(id, tpe, hints) =>
          Alternative(id, dereference(tpe).id, hints)
        }
        createOne(Union(id, newAlts, kind, hints))
      case ListDef(id, member, hints) =>
        createOne(ListDef(id, dereference(member).id, hints))
      case SetDef(id, member, hints) =>
        createOne(SetDef(id, dereference(member).id, hints))
      case MapDef(id, key, value, hints) =>
        createOne(MapDef(id, dereference(key).id, dereference(value).id, hints))
      case other => createOne(other)
    }.unzip
    val remove = removeDefs.flatten
    val finalDefs = newDefs.flatten.filterNot(d => remove.contains(d.id))
    IModel(finalDefs, model.suppressions)
  }
}
