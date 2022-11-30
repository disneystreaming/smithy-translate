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

package smithytranslate.cli.runners

import smithytranslate.cli.opts.ProtoOpts
import smithytranslate.cli.transformer.TransformerLookup
import smithyproto.proto3.{Compiler, Renderer, ModelPreProcessor}
import java.net.URLClassLoader

import software.amazon.smithy.model.Model

object Proto {

  /** Builds a model from the CLI options, transforms it and then run the
    * conversion.
    */
  def runFromCli(opts: ProtoOpts): Unit = {
    val transformers = TransformerLookup.getAll()
    val currentClassLoader = this.getClass().getClassLoader()
    val modelBuilder =
      Model
        .assembler()
        .discoverModels(currentClassLoader)
        .assemble()
        .unwrap()
        .toBuilder()
    Deps.forDeps(opts.deps, opts.repositories)(modelBuilder)

    val modelAssembler = Model.assembler().addModel(modelBuilder.build())

    val model0 =
      opts.inputFiles
        .foldLeft(modelAssembler.discoverModels) { case (m, path) =>
          m.addImport(path.toNIO)
        }
        .assemble
        .unwrap
    val model = ModelPreProcessor(
      model0,
      transformers ++ ModelPreProcessor.transformers.all(None)
    )

    run(model, opts.outputPath)
  }

  /** Transforms the given model, then run the conversion.
    */
  def runForModel(
      model0: Model,
      outputPath: os.Path,
      allowedNamespace: Option[String]
  ): Unit = {
    val transformers = TransformerLookup.getAll()
    val model = ModelPreProcessor(
      model0,
      transformers ++ ModelPreProcessor.transformers.all(allowedNamespace)
    )
    run(model, outputPath)
  }

  private def run(model: Model, outputPath: os.Path): Unit = {
    val proto3Backend = new Compiler()
    val out = proto3Backend.compile(model)

    os.walk(outputPath)
      .filter(p => os.isFile(p) && p.ext == "proto")
      .foreach(os.remove)
    out.foreach { output =>
      val relpath = os.RelPath(output.path.toIndexedSeq, ups = 0)
      val rendering = Renderer.render(output.unit)
      val outPath = outputPath / relpath
      os.write(
        outPath,
        data = rendering,
        createFolders = true
      )
    }
    println(s"Produced ${out.size} protobuf files.")
  }
}

private object Deps {
  import coursier._
  import coursier.parse.DependencyParser
  import coursier.parse.RepositoryParser

  def forDeps(dependencies: List[String], repositories: List[String])(
      modelBuilder: Model.Builder
  ): Unit = {
    val validRepos = RepositoryParser
      .repositories(repositories)
      .either
      .getOrElse(sys.error("Invalid repository."))
    val validDeps = DependencyParser
      .dependencies(
        dependencies,
        defaultScalaVersion = "2.13.10"
      )
      .either
      .getOrElse(sys.error("Invalid dependency."))

    if (dependencies.nonEmpty) {
      val fetch =
        Fetch().addRepositories(validRepos: _*).addDependencies(validDeps: _*)
      val files = fetch.run()
      val urls = files.map(_.toURI().toURL()).toArray

      val upstreamClassLoader = new URLClassLoader(urls)
      val upstreamModel = Model
        .assembler()
        .discoverModels(upstreamClassLoader)
        .assemble()
        .unwrap()

      modelBuilder.addShapes(upstreamModel): Unit
    }
  }
}
