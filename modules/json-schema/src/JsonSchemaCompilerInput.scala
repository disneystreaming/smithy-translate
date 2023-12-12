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

package smithytranslate.compiler.json_schema

import cats.data.NonEmptyList
import org.everit.json.schema.Schema
import io.circe.Json
import smithytranslate.compiler.FileContents

sealed trait JsonSchemaCompilerInput

object JsonSchemaCompilerInput {
  final case class UnparsedSpecs(specs: List[FileContents])
      extends JsonSchemaCompilerInput

  final case class ParsedSpec(
      path: NonEmptyList[String],
      rawJson: Json,
      schema: Schema
  ) extends JsonSchemaCompilerInput

}
