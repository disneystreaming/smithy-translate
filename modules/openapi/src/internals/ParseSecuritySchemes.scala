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

import io.swagger.v3.oas.models.OpenAPI
import scala.jdk.CollectionConverters._
import io.swagger.v3.oas.models.security.SecurityScheme.Type._
import io.swagger.v3.oas.models.security.SecurityScheme.In._
import cats.syntax.all._

private[openapi] object ParseSecuritySchemes
    extends (OpenAPI => (List[ToSmithyError], SecuritySchemes)) {
  def apply(o: OpenAPI): (List[ToSmithyError], SecuritySchemes) =
    Option(o.getComponents())
      .flatMap(c => Option(c.getSecuritySchemes()).map(_.asScala))
      .getOrElse(Map.empty)
      .toList
      .map { case (name, ss) =>
        ss.getType() match {
          case APIKEY =>
            ss.getIn() match {
              case COOKIE =>
                error(
                  ToSmithyError.Restriction(
                    "Cookie is not a supported ApiKey location."
                  )
                )
              case HEADER =>
                success(
                  name -> SecurityScheme
                    .ApiKey(ApiKeyLocation.Header, ss.getName())
                )
              case QUERY =>
                success(
                  name -> SecurityScheme
                    .ApiKey(ApiKeyLocation.Query, ss.getName())
                )
            }
          case HTTP =>
            Option(ss.getScheme).map(_.toLowerCase) match {
              case Some("bearer") =>
                success(
                  name -> SecurityScheme.BearerAuth(
                    Option(ss.getBearerFormat())
                  )
                )
              case Some("basic") =>
                success(name -> SecurityScheme.BasicAuth)
              case format =>
                error(
                  ToSmithyError.Restriction(
                    s"$format is not a supported format of the http security scheme."
                  )
                )
            }
          case OAUTH2 =>
            error(
              ToSmithyError.Restriction(
                "OAuth2 is not a supported security scheme."
              )
            )
          case OPENIDCONNECT =>
            error(
              ToSmithyError.Restriction(
                "OpenIdConnect is not a supported security scheme."
              )
            )
          case MUTUALTLS =>
            error(
              ToSmithyError.Restriction(
                "MutualTLS is not a supported security scheme."
              )
            )
        }
      }
      .unzip
      .bimap(_.flatten, _.flatten.toMap)

  private def success(
      in: (String, SecurityScheme)
  ): (List[ToSmithyError], Option[(String, SecurityScheme)]) =
    (Nil, Some(in))

  private def error(
      modelError: ToSmithyError
  ): (List[ToSmithyError], Option[(String, SecurityScheme)]) =
    (List(modelError), None)
}
