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

package smithytranslate.json_schema.internals

import cats.data.NonEmptyChain
import smithytranslate.openapi.internals.{Local => _, _}
import org.everit.json.schema.Schema
import io.circe.Json

case class Local(
    context: Context,
    schema: Schema,
    topLevelJson: Json
) {

  def mapPath(f: Name => Name): Local =
    copy(context = context.copy(path = f(context.path)))

  def addHints(newHints: List[Hint]): Local =
    copy(context = context.copy(hints = newHints ++ context.hints))

  def addHints(newHints: Hint*): Local =
    this.addHints(newHints.toList)

  def filterHints(f: Hint => Boolean): Local =
    copy(context = context.copy(hints = context.hints.filter(f)))

  def down(segment: Segment, subSchema: Schema): Local =
    new Local(context.append(segment), subSchema, topLevelJson)

  def down(name: Name, subSchema: Schema): Local =
    new Local(context.append(name), subSchema, topLevelJson)

}

object Local {
  def apply(path: Name, schema: Schema, topLevelJson: Json): Local =
    Local(Context(path, Nil, Nil), schema, topLevelJson)

  def apply(path: Segment, schema: Schema, topLevelJson: Json): Local =
    Local(Context(Name(NonEmptyChain.of(path)), Nil, Nil), schema, topLevelJson)
}
