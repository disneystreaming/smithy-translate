/* Copyright 2025 Disney Streaming
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
import com.sun.net.httpserver.SimpleFileServer
import java.net.InetSocketAddress
import munit.Location

final class HttpBasedSpec extends munit.FunSuite {

  val FileServerLogLevel = SimpleFileServer.OutputLevel.NONE

  case class FileServerMetadata(baseServerUrl: String, servingDirectory: os.Path)
  case class TranslationPair(jsonSchemaInput: String, expectedSmithyOutput: String)

  private def withFileServer(
      port: Int,
  )(testCode: FileServerMetadata => Unit): Unit = {
    val dir = os.temp.dir()
    val serverAddress = new InetSocketAddress("localhost", port)

    // N.B. : Requires at least JDK 18.
    //        May need to host a file-server some other way. 
    //        Could serve files from memory
    val server = SimpleFileServer.createFileServer(
      serverAddress, 
      dir.toNIO, 
      FileServerLogLevel
    )
    val address = server.getAddress()
    val serverBasePath = s"http://${address.getHostName()}:${address.getPort()}"
    val metadata = FileServerMetadata(serverBasePath, dir)
    try {
      server.start()
      testCode(metadata)
    } finally {
      // Close and allow 1 second to finish processing requests
      server.stop(1)
    }
  }

  private def httpRefTest( port: Int)(fileContentGenerator: FileServerMetadata => Map[os.RelPath, TranslationPair])(implicit loc: Location): Unit = {
    withFileServer(port) { case m@FileServerMetadata(_, servingDirectory) =>
      val files = fileContentGenerator(m)

      files.foreach { case (path, TranslationPair(jsonSchema, _)) =>
        os.write.over(servingDirectory / path, jsonSchema, createFolders = true)
      }

      val testInputs = 
        files.toList.map { case (path, TranslationPair(jsonSchemaInput, expectedSmithyOutput)) =>
          TestUtils.ConversionTestInput(
            NonEmptyList.fromListUnsafe(path.segments.toList),
            jsonSchemaInput,
            expectedSmithyOutput
          )
        }
      TestUtils.runConversionTest(NonEmptyList.fromListUnsafe(testInputs))
    }
  }
    


  test("multiple files - root path") {
    httpRefTest(10123) { case FileServerMetadata(baseUrl, _) =>
      Map(
        os.rel / "nested.json" -> TranslationPair(
          s"""|{
              |  "$$schema": "http://json-schema.org/draft-07/schema#",
              |  "$$id": "$baseUrl/nested.json",
              |  "type": "object",
              |  "title": "nested",
              |  "additionalProperties": false,
              |  "properties": {
              |    "id": {
              |      "type": "string"
              |    }
              |  }
              |}""".stripMargin,
          s"""|namespace nested
              |
              |structure Nested {
              |    id: String,
              |}
              |""".stripMargin
        ),
      os.rel / "wrapper.json" -> TranslationPair(
        s"""|{
            |  "$$schema": "http://json-schema.org/draft-07/schema#",
            |  "$$id": "$baseUrl/wrapper.json",
            |  "type": "object",
            |  "title": "wrapper",
            |  "additionalProperties": false,
            |  "properties": {
            |    "data": {
            |      "$$ref": "$baseUrl/nested.json"
            |    }
            |  }
            |}""".stripMargin,
        s"""|namespace wrapper
            |
            |use nested#Nested
            |
            |structure Wrapper {
            |    data: Nested
            |}
            |""".stripMargin
        )
      )
    }
  }
  
  test("multiple files - entire schema reference") {
    httpRefTest(10123) { case FileServerMetadata(baseUrl, _) =>
      Map(
        os.rel / "nested.json" -> TranslationPair(
          s"""|{
              |  "$$schema": "http://json-schema.org/draft-07/schema#",
              |  "$$id": "$baseUrl/nested.json",
              |  "type": "object",
              |  "title": "nested",
              |  "additionalProperties": false,
              |  "properties": {
              |    "id": {
              |      "type": "string"
              |    }
              |  }
              |}""".stripMargin,
          s"""|namespace nested
              |
              |structure Nested {
              |    id: String,
              |}
              |""".stripMargin
        ),
      os.rel / "wrapper.json" -> TranslationPair(
        s"""|{
            |  "$$schema": "http://json-schema.org/draft-07/schema#",
            |  "$$id": "$baseUrl/wrapper.json",
            |  "type": "object",
            |  "title": "wrapper",
            |  "additionalProperties": false,
            |  "properties": {
            |    "data": {
            |      "$$ref": "$baseUrl/nested.json#/properties/id"
            |    }
            |  }
            |}""".stripMargin,
        s"""|namespace wrapper
            |
            |use nested#Nested
            |
            |structure Wrapper {
            |    data: String
            |}
            |""".stripMargin
        )
      )
    }
  }

  
  test("multiple files - nested path") {
    httpRefTest(10123) { case FileServerMetadata(baseUrl, _) =>
      Map(
        os.rel / "foo" / "nested.json" -> TranslationPair(
          s"""|{
              |  "$$schema": "http://json-schema.org/draft-07/schema#",
              |  "$$id": "$baseUrl/foo/nested.json",
              |  "type": "object",
              |  "title": "nested",
              |  "additionalProperties": false,
              |  "properties": {
              |    "id": {
              |      "type": "string"
              |    }
              |  }
              |}""".stripMargin,
          s"""|namespace foo.nested
              |
              |structure Nested {
              |    id: String,
              |}
              |""".stripMargin
        ),
      os.rel / "wrapper.json" -> TranslationPair(
        s"""|{
            |  "$$schema": "http://json-schema.org/draft-07/schema#",
            |  "$$id": "$baseUrl/wrapper.json",
            |  "type": "object",
            |  "title": "wrapper",
            |  "additionalProperties": false,
            |  "properties": {
            |    "data": {
            |      "$$ref": "$baseUrl/foo/nested.json"
            |    }
            |  }
            |}""".stripMargin,
        s"""|namespace wrapper
            |
            |use foo.nested#Nested
            |
            |structure Wrapper {
            |    data: Nested
            |}
            |""".stripMargin
        )
      )
    }
  }
 
  test("multiple files - with defs in referenced file") {
    httpRefTest(10123) { case FileServerMetadata(baseUrl, _) =>
      Map(
        os.rel / "nested.json" -> TranslationPair(
          s"""|{
              |  "$$schema": "http://json-schema.org/draft-07/schema#",
              |  "$$id": "$baseUrl/nested.json",
              |  "type": "object",
              |  "title": "nested",
              |  "additionalProperties": false,
              |  "$$defs": {
              |    "Bar": {
              |      "type": "object",
              |      "properties": { "id": { "type": "string" } }
              |    }
              |  },
              |  "properties": {
              |    "bar": { "$$ref": "#/$$defs/Bar" }
              |  }
              |}""".stripMargin,
          s"""|namespace nested
              |
              |structure Bar {
              |    id: String
              |}
              |
              |structure Nested {
              |    bar: Bar
              |}
              |""".stripMargin
        ),
      os.rel / "wrapper.json" -> TranslationPair(
        s"""|{
            |  "$$schema": "http://json-schema.org/draft-07/schema#",
            |  "$$id": "$baseUrl/wrapper.json",
            |  "type": "object",
            |  "title": "wrapper",
            |  "additionalProperties": false,
            |  "properties": {
            |    "data": {
            |      "$$ref": "$baseUrl/nested.json"
            |    }
            |  }
            |}""".stripMargin,
        s"""|namespace wrapper
            |
            |use nested#Nested
            |
            |structure Wrapper {
            |    data: Nested
            |}
            |""".stripMargin
        )
      )
    }
  }
  
  test("multiple files - referencing def in external file") {
    httpRefTest(10123) { case FileServerMetadata(baseUrl, _) =>
      Map(
        os.rel / "nested.json" -> TranslationPair(
          s"""|{
              |  "$$schema": "http://json-schema.org/draft-07/schema#",
              |  "$$id": "$baseUrl/nested.json",
              |  "type": "object",
              |  "title": "nested",
              |  "additionalProperties": false,
              |  "$$defs": {
              |    "Bar": {
              |      "type": "object",
              |      "properties": { "id": { "type": "string" } }
              |    }
              |  },
              |  "properties": {
              |    "bar": { "$$ref": "#/$$defs/Bar" }
              |  }
              |}""".stripMargin,
          s"""|namespace nested
              |
              |structure Bar {
              |    id: String
              |}
              |
              |structure Nested {
              |    bar: Bar
              |}
              |""".stripMargin
        ),
      os.rel / "wrapper.json" -> TranslationPair(
        s"""|{
            |  "$$schema": "http://json-schema.org/draft-07/schema#",
            |  "$$id": "$baseUrl/wrapper.json",
            |  "type": "object",
            |  "title": "wrapper",
            |  "additionalProperties": false,
            |  "properties": {
            |    "data": {
            |      "$$ref": "$baseUrl/nested.json/#/$$defs/Bar"
            |    }
            |  }
            |}""".stripMargin,
        s"""|namespace wrapper
            |
            |use nested#Bar
            |
            |structure Wrapper {
            |    data: Bar
            |}
            |""".stripMargin
        )
      )
    }
  }
}
