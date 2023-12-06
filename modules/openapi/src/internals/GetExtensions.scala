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

import software.amazon.smithy.model.node.Node
import scala.jdk.CollectionConverters._
import scala.language.reflectiveCalls
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.{node => jackson}
import java.time.format.DateTimeFormatter
import java.time.ZoneId

object GetExtensions {

  def transformPattern[A](
      local: Local
  ): OpenApiPattern[A] => OpenApiPattern[A] = {
    val maybeHints = from(HasExtensions.unsafeFrom(local.schema))
    (pattern: OpenApiPattern[A]) =>
      pattern.mapContext(_.addHints(maybeHints, retainTopLevel = true))
  }

  // Using reflective calls because openapi does not seem to have a common interface
  // that exposes the presence of extensions.
  type HasExtensions = { def getExtensions(): java.util.Map[String, Any] }
  object HasExtensions {
    def unsafeFrom(s: Any): HasExtensions = s.asInstanceOf[HasExtensions]
  }

  def from(s: HasExtensions): List[Hint] =
    Option(s)
      .flatMap(s => Option(s.getExtensions()))
      .map(_.asScala)
      .filterNot(_.isEmpty)
      .map[Hint] { ext =>
        val nodeMap = ext.map { case (k, v) =>
          (k, anyToNode(v))
        }
        Hint.OpenApiExtension(nodeMap.toMap)
      }
      .toList

  private val formatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("UTC"))

  protected[smithytranslate] def anyToNode(input: Any): Node = input match {
    case null                        => Node.nullNode()
    case b: Boolean                  => Node.from(b)
    case s: String                   => Node.from(s)
    case i: Int                      => Node.from(i)
    case d: Double                   => Node.from(d)
    case s: Short                    => Node.from(s)
    case l: Long                     => Node.from(l)
    case f: Float                    => Node.from(f)
    case n: Number                   => Node.from(n)
    case d: java.time.OffsetDateTime => Node.from(d.toString())
    case d: java.util.Date => Node.from(formatter.format(d.toInstant()))
    case u: java.util.UUID =>
      Node.from(u.toString)
    case m: java.util.Map[_, _] =>
      Node.objectNode {
        m.asScala.collect { case (k: String, v: Any) =>
          (Node.from(k), anyToNode(v))
        }.asJava
      }
    case c: java.util.Collection[_] =>
      Node.fromNodes(c.asScala.map(anyToNode).toList.asJava)
    case j: JsonNode => jacksonToSmithy(j)
    case _ => Node.nullNode() // if nothing is found, to prevent match errors
  }

  private def jacksonToSmithy(jn: JsonNode): Node = jn match {
    case s: jackson.TextNode    => Node.from(s.textValue)
    case b: jackson.BooleanNode => Node.from(b.booleanValue)
    case n: jackson.NumericNode => Node.from(n.numberValue)
    case _: jackson.NullNode    => Node.nullNode()
    case a: jackson.ArrayNode =>
      Node.fromNodes(a.elements().asScala.toList.map(jacksonToSmithy).asJava)
    case o: jackson.ObjectNode =>
      Node.objectNode {
        o.fields()
          .asScala
          .map { entry =>
            (Node.from(entry.getKey), jacksonToSmithy(entry.getValue))
          }
          .toMap
          .asJava
      }
  }

}
