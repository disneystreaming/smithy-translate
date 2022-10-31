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

object FixMissingTargetsTransformer extends IModelPostProcessor {

  def apply(model: IModel): IModel = {
    val allDefIds = model.definitions.map(_.id).toSet
    val types = model.definitions.collect {
      case Structure(_, localFields, _, _) => localFields.map(_.tpe)
    }.flatten
    val nonExistentTargets = types.filterNot(n =>
      allDefIds(n) || n.namespace.segments == List("smithy", "api")
    )

    def getCurrentLocationHint(
        hints: List[Hint]
    ): Option[Hint.CurrentLocation] =
      hints.collectFirst { case c: Hint.CurrentLocation =>
        c
      }

    val locationToName: Map[Hint.CurrentLocation, DefId] =
      model.definitions.flatMap {
        case Structure(id, localFields, _, hints) =>
          val res = localFields.flatMap(f =>
            getCurrentLocationHint(f.hints).map(_ -> f.tpe)
          ) ++ getCurrentLocationHint(hints).map(_ -> id)
          res
        case other =>
          getCurrentLocationHint(other.hints).map(_ -> other.id).toList
      }.toMap

    val updateTargetsTo = nonExistentTargets.flatMap { defId =>
      val h = Hint.CurrentLocation(
        (defId.name.segments.toChain.toList
          .map(_.value.toString))
          .mkString("#/", "/", "")
      )
      locationToName.get(h).map(defId -> _)
    }.toMap
    val newDefs = model.definitions.map {
      case s: Structure =>
        s.copy(localFields =
          s.localFields.map(f =>
            f.copy(tpe = updateTargetsTo.get(f.tpe).getOrElse(f.tpe))
          )
        )
      case other => other
    }
    model.copy(definitions = newDefs)
  }

}
