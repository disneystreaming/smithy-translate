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

package smithytranslate

import java.util.ServiceLoader

import scala.io.Source
import scala.jdk.CollectionConverters._

import software.amazon.smithy.model.Model
import software.amazon.smithy.model.traits.{ Trait, TraitDefinition, TraitService }
import software.amazon.smithy.model.shapes.ShapeId

final class TraitProviderSpec extends munit.FunSuite {

  val Namespace = "smithytranslate"

  test("trait providers are defined and registered") {
    val model = Model.assembler().discoverModels().assemble().unwrap()

    val traits = model
      .shapes()
      .iterator()
      .asScala
      .filter(s => s.getId().getNamespace() == Namespace)
      .filter(_.hasTrait(classOf[TraitDefinition]))
      .map(_.getId())
      .toSet

    val providers = ServiceLoader
      .load(classOf[TraitService])
      .asScala
      .filter(s => s.getShapeId().getNamespace() == Namespace)
      .map(_.getShapeId())
      .toSet

    assertEquals(providers, traits)
  }

  test("trait providers are working") {
    val spec =
      """|$version: "2"
         |
         |namespace test
         |
         |use smithytranslate#const
         |use smithytranslate#contentType
         |use smithytranslate#contentTypeDiscriminated
         |use smithytranslate#errorMessage
         |
         |structure Foo {
         |  @errorMessage("error")
         |  s: String
         |  @contentType("application/json")
         |  @httpPayload
         |  content: Content
         |  @const({a: 1})
         |  constant: Document
         |}
         |
         |structure Content {}
         |
         |@contentTypeDiscriminated
         |union Bar {
         |  @contentType("application/octet-stream")
         |  applicationOctetStream: Blob,
         |  @contentType("application/json")
         |  applicationJson: Content
         |}
         |""".stripMargin

    val model = Model
      .assembler()
      .addUnparsedModel("test.smithy", spec)
      .discoverModels()
      .assemble()
      .unwrap()

    // read TraitsService file to list all registered traits
    val resources = getClass()
      .getClassLoader()
      .getResources(
        "META-INF/services/software.amazon.smithy.model.traits.TraitService"
      )
      .asScala
      .toList

    val lines = resources.flatMap { resource =>
      scala.util
        .Using(Source.fromURL(resource))(_.getLines().toList)
        .fold(
          err => fail("Failed to load TraitService resource", err),
          identity
        )
    }

    // use ServiceLoader to load all registred trait providers
    val providers = ServiceLoader
      .load(classOf[TraitService])
      .asScala
      .filter(s => s.getShapeId().getNamespace() == Namespace)
      .map(e => e.getShapeId() -> e)
      .toMap

    // for each registered trait verify that:
    // * the input spec uses the trait
    // * the trait provider is able to serialize and deserialize the trait to
    //   and from a node
    val classesFQN =
      lines.map(_.split("\\$").head).filter { fqn =>
        fqn.split('.') match {
          case Array(Namespace, _) => true
          case _ => false
        }
      }
    val classes = classesFQN.map(
      getClass()
        .getClassLoader()
        .loadClass(_)
        .asInstanceOf[Class[Trait]]
    )
    classes.map { cls =>
      val shapes = model.getShapesWithTrait(cls).asScala
      assert(
        shapes.nonEmpty,
        s"No shape with trait ${cls.getSimpleName()} found"
      )
      shapes.foreach { shape =>
        val tr = shape.expectTrait(cls)
        val traitAsNode = tr.toNode()
        val decodedTrait =
          providers(tr.toShapeId).createTrait(tr.toShapeId(), traitAsNode)
        assertEquals(decodedTrait, tr)
      }
    }
  }
}
