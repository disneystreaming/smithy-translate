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
package json_schema

import org.everit.json.schema.Schema
import org.json.JSONObject
import org.everit.json.schema.loader.SchemaLoader

private[compiler] object LoadSchema extends (JSONObject => Schema) {
  def apply(sch: JSONObject): Schema =
    SchemaLoader
      .builder()
      .draftV7Support()
      .addFormatValidator(new EmptyValidator("uuid"))
      .addFormatValidator(new EmptyValidator("date"))
      .addFormatValidator(new EmptyValidator("date-time"))
      .addFormatValidator(new EmptyValidator("local-date"))
      .addFormatValidator(new EmptyValidator("local-time"))
      .addFormatValidator(new EmptyValidator("local-date-time"))
      .addFormatValidator(new EmptyValidator("offset-date-time"))
      .addFormatValidator(new EmptyValidator("offset-time"))
      .addFormatValidator(new EmptyValidator("zone-id"))
      .addFormatValidator(new EmptyValidator("zone-offset"))
      .addFormatValidator(new EmptyValidator("zoned-date-time"))
      .addFormatValidator(new EmptyValidator("year"))
      .addFormatValidator(new EmptyValidator("year-month"))
      .addFormatValidator(new EmptyValidator("month-day"))
      .useDefaults(true)
      .schemaJson(sch)
      .build()
      .load()
      .build()
}
