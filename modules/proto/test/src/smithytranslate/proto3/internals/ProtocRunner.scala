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

import coursier._
import coursier.core.Extension
import protocbridge.SystemDetector

// This file was copied exactly from ScalaPB
// (https://github.com/scalapb/ScalaPB/blob/6cf2b623c8458d13a8e3edcc4366c7f85ecf73fe/compiler-plugin/src/test/scala/scalapb/compiler/ProtocRunner.scala)
// ScalaPB is licensed under Apache 2.0. The copied file does not have a license header to bring over.
object ProtocRunner {
  def forVersion(version: String): protocbridge.ProtocRunner[Int] = {
    val protocDep =
      Dependency(
        Module(Organization("com.google.protobuf"), ModuleName("protoc")),
        version = version
      ).withPublication(
        "protoc",
        Type("jar"),
        Extension("exe"),
        Classifier(SystemDetector.detectedClassifier())
      )

    val protoc = Fetch().addDependencies(protocDep).run().head
    protoc.setExecutable(true)
    protocbridge.ProtocRunner(protoc.getAbsolutePath())
  }
}
