/* Copyright 2025 Disney Streaming
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
package smithytranslate.compiler
package internals

import cats.data.Chain
import cats.data.NonEmptyChain

final class NamespaceRemapper(
    remaps: Map[NonEmptyChain[String], Chain[String]]
) {
  final def remap(ns: List[String]): List[String] = {
    remaps
      .collectFirst {
        case (key, value) if ns.startsWith(key.toChain.toList) =>
          (value ++ Chain.fromSeq(ns.drop(key.length.toInt))).toList
      }
      .getOrElse(ns)
  }

  final def remap(defId: DefId): DefId = {
    defId.copy(namespace = Namespace(remap(defId.namespace.segments)))
  }

}
