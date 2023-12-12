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

import io.swagger.v3.oas.models.headers.Header
import GetExtensions.HasExtensions

private[openapi] object HeadersToParams
    extends (Iterable[(String, Header)] => Vector[Param]) {

  def apply(
      headerMap: Iterable[(String, Header)]
  ): Vector[Param] = {
    Option(headerMap).toVector.flatten.map { case (name, header) =>
      val requiredHint =
        if (header.getRequired()) List(Hint.Required) else List.empty
      val exts = GetExtensions.from(HasExtensions.unsafeFrom(header))
      val hints = List(Hint.Header(name)) ++ requiredHint ++ exts
      val refOrSchema = Option(header.get$ref()) match {
        case Some(value) => Left(value)
        case None        => Right(header.getSchema())
      }
      Param(name, refOrSchema, hints)
    }.toVector
  }

}
