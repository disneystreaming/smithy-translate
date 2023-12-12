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

import software.amazon.smithy.model.node.Node
import cats.syntax.all._
import cats.kernel.Semigroup

private[compiler] object ExtensionsMerger extends IModelPostProcessor {

  def apply(model: IModel): IModel =
    IModel(model.definitions.map(process), model.suppressions)

  private def process(d: Definition) = d.mapHints(mergeHints)

  private def mergeHints(hints: List[Hint]): List[Hint] = {
    implicit val nodeSemigroup: Semigroup[Node] =
      Semigroup.instance[Node]((_, right) => right)
    val (exts, remaining) = hints.foldMap[(Map[String, Node], List[Hint])] {
      case (Hint.OpenApiExtension(m)) => (m, List.empty)
      case other                      => (Map.empty, List(other))
    }
    Hint.OpenApiExtension(exts) :: remaining
  }

}
