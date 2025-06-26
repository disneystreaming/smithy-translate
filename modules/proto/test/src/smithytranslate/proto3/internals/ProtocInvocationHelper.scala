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

package smithytranslate.proto3.internals

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

  private def loadProtoFiles(directories: String*): List[(String, String)] = {
    directories.flatMap { d =>
      val maybeDir: Option[File] =
        getClass()
          .getClassLoader()
          .getResources(d)
          .asScala
          .collectFirst {
            // do not include resources from external jars
            // here we are targeting the resources that come from
            // our local build
            case url if !url.getProtocol.startsWith("jar") =>
              new File(url.getPath())
          }

      maybeDir.toList
        .flatMap(
          _.listFiles().toSeq.filter(_.getName().endsWith(".proto")).map {
            file =>
              (d + "/" + file.getName) -> Source
                .fromFile(file)
                .getLines()
                .mkString("\n")
          }
        )
    }.toList
  }

  def generateFileSet(files: Seq[(String, String)]): Seq[FileDescriptor] = {
    val tmpDir = Files.createTempDirectory("validation").toFile
    val extraFiles = loadProtoFiles(
      "google/protobuf",
      "alloy/protobuf"
    )
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

    // println(
    //   protoc
    //     .run(
    //       Seq(
    //         "-I",
    //         tmpDir.toString,
    //         s"--descriptor_set_out=${outFile.toString}",
    //         "--include_imports"
    //       ) ++ fileNames.map(_.toString),
    //       Seq.empty
    //   )
    // )

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
