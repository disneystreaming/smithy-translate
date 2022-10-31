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

import smithytranslate.openapi.ModelError

case class Context(path: Name, hints: List[Hint], errors: List[ModelError]) {
  def append(name: Name): Context =
    this.copy(
      path = path ++ name,
      hints = List.empty
    )
  def append(segment: Segment): Context =
    this.copy(
      path = path :+ segment,
      hints = List.empty
    )
  def addHints(hints: List[Hint], retainTopLevel: Boolean = false): Context =
    this.copy(hints =
      this.hints.filter(_ != Hint.TopLevel || retainTopLevel) ++ hints
    )
  def addHints(hints: Hint*): Context =
    this.addHints(hints.toList)
  def addErrors(errors: List[ModelError]): Context =
    this.copy(errors = this.errors ++ errors)
  def removeTopLevel(): Context =
    this.copy(hints = this.hints.filter(_ != Hint.TopLevel))
}
