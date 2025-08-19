package smithytranslate.compiler
package internals
package json_schema

import cats.syntax.all._
import io.circe.Json
import org.typelevel.ci.CIString
import org.everit.json.schema.Schema
import io.circe.JsonObject
import org.json.JSONObject

private[compiler] object JsonSchemaOps {

  def extractDefs(json: Json): Vector[(Name, Schema, Json)] = {
    def $defSchemas(name: String): Vector[(Name, Schema, Json)] = {
      val defsObject = json.asObject
        .flatMap(_.apply(name))
        .flatMap(_.asObject)

      val allDefs = defsObject.toVector
        .flatMap(_.toVector)
        .flatMap(_.traverse(_.asObject).toVector)

      allDefs
        .map { case (key, value) =>
          val topLevelJson = Json.fromJsonObject {
            value.add(name, Json.fromJsonObject(defsObject.get))
          }

          val defSchemaName =
            Name(
              Segment.Arbitrary(CIString(name)),
              Segment.Derived(CIString(key))
            )

          (defSchemaName, LoadSchema(new JSONObject(topLevelJson.noSpaces)), topLevelJson)
        }.toVector
    }
  
    $defSchemas("$defs") ++ $defSchemas("definitions")
  }
}
