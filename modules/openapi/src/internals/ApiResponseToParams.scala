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

import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import smithytranslate.openapi.internals.GetExtensions.HasExtensions

import scala.jdk.CollectionConverters._

object ApiResponseToParams
    extends ((Name, ApiResponse) => RefOr[HttpMessageInfo]) {

  def apply(
      opName: Name,
      response: ApiResponse
  ): RefOr[HttpMessageInfo] = {
    val maybeRef = Option(response.get$ref())

    val maybeBodyParam: Option[Param] = {
      val bodies = ContentToSchemaOpt(
        response.getContent()
      ).toList
      bodies.toList match {
        case Nil                                         => None
        case (contentType, bodySchema: Schema[_]) :: Nil =>
          // TODO: figure out how to deal with reflective calls in scala 2.12
          val bodyExts =
            GetExtensions.from(bodySchema.asInstanceOf[HasExtensions])
          Some(
            Param(
              "body",
              Right(bodySchema),
              List(
                Hint.Required,
                Hint.Body,
                Hint.ContentTypeLabel(contentType)
              ) ++ bodyExts
            )
          )
        case list =>
          val bodySchema = ContentTypeDiscriminatedSchema(list.toMap)
          val bodyExts =
            GetExtensions.from(bodySchema.asInstanceOf[HasExtensions])
          Some(
            Param(
              "body",
              Right(bodySchema),
              List(Hint.Required, Hint.Body) ++ bodyExts
            )
          )
      }
    }

    val headers = response.getHeaders().asScala
    val headerParams: Vector[Param] = HeadersToParams(headers)
    val allParams = maybeBodyParam.toVector ++ headerParams
    maybeRef match {
      case Some(ref) => Left(ref)
      case None =>
        val exts = GetExtensions.from(response.asInstanceOf[HasExtensions])
        val httpMessageInfo = HttpMessageInfo(opName, allParams, exts)
        Right(httpMessageInfo)
    }
  }

}
