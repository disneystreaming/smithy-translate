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

package smithytranslate.compiler
package internals
package openapi

import smithytranslate.compiler.internals._

private[openapi] case class HttpMessageInfo(
    name: Name,
    params: Vector[Param],
    hints: List[Hint]
) {
  def mapHints(f: List[Hint] => List[Hint]) = this.copy(hints = f(hints))
}
