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
