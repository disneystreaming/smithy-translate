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
import scala.jdk.CollectionConverters._
import io.swagger.v3.oas.models.media.Content
import scala.collection.compat._

object ContentToSchemaOpt extends (Content => Map[String, Schema[_]]) {

  def apply(c: Content): Map[String, Schema[_]] =
    Option(c)
      .map(
        _.asScala.view.mapValues(_.getSchema()).toMap
      )
      .getOrElse(Map.empty)
}
