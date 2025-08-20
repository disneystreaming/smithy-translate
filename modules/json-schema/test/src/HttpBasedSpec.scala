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
import smithytranslate.compiler.SmithyVersion
import smithytranslate.compiler.ToSmithyCompilerOptions
import munit.FailException

final class HttpBasedSpec extends munit.FunSuite {

  val FileServerLogLevel = SimpleFileServer.OutputLevel.NONE

  private case class FileServerMetadata(baseServerUrl: String, servingDirectory: os.Path)
  private case class TranslationPair(jsonSchemaInput: String, expectedSmithyOutput: String)
  private case class LocalAndRemoteSchemas(localSchemas: List[(os.RelPath, TranslationPair)], remoteSchemas: List[(os.RelPath, TranslationPair)])


  private def withFileServer(testCode: FileServerMetadata => Unit): Unit = {
    val dir = os.temp.dir()
    val serverAddress = new InetSocketAddress("localhost", 0)

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

  private def httpRefTest(compilerOptionsTransform: ToSmithyCompilerOptions => ToSmithyCompilerOptions = identity)(createSchemas: FileServerMetadata => LocalAndRemoteSchemas)(implicit loc: Location): Unit = {
    withFileServer { case m@FileServerMetadata(_, servingDirectory) =>
      val LocalAndRemoteSchemas(localSchemas, remoteSchemas) = createSchemas(m)
      val localDir = os.temp.dir()

      // Write local schemas to their own temp dir
      localSchemas.foreach { case (path, TranslationPair(jsonSchema, _)) =>
        os.write.over(localDir / path, jsonSchema, createFolders = true)
      }

      // Write remote schemas to the http server's servingDirectory
      remoteSchemas.foreach { case (path, TranslationPair(jsonSchema, _)) =>
        os.write.over(servingDirectory / path, jsonSchema, createFolders = true)
      }

      val remoteTestInputs = 
        localSchemas.map { 
          case (path, TranslationPair(jsonSchemaInput, expectedSmithyOutput)) =>
            TestUtils.ConversionTestInput(
              NonEmptyList.fromListUnsafe(path.segments.toList),
              jsonSchemaInput,
              expectedSmithyOutput
            )
        } ++ 
        remoteSchemas.map {
          case (path, TranslationPair(_, expectedSmithyOutput)) =>
            TestUtils.ConversionTestInput(
              NonEmptyList.fromListUnsafe(path.segments.toList),
              None, // This should be picked up from the remote server.
              expectedSmithyOutput,
              None,
              SmithyVersion.Two
            )
        }
      TestUtils.runConversionTestWithOpts(
        compilerOptionsTransform(
          ToSmithyCompilerOptions(
            useVerboseNames = false,
            validateInput = false,
            validateOutput = false,
            transformers = List.empty,
            useEnumTraitSyntax = false,
            debug = true,
            allowedRemoteRefs = Vector(m.baseServerUrl)
          )
        ),
        NonEmptyList.fromListUnsafe(remoteTestInputs))
    }
  }
    


  test("single local file - single remote file in root path") {
    httpRefTest() { case FileServerMetadata(baseUrl, _) => LocalAndRemoteSchemas(
      localSchemas = List(
        os.rel / "local.json" -> TranslationPair(
        s"""|{
            |  "$$schema": "http://json-schema.org/draft-07/schema#",
            |  "$$id": "local.json",
            |  "type": "object",
            |  "title": "local",
            |  "additionalProperties": false,
            |  "properties": {
            |    "data": {
            |      "$$ref": "$baseUrl/remote.json"
            |    }
            |  }
            |}""".stripMargin,
        s"""|namespace local
            |
            |use remote#Remote
            |
            |structure Local {
            |    data: Remote
            |}
            |""".stripMargin
        )
      ),
      remoteSchemas = List(
        os.rel / "remote.json" -> TranslationPair(
          s"""|{
              |  "$$schema": "http://json-schema.org/draft-07/schema#",
              |  "$$id": "$baseUrl/remote.json",
              |  "type": "object",
              |  "title": "Remote",
              |  "additionalProperties": false,
              |  "properties": {
              |    "id": {
              |      "type": "string"
              |    }
              |  }
              |}""".stripMargin,
          s"""|namespace remote
              |
              |structure Remote {
              |    id: String,
              |}
              |""".stripMargin
        ),
      )
    )}
  }
  
  test("single local file - remote file referencing other remote file in root path") {
    httpRefTest() { case FileServerMetadata(baseUrl, _) => LocalAndRemoteSchemas(
      localSchemas = List(
        os.rel / "local.json" -> TranslationPair(
        s"""|{
            |  "$$schema": "http://json-schema.org/draft-07/schema#",
            |  "$$id": "local.json",
            |  "type": "object",
            |  "title": "local",
            |  "additionalProperties": false,
            |  "properties": {
            |    "data": {
            |      "$$ref": "$baseUrl/remote2.json"
            |    }
            |  }
            |}""".stripMargin,
        s"""|namespace local
            |
            |use remote2#Remote2
            |
            |structure Local {
            |    data: Remote2
            |}
            |""".stripMargin
        )
      ),
      remoteSchemas = List(
        os.rel / "remote1.json" -> TranslationPair(
          s"""|{
              |  "$$schema": "http://json-schema.org/draft-07/schema#",
              |  "$$id": "$baseUrl/remote1.json",
              |  "type": "object",
              |  "title": "Remote1",
              |  "additionalProperties": false,
              |  "properties": {
              |    "something": {
              |      "type": "string"
              |    }
              |  }
              |}""".stripMargin,
          s"""|namespace remote1
              |
              |structure Remote1 {
              |    something: String
              |}
              |""".stripMargin
        ),
        os.rel / "remote2.json" -> TranslationPair(
          s"""|{
              |  "$$schema": "http://json-schema.org/draft-07/schema#",
              |  "$$id": "$baseUrl/remote2.json",
              |  "type": "object",
              |  "title": "Remote2",
              |  "additionalProperties": false,
              |  "properties": {
              |    "id": {
              |      "type": "string"
              |    },
              |    "other": {
              |      "$$ref": "$baseUrl/remote1.json"
              |    }
              |  }
              |}""".stripMargin,
          s"""|namespace remote2
              |
              |use remote1#Remote1
              |
              |structure Remote2 {
              |    id: String
              |    other: Remote1
              |}
              |""".stripMargin
        ),
      )
    )}
  }
  
  test("multiple local - locally available file referenced as remote") {
    httpRefTest() { case FileServerMetadata(baseUrl, _) => LocalAndRemoteSchemas(
      localSchemas = List(
        os.rel / "local.json" -> TranslationPair(
          s"""|{
              |  "$$schema": "http://json-schema.org/draft-07/schema#",
              |  "$$id": "local.json",
              |  "type": "object",
              |  "title": "local",
              |  "additionalProperties": false,
              |  "properties": {
              |    "data": {
              |      "$$ref": "$baseUrl/duplicatedInRemote.json"
              |    }
              |  }
              |}""".stripMargin,
          s"""|namespace local
              |
              |use duplicatedInRemote#DuplicatedInRemote
              |
              |structure Local {
              |    data: DuplicatedInRemote
              |}
              |""".stripMargin
        ),
        os.rel / "duplicatedInRemote.json" -> TranslationPair(
          s"""|{
              |  "$$schema": "http://json-schema.org/draft-07/schema#",
              |  "$$id": "$baseUrl/duplicatedInRemote.json",
              |  "type": "object",
              |  "title": "DuplicatedInRemote",
              |  "additionalProperties": false,
              |  "properties": {
              |    "something": {
              |      "type": "string"
              |    }
              |  }
              |}""".stripMargin,
          s"""|namespace duplicatedInRemote
              |
              |structure DuplicatedInRemote {
              |    something: String
              |}
              |""".stripMargin
        ),
      ),
      remoteSchemas = List(
        os.rel / "duplicatedInRemote.json" -> TranslationPair(
          s"""|{
              |  "$$schema": "http://json-schema.org/draft-07/schema#",
              |  "$$id": "$baseUrl/duplicatedInRemote.json",
              |  "type": "object",
              |  "title": "DuplicatedInRemote",
              |  "additionalProperties": false,
              |  "properties": {
              |    "something": {
              |      "type": "string"
              |    }
              |  }
              |}""".stripMargin,
          s"""|namespace duplicatedInRemote
              |
              |structure DuplicatedInRemote {
              |    something: String
              |}
              |""".stripMargin
        ),
      )
    )}
  }
  
  test("single local file - single remote file in root path") {
    httpRefTest() { case FileServerMetadata(baseUrl, _) => LocalAndRemoteSchemas(
      localSchemas = List(
        os.rel / "local.json" -> TranslationPair(
        s"""|{
            |  "$$schema": "http://json-schema.org/draft-07/schema#",
            |  "$$id": "local.json",
            |  "type": "object",
            |  "title": "local",
            |  "additionalProperties": false,
            |  "properties": {
            |    "data": {
            |      "$$ref": "$baseUrl/remote.json"
            |    }
            |  }
            |}""".stripMargin,
        s"""|namespace local
            |
            |use remote#Remote
            |
            |structure Local {
            |    data: Remote
            |}
            |""".stripMargin
        )
      ),
      remoteSchemas = List(
        os.rel / "remote.json" -> TranslationPair(
          s"""|{
              |  "$$schema": "http://json-schema.org/draft-07/schema#",
              |  "$$id": "$baseUrl/remote.json",
              |  "type": "object",
              |  "title": "Remote",
              |  "additionalProperties": false,
              |  "properties": {
              |    "id": {
              |      "type": "string"
              |    }
              |  }
              |}""".stripMargin,
          s"""|namespace remote
              |
              |structure Remote {
              |    id: String,
              |}
              |""".stripMargin
        ),
      )
    )}
  }
  
  test("single local file - remote file referencing other remote file in root path") {
    httpRefTest() { case FileServerMetadata(baseUrl, _) => LocalAndRemoteSchemas(
      localSchemas = List(
        os.rel / "local.json" -> TranslationPair(
        s"""|{
            |  "$$schema": "http://json-schema.org/draft-07/schema#",
            |  "$$id": "local.json",
            |  "type": "object",
            |  "title": "local",
            |  "additionalProperties": false,
            |  "properties": {
            |    "data": {
            |      "$$ref": "$baseUrl/remote2.json"
            |    }
            |  }
            |}""".stripMargin,
        s"""|namespace local
            |
            |use remote2#Remote2
            |
            |structure Local {
            |    data: Remote2
            |}
            |""".stripMargin
        )
      ),
      remoteSchemas = List(
        os.rel / "remote1.json" -> TranslationPair(
          s"""|{
              |  "$$schema": "http://json-schema.org/draft-07/schema#",
              |  "$$id": "$baseUrl/remote1.json",
              |  "type": "object",
              |  "title": "Remote1",
              |  "additionalProperties": false,
              |  "properties": {
              |    "something": {
              |      "type": "string"
              |    }
              |  }
              |}""".stripMargin,
          s"""|namespace remote1
              |
              |structure Remote1 {
              |    something: String
              |}
              |""".stripMargin
        ),
        os.rel / "remote2.json" -> TranslationPair(
          s"""|{
              |  "$$schema": "http://json-schema.org/draft-07/schema#",
              |  "$$id": "$baseUrl/remote2.json",
              |  "type": "object",
              |  "title": "Remote2",
              |  "additionalProperties": false,
              |  "properties": {
              |    "id": {
              |      "type": "string"
              |    },
              |    "other": {
              |      "$$ref": "$baseUrl/remote1.json"
              |    }
              |  }
              |}""".stripMargin,
          s"""|namespace remote2
              |
              |use remote1#Remote1
              |
              |structure Remote2 {
              |    id: String
              |    other: Remote1
              |}
              |""".stripMargin
        ),
      )
    )}
  }
  
  test("multiple local - locally available file referenced as remote") {
    httpRefTest() { case FileServerMetadata(baseUrl, _) => LocalAndRemoteSchemas(
      localSchemas = List(
        os.rel / "local.json" -> TranslationPair(
          s"""|{
              |  "$$schema": "http://json-schema.org/draft-07/schema#",
              |  "$$id": "local.json",
              |  "type": "object",
              |  "title": "local",
              |  "additionalProperties": false,
              |  "properties": {
              |    "data": {
              |      "$$ref": "$baseUrl/duplicatedInRemote.json"
              |    }
              |  }
              |}""".stripMargin,
          s"""|namespace local
              |
              |use duplicatedInRemote#DuplicatedInRemote
              |
              |structure Local {
              |    data: DuplicatedInRemote
              |}
              |""".stripMargin
        ),
        os.rel / "duplicatedInRemote.json" -> TranslationPair(
          s"""|{
              |  "$$schema": "http://json-schema.org/draft-07/schema#",
              |  "$$id": "$baseUrl/duplicatedInRemote.json",
              |  "type": "object",
              |  "title": "DuplicatedInRemote",
              |  "additionalProperties": false,
              |  "properties": {
              |    "something": {
              |      "type": "string"
              |    }
              |  }
              |}""".stripMargin,
          s"""|namespace duplicatedInRemote
              |
              |structure DuplicatedInRemote {
              |    something: String
              |}
              |""".stripMargin
        ),
      ),
      remoteSchemas = List(
        os.rel / "duplicatedInRemote.json" -> TranslationPair(
          s"""|{
              |  "$$schema": "http://json-schema.org/draft-07/schema#",
              |  "$$id": "$baseUrl/duplicatedInRemote.json",
              |  "type": "object",
              |  "title": "DuplicatedInRemote",
              |  "additionalProperties": false,
              |  "properties": {
              |    "something": {
              |      "type": "string"
              |    }
              |  }
              |}""".stripMargin,
          s"""|namespace duplicatedInRemote
              |
              |structure DuplicatedInRemote {
              |    something: String
              |}
              |""".stripMargin
        ),
      )
    )}
  }
  
  test("multiple local - locally available file referenced as remote, but not allowed to fetch remote") {
    // This test should pass, because the remotely referenced schema is available in the local sources.
    // This is a fairly common json-schema usecase, where all schemas are copied locally but contain their http refs.
    httpRefTest(_.copy(allowedRemoteRefs = Vector.empty)) { case FileServerMetadata(baseUrl, _) => LocalAndRemoteSchemas(
      localSchemas = List(
        os.rel / "local.json" -> TranslationPair(
          s"""|{
              |  "$$schema": "http://json-schema.org/draft-07/schema#",
              |  "$$id": "local.json",
              |  "type": "object",
              |  "title": "local",
              |  "additionalProperties": false,
              |  "properties": {
              |    "data": {
              |      "$$ref": "$baseUrl/duplicatedInRemote.json"
              |    }
              |  }
              |}""".stripMargin,
          s"""|namespace local
              |
              |use duplicatedInRemote#DuplicatedInRemote
              |
              |structure Local {
              |    data: DuplicatedInRemote
              |}
              |""".stripMargin
        ),
        os.rel / "duplicatedInRemote.json" -> TranslationPair(
          s"""|{
              |  "$$schema": "http://json-schema.org/draft-07/schema#",
              |  "$$id": "$baseUrl/duplicatedInRemote.json",
              |  "type": "object",
              |  "title": "DuplicatedInRemote",
              |  "additionalProperties": false,
              |  "properties": {
              |    "something": {
              |      "type": "string"
              |    }
              |  }
              |}""".stripMargin,
          s"""|namespace duplicatedInRemote
              |
              |structure DuplicatedInRemote {
              |    something: String
              |}
              |""".stripMargin
        ),
      ),
      remoteSchemas = List(
        os.rel / "duplicatedInRemote.json" -> TranslationPair(
          s"""|{
              |  "$$schema": "http://json-schema.org/draft-07/schema#",
              |  "$$id": "$baseUrl/duplicatedInRemote.json",
              |  "type": "object",
              |  "title": "DuplicatedInRemote",
              |  "additionalProperties": false,
              |  "properties": {
              |    "something": {
              |      "type": "string"
              |    }
              |  }
              |}""".stripMargin,
          s"""|namespace duplicatedInRemote
              |
              |structure DuplicatedInRemote {
              |    something: String
              |}
              |""".stripMargin
        ),
      )
    )}
  }

  test("remote refs not in allow list are ignored") {
    // This is expected to fail, because the refereced remote ref will get filtered out/not retrieved
    intercept[FailException] {
      httpRefTest(_.copy(allowedRemoteRefs = Vector.empty)) { case FileServerMetadata(baseUrl, _) => LocalAndRemoteSchemas(
        localSchemas = List(
          os.rel / "local.json" -> TranslationPair(
          s"""|{
              |  "$$schema": "http://json-schema.org/draft-07/schema#",
              |  "$$id": "local.json",
              |  "type": "object",
              |  "title": "local",
              |  "additionalProperties": false,
              |  "properties": {
              |    "data": {
              |      "$$ref": "$baseUrl/remote.json"
              |    }
              |  }
              |}""".stripMargin,
          s"""|namespace local
              |
              |use remote#Remote
              |
              |structure Local {
              |    data: Remote
              |}
              |""".stripMargin
          )
        ),
        remoteSchemas = List(
          os.rel / "remote.json" -> TranslationPair(
            s"""|{
                |  "$$schema": "http://json-schema.org/draft-07/schema#",
                |  "$$id": "$baseUrl/remote.json",
                |  "type": "object",
                |  "title": "Remote",
                |  "additionalProperties": false,
                |  "properties": {
                |    "id": {
                |      "type": "string"
                |    }
                |  }
                |}""".stripMargin,
            s"""|namespace remote
                |
                |structure Remote {
                |    id: String,
                |}
                |""".stripMargin
          ),
        )
      )}
    }
  }
}
