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

package smithytranslate.compiler.internals.postprocess

import smithytranslate.compiler.internals._

private[postprocess] object util {
  // returns all Ids that are ever referenced as a target
  def getAllTargets(
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
}
