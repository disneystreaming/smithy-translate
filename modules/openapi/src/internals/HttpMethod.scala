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
sealed abstract class HttpMethod extends Product with Serializable
object HttpMethod {
  case object GET extends HttpMethod
  case object POST extends HttpMethod
  case object PUT extends HttpMethod
  case object PATCH extends HttpMethod
  case object DELETE extends HttpMethod
  case object HEAD extends HttpMethod
  case object OPTIONS extends HttpMethod
  case object TRACE extends HttpMethod
  val all: List[HttpMethod] =
    List(GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS, TRACE)
}
