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

package smithyproto.validation

import com.google.protobuf.Descriptors.FileDescriptor
import java.nio.file.Files
import java.io.File
import java.io.PrintWriter
import java.io.FileInputStream
import com.google.protobuf.ExtensionRegistry
import scalapb.options.Scalapb
import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import scala.jdk.CollectionConverters._
import scalapb.compiler.Version
import scala.io.Source

// This file was copied from ScalaPB. It has been modified to be able to handle files in sub-directories
// and include external proto files as well
// (https://github.com/scalapb/ScalaPB/blob/6cf2b623c8458d13a8e3edcc4366c7f85ecf73fe/compiler-plugin/src/test/scala/scalapb/compiler/ProtocInvocationHelper.scala)
// ScalaPB is licensed under Apache 2.0. The copied file does not have a license header to bring over.
trait ProtocInvocationHelper {
  private lazy val protoc = ProtocRunner.forVersion(Version.protobufVersion)

  private def loadProtoFiles(names: String*): List[(String, String)] = {
    val dir = "/google/protobuf/"
    val path = getClass.getResource(dir)
    val folder = new File(path.getPath)
    if (folder.exists && folder.isDirectory) {
      folder.listFiles.toList
        .collect {
          case file if names.contains(file.getName) =>
            dir + file.getName -> Source
              .fromFile(file)
              .getLines()
              .mkString("\n")
        }
    } else List.empty
  }

  def generateFileSet(files: Seq[(String, String)]): Seq[FileDescriptor] = {
    val tmpDir = Files.createTempDirectory("validation").toFile
    val extraFiles = loadProtoFiles("wrappers.proto", "any.proto")
    val allFiles = files ++ extraFiles
    val fileNames = allFiles.map { case (name, content) =>
      val names = name.split("/")
      if (names.length > 0) {
        val dirs = names.dropRight(1).mkString("/")
        new File(tmpDir, dirs).mkdirs()
      }
      val file = new File(tmpDir, name)
      val pw = new PrintWriter(file)
      pw.write(content)
      pw.close()
      file.getAbsoluteFile
    }
    val outFile = new File(tmpDir, "descriptor.out")
    require(
      protoc
        .run(
          Seq(
            "-I",
            tmpDir.toString,
            s"--descriptor_set_out=${outFile.toString}",
            "--include_imports"
          ) ++ fileNames.map(_.toString),
          Seq.empty
        ) == 0,
      "protoc exited with an error"
    )

    val fileset: Seq[FileDescriptor] = {
      val fin = new FileInputStream(outFile)
      val registry = ExtensionRegistry.newInstance()
      Scalapb.registerAllExtensions(registry)
      val fileset =
        try {
          FileDescriptorSet.parseFrom(fin, registry)
        } finally {
          fin.close()
        }
      fileset.getFileList.asScala
        .foldLeft[Map[String, FileDescriptor]](Map.empty) { case (acc, fp) =>
          val deps = fp.getDependencyList.asScala.map(acc)
          acc + (fp.getName -> FileDescriptor.buildFrom(fp, deps.toArray))
        }
        .values
        .toVector
    }
    fileset
  }
}
