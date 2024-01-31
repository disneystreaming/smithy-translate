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

package smithytranslate.proto3.internals

import software.amazon.smithy.model.shapes.ShapeId
import ProtoIR.Fqn

private[internals] object Namespacing {
  def shapeIdToFqn(id: ShapeId): Fqn =
    Fqn(Some(namespaceToPackage(id.getNamespace)), id.getName)

  def shapeIdToImportFqn(id: ShapeId): Fqn =
    namespaceToFqn(id.getNamespace())

  private def namespaceToPackage(namespace: String): List[String] =
    namespace.split("\\.").toList

  def namespaceToFqn(ns: String): Fqn = {
    val parts = ns.split("\\.").toList
    if (parts.size == 0) {
      Fqn(None, "definitions") // should not happen
    } else if (parts.size == 1) {
      Fqn(Some(parts), "definitions")
    } else {
      // The reason for using the last directory as the filename as well is
      // that the filename is used by several of the proto code generators
      val last = parts.last
      Fqn(Some(parts), last)
    }
  }
}
