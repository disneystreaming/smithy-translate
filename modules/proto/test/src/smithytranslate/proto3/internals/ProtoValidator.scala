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

import scalapb.compiler._
import protocgen.CodeGenRequest
import com.google.protobuf.compiler.PluginProtos

object ProtoValidator extends ProtocInvocationHelper {
  def run(
      files: (String, String)*
  ): Unit = {
    val fileset = generateFileSet(files)
    val genRequest = PluginProtos.CodeGeneratorRequest.newBuilder().build()
    val request = new CodeGenRequest("", Seq.empty, fileset, None, genRequest)
    val validation = new ProtoValidation(
      DescriptorImplicits.fromCodeGenRequest(new GeneratorParams, request)
    )
    validation.validateFiles(fileset)
    ()
  }
}
