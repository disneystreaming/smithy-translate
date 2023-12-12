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

import cats.data.NonEmptyChain
import io.swagger.v3.oas.models.media.Schema

private[openapi] case class Local(
    context: Context,
    schema: Schema[_]
) {
  def mapPath(f: Name => Name): Local =
    copy(context = context.copy(path = f(context.path)))

  def addHints(newHints: List[Hint]): Local =
    copy(context = context.copy(hints = newHints ++ context.hints))

  def addHints(newHints: Hint*): Local =
    this.addHints(newHints.toList)

}

private[openapi] object Local {
  def apply(path: Name, schema: Schema[_]): Local =
    Local(Context(path, Nil, Nil), schema)

  def apply(path: Segment, schema: Schema[_]): Local =
    Local(Context(Name(NonEmptyChain.of(path)), Nil, Nil), schema)
}
