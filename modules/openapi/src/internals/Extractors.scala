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

import smithytranslate.compiler._
import smithytranslate.compiler.internals._
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media._
import scala.jdk.CollectionConverters._

private[openapi] object CaseEnum {
  def unapply(sch: Schema[_]): Option[Vector[String]] = sch match {
    case s: StringSchema if s.getEnum() != null && !s.getEnum().isEmpty =>
      Some(s.getEnum().asScala.toVector)
    case _ => None
  }
}

private[openapi] object CaseMap {
  def unapply(sch: Schema[_]): Option[Schema[_]] = {
    sch match {
      case m: MapSchema =>
        Option(m.getAdditionalProperties()).collect { case s: Schema[_] =>
          s
        }
      case _ => None
    }
  }
}

private[openapi] object IsFreeForm {
  def unapply(sch: Schema[_]): Boolean = {
    val isObjectSchema = sch.isInstanceOf[ObjectSchema]
    val hasNoProperties = Option(sch.getProperties()).forall(_.isEmpty())
    (Option(sch.getAdditionalProperties())
      .map {
        case b: java.lang.Boolean => b.booleanValue()
        case _                    => CaseMap.unapply(sch).isEmpty
      })
      .getOrElse(isObjectSchema && hasNoProperties)
  }
}

// Format, XFormat, NoFormat and & are retained for binary compatibility.
// Primitive conversion uses SchemaReader.
@deprecated("Use SchemaReader instead.", since = "0.7.9")
private[openapi] object Format {
  def unapply(sch: Schema[_]): Option[String] = Option(sch.getFormat())
}

@deprecated("Use SchemaReader instead.", since = "0.7.9")
private[openapi] object XFormat {
  def unapply(sch: Schema[_]): Option[String] =
    Option(sch.getExtensions().asScala)
      .flatMap(_.get("x-format"))
      .map(_.toString())
}

@deprecated("Use SchemaReader instead.", since = "0.7.9")
private[openapi] object NoFormat {
  def unapply(sch: Schema[_]): Boolean = Option(sch.getFormat()).isEmpty
}

@deprecated("Use SchemaReader instead.", since = "0.7.9")
private[openapi] object & {
  def unapply[A](a: A): Some[(A, A)] = Some((a, a))
}

private[openapi] object CaseAllOf {
  def unapply(sch: Schema[_]): Option[Vector[Schema[_]]] = sch match {
    case composed: ComposedSchema =>
      Option(composed.getAllOf()).map(_.asScala.toVector)
    case _ => None
  }
}

private[openapi] object CaseOneOf {
  def unapply(sch: Schema[_]): Option[Vector[Schema[_]]] = sch match {
    case composed: ComposedSchema =>
      Option(composed.getOneOf()).map(_.asScala.toVector)
    case _ => None
  }
}

private[openapi] object CaseObject {
  def unapply(sch: Schema[_]): Option[Schema[_]] = sch match {
    case o: ObjectSchema => Some(o)
    case s: Schema[_] if Option(s.getProperties()).exists(_.asScala.nonEmpty) =>
      Some(s)
    case _ => None
  }
}

private[openapi] object NonEmptySegments {
  def unapply(s: String): Option[(List[String], String)] = {
    val segments = s.split("/").toList.filterNot(_.isEmpty())
    segments.lastOption.map(last => segments.dropRight(1) -> last)
  }
}

// Retained for binary compatibility; conversion uses SchemaReader.
@deprecated("Use SchemaReader instead.", since = "0.7.9")
private[openapi] object CasePrimitive {
  private val reader = new SchemaReader(new OpenAPI().openapi("3.0.3"))
  def unapply(sch: Schema[_]): Option[Primitive] = reader.unapply(sch)
}

/*
 * The most complicated thing
 */
private[compiler] abstract class CaseRefBuilder(ns: Path)
    extends smithytranslate.compiler.internals.RefParser(ns) {
  def unapply(sch: Schema[_]): Option[Either[ToSmithyError, ParsedRef]] =
    Option(sch.get$ref).map(this.apply)
}
