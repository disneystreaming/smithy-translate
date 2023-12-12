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

private[compiler] sealed abstract class TimestampFormat
    extends Product
    with Serializable

private[compiler] object TimestampFormat {
  case object DateTime extends TimestampFormat // e.g. 2017-07-21T17:32:28Z
  case object SimpleDate extends TimestampFormat // e.g. 2017-07-21
}
