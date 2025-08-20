package smithytranslate.compiler.json_schema

import io.circe.Json
import org.everit.json.schema.Schema
import smithytranslate.compiler.internals.Name
import smithytranslate.compiler.internals.Path

case class CompilationUnit(namespace: Path, name: Name, schema: Schema, json: Json)

