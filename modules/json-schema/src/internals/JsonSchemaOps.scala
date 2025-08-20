package smithytranslate.compiler
package internals
package json_schema

import cats.syntax.all._
import io.circe.Json
import org.typelevel.ci.CIString
import org.json.JSONObject
import smithytranslate.compiler.json_schema.CompilationUnit

private[compiler] object JsonSchemaOps {
  /**
   * Given some namespace path, and a json object representing a full json schema file,
   * create all of the compilation units associated with that schema.
   * 
   * @param namespace The namespace path for the schema
   * @param json The json object representing the schema
   * @return A vector of CompilationUnits, one for the root schema and one for each $defs and definitions entry
   */
  def createCompilationUnits(namespace: Path, json: Json): Vector[CompilationUnit] = {
    def $defUnits(name: String): Vector[CompilationUnit] = {
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

          CompilationUnit(namespace, defSchemaName, LoadSchema(new JSONObject(topLevelJson.noSpaces)), topLevelJson)
        }.toVector
    }
  
    val schema = LoadSchema(new JSONObject(json.noSpaces))
    val name = 
        Name(
          Segment.Derived(CIString(
            Option(schema.getTitle).getOrElse("input")
          ))
        )
    val rootUnit = CompilationUnit(namespace, name, schema, json)

    rootUnit +: ($defUnits("$defs") ++ $defUnits("definitions"))
  }
}
