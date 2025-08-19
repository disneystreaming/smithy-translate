package smithytranslate.compiler.json_schema

import io.circe.Json
import cats.data.NonEmptyChain
import org.everit.json.schema.Schema

case class CompilationUnit(namespace: NonEmptyChain[String], schema: Schema, json: Json)
