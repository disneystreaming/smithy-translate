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

package smithytranslate.proto3

import software.amazon.smithy.model.Model

object SmithyToProtoCompiler {

  /** Transforms a smithy model into a list of protobuf files.
    */
  def compile(smithyModel: Model): List[RenderedProtoFile] = {
    val compiler = new internals.Compiler(smithyModel, allShapes = false)
    compiler
      .compile()
      .map { compileOutput =>
        val contents =
          smithytranslate.proto3.internals.Renderer.render(compileOutput.unit)
        RenderedProtoFile(compileOutput.path, contents)
      }
  }

  case class RenderedProtoFile(path: List[String], contents: String)

}
