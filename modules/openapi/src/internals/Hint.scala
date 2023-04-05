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

import software.amazon.smithy.model.node.Node
// format: off
sealed abstract class Hint extends Product with Serializable
object Hint {
  case class Header(name: String) extends Hint
  case class Length(min: Option[Long], max: Option[Long]) extends Hint
  case class Range(min: Option[BigDecimal], max: Option[BigDecimal]) extends Hint
  case class Pattern(value: String) extends Hint
  case class PathParam(name: String) extends Hint
  case class QueryParam(name: String) extends Hint
  case object Body extends Hint
  case object Required extends Hint
  case object Sensitive extends Hint
  case class Timestamp(format: TimestampFormat) extends Hint
  case class Description(value: String) extends Hint
  case class HttpCode(code: Int) extends Hint
  case class Error(code: Int, isServerError: Boolean) extends Hint
  case class Http(method: HttpMethod, path: String, code: Option[Int]) extends Hint
  case class ErrorMessage(message: String) extends Hint
  case class OpenApiExtension(values: Map[String, Node]) extends Hint
  case object TopLevel extends Hint
  case class ContentTypeLabel(value: String) extends Hint
  case class Security(schemes: Vector[SecurityScheme]) extends Hint
  case class Auth(schemes: Vector[SecurityScheme]) extends Hint
  case class DefaultValue(node: Node) extends Hint
  case object UniqueItems extends Hint
  case object Nullable extends Hint
  case class CurrentLocation(str: String) extends Hint
  case class TargetLocation(str: String) extends Hint
  case class JsonName(name: String) extends Hint
  case class Examples(value: List[Node]) extends Hint
}
// format: on
