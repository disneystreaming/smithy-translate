import $file.buildSetup
import $file.buildDeps

import mill._
import buildSetup._
import buildSetup.ScalaVersions._
import coursier.maven.MavenRepository
import mill.contrib.scalapblib.ScalaPBModule
import mill.contrib.buildinfo
import mill.define.Sources
import mill.define.Task
import mill.scalalib.Assembly
import mill.util.Jvm
import mill.scalajslib.ScalaJSModule
import mill.scalalib._
import mill.scalalib.publish._
import mill.contrib.buildinfo.BuildInfo

import scala.Ordering.Implicits._
import mill.eval.Evaluator

object `compiler-core` extends Cross[CompilerCoreModule](scalaVersions)
trait CompilerCoreModule
    extends CrossScalaModule
    with BaseScalaModule
    with BasePublishModule {

  def publishArtifactName = "smithytranslate-compiler-core"

  def moduleDeps = Seq(traits)

  def ivyDeps = Agg(
    ivy"com.fasterxml.jackson.core:jackson-databind:2.20.0",
    buildDeps.smithy.model,
    buildDeps.smithy.build,
    buildDeps.cats.mtl,
    buildDeps.ciString,
    buildDeps.slf4j,
    buildDeps.alloy.core,
    buildDeps.collectionsCompat
  )

  object test extends ScalaTests with BaseMunitTests {
    def ivyDeps = super.ivyDeps() ++ Agg(buildDeps.smithy.diff)
  }
}

object `json-schema` extends Cross[JsonSchemaModule](scalaVersions)
trait JsonSchemaModule
    extends CrossScalaModule
    with BaseScalaModule
    with BasePublishModule {
  def moduleDeps = Seq(`compiler-core`())

  def publishArtifactName = "smithytranslate-json-schema"

  def ivyDeps = Agg(
    buildDeps.circe.jawn,
    buildDeps.everit.jsonSchema,
    buildDeps.collectionsCompat
  )

  object test extends ScalaTests with BaseMunitTests {
    def moduleDeps = super.moduleDeps ++ Seq(`compiler-core`().test)

    def ivyDeps = super.ivyDeps() ++ Agg(
      buildDeps.smithy.build,
      buildDeps.lihaoyi.oslib
    )
  }
}

object openapi extends Cross[OpenApiModule](scalaVersions)
trait OpenApiModule
    extends CrossScalaModule
    with BaseScalaModule
    with BasePublishModule {

  def publishArtifactName = "smithytranslate-openapi"

  def moduleDeps = Seq(`compiler-core`())

  def ivyDeps =
    buildDeps.swagger.parser

  object test extends ScalaTests with BaseMunitTests {
    def moduleDeps = super.moduleDeps ++ Seq(`compiler-core`().test)

    def ivyDeps = super.ivyDeps() ++ Agg(
      buildDeps.smithy.build,
      buildDeps.smithy.diff,
      buildDeps.scalaJavaCompat
    )
  }
}

object runners extends Cross[RunnersModule](scalaVersions)
trait RunnersModule
    extends CrossScalaModule
    with BaseScalaModule
    with BasePublishModule {

  def publishArtifactName = "smithytranslate-runners"

  def ivyDeps = Agg(buildDeps.lihaoyi.oslib, buildDeps.lihaoyi.ujson,buildDeps.coursier(scalaVersion()))

  def moduleDeps =
    Seq(`compiler-core`(), openapi(), proto(), `json-schema`(), formatter.jvm())
}

object cli
    extends BaseScala213Module
    with buildinfo.BuildInfo
    with BasePublishModule {

  def publishArtifactName = "smithytranslate-cli"

  def moduleDeps =
    Seq(
      runners(scala213)
    )

  def ivyDeps = Agg(
    buildDeps.decline,
    buildDeps.smithy.build
  )

  object test extends ScalaTests with BaseMunitTests {
    def ivyDeps =
      super.ivyDeps() ++ Agg(buildDeps.lihaoyi.oslib, buildDeps.lihaoyi.ujson)
  }

  def buildInfoPackageName = "smithytranslate.cli.internal"

  def buildInfoMembers = Seq(
    BuildInfo.Value("alloyVersion", buildDeps.alloy.alloyVersion),
    BuildInfo.Value("cliVersion", publishVersion().toString)
  )

  def runProtoAux = T.task { (inputs: List[os.Path], output: os.Path) =>
    val inputArgs = inputs.flatMap { p =>
      "--input" :: p.toString() :: Nil
    }.toList
    val cmd = List("smithy-to-proto")
    val args = cmd ++ inputArgs ++ List(output.toString)

    mill.util.Jvm.callProcess(
      mainClass = finalMainClass(),
      classPath = runClasspath().map(_.path),
      mainArgs = args
    )
  }
}

object formatter extends BaseModule { outer =>

  val deps = Agg(
    ivy"org.typelevel::cats-parse::1.1.0",
    buildDeps.collectionsCompat
  )

  object jvm extends Cross[JvmModule](scalaVersions)

  trait JvmModule
      extends CrossScalaModule
      with BaseScalaModule
      with BasePublishModule { jvmOuter =>

    def publishArtifactName = "smithytranslate-formatter"

    override def ivyDeps = T { super.ivyDeps() ++ deps }
    override def millSourcePath = outer.millSourcePath

    object test extends ScalaTests with BaseMunitTests {
      def ivyDeps = super.ivyDeps() ++ Agg(
        buildDeps.smithy.build,
        buildDeps.lihaoyi.oslib
      )
    }

    object `parser-test` extends BaseScalaModule {
      def scalaVersion = jvmOuter.scalaVersion
      def moduleDeps = Seq(jvmOuter)
      override def millSourcePath = outer.millSourcePath / "parser-test"

      def ivyDeps = Agg(
        buildDeps.decline,
        buildDeps.lihaoyi.oslib
      )
    }

    object shaded extends BaseJavaModule with BasePublishModule {

      override def millSourcePath = outer.millSourcePath / "shaded"

      def publishArtifactName = "smithytranslate-formatter-shaded"

      override def localClasspath: T[Seq[PathRef]] =
        jvmOuter.localClasspath()

      override def resolvedRunIvyDeps: T[Agg[PathRef]] =
        jvmOuter.resolvedRunIvyDeps()

      override def publishXmlDeps = T.task { Agg.empty[Dependency] }

      override def assemblyRules: Seq[Assembly.Rule] =
        super.assemblyRules ++ Seq(
          Assembly.Rule
            .Relocate("smithytranslate.**", "smithyfmt.smithytranslate.@1"),
          Assembly.Rule.Relocate("scala.**", "smithyfmt.scala.@1"),
          Assembly.Rule.Relocate("cats.**", "smithyfmt.cats.@1")
        )
      override def jar: T[PathRef] = assembly
    }

    object `java-api` extends BaseJavaModule with BasePublishModule {
      def publishArtifactName = "smithytranslate-formatter-java-api"

      override def unmanagedClasspath = T {
        super.unmanagedClasspath() ++ Agg(jvmOuter.shaded.jar())
      }
      override def publishXmlDeps = T.task {
        Agg(
          mill.scalalib.publish.Dependency(
            jvmOuter.shaded.publishSelfDependency(),
            Scope.Compile
          )
        )
      }
      override def millSourcePath = outer.millSourcePath / "java-api"
    }
  }

  object js extends Cross[JsModule](scalaVersions)
  trait JsModule extends CrossScalaModule with BaseScalaJSModule {

    def publishArtifactName = "smithytranslate-formatter"

    override def ivyDeps = T { super.ivyDeps() ++ deps }
    override def millSourcePath = outer.millSourcePath

    def jsSources = T { PathRef(millSourcePath / "src-js") }

    override def sources = T {
      super.sources() :+ jsSources()
    }
  }
}

object traits extends BaseJavaModule with BasePublishModule {

  def publishArtifactName = "smithytranslate-traits"

  def ivyDeps = Agg(
    buildDeps.smithy.model
  )

  /** Exclude smithy file from source jars to avoid conflict with smithy files
    * packaged into the main jar (happens when scanning the classpath via the
    * ModelAssembler if sources jars were resolved).
    */
  override def sourceJar: T[PathRef] = T {
    def underMetaInfSmithy(p: os.RelPath): Boolean =
      Seq("META-INF", "smithy").forall(p.segments.contains)

    Jvm.createJar(
      (allSources() ++ resources())
        .map(_.path)
        .filter(os.exists),
      manifest(),
      fileFilter = (_, relPath) => !underMetaInfSmithy(relPath)
    )
  }

  object test extends Cross[TestModule](scalaVersions)
  trait TestModule extends CrossScalaModule with JavaTests with BaseMunitTests
}

object `readme-validator` extends BaseScala213Module {
  def moduleDeps =
    Seq(
      openapi(scala213),
      proto(scala213),
      `json-schema`(scala213),
      `compiler-core`(scala213).test
    )

  def ivyDeps = Agg(
    buildDeps.cats.parse,
    buildDeps.lihaoyi.oslib
  )

  def validate() = T.command {
    val args = docs.docFiles().map(_.path.toString)
    mill.util.Jvm.callProcess(
      mainClass = finalMainClass(),
      classPath = runClasspath().map(_.path),
      mainArgs = args
    )
  }
}

object docs extends BasePublishModule {

  override def publishArtifactName = "smithytranslate-docs"

  def docFiles =
    T.sources(os.walk(millSourcePath).map(mill.api.PathRef(_)))

  override def resources = T.sources {
    docFiles()
  }

}

object proto extends Cross[ProtoModule](scalaVersions)
trait ProtoModule
    extends CrossScalaModule
    with BaseScalaModule
    with BasePublishModule {

  def publishArtifactName = "smithytranslate-proto"

  def ivyDeps = Agg(
    buildDeps.smithy.model,
    buildDeps.alloy.core,
    buildDeps.collectionsCompat
  )

  def moduleDeps = Seq(traits, transitive())
  object test extends ScalaTests with BaseMunitTests with ScalaPBModule {
    def ivyDeps = super.ivyDeps() ++ Agg(
      buildDeps.smithy.build,
      buildDeps.scalapb.compilerPlugin,
      buildDeps.scalapb.protocCache.withDottyCompat(scalaVersion()),
      buildDeps.alloy.protobuf
    )
    def scalaPBVersion = buildDeps.scalapb.version

    // There are no sources to generate in this module.
    // We use scalaPB to unpack some files.
    // Changes in the 0.10.10 version mill removed a check
    // that ensures the directory existed before running the
    // compiler and that breaks this build.
    // See: https://github.com/com-lihaoyi/mill/pull/2126/files
    def compileScalaPB = T {
      val out = T.dest
      PathRef(out)
    }

    override def scalaPBOptions = "scala3_sources"

    def protobufDefinitions = T.sources { Seq(scalaPBUnpackProto()) }

    def resources = T.sources {
      super.resources() ++ protobufDefinitions()
    }
  }
}

object `proto-examples` extends BaseScala213Module with ScalaPBModule {
  def scalaPBVersion = buildDeps.scalapb.version

  def smithyFiles = T.sources {
    os.walk(millSourcePath / "smithy", skip = !_.last.endsWith(".smithy"))
      .map(p => PathRef(p))
  }

  def cliRunOutput = millSourcePath / "protobuf"

  def scalaPBSources = T.sources { runCli()() }

  // required to include wrappers proto definitions
  def scalaPBIncludePath = T.sources { Seq(scalaPBUnpackProto()) }

  def runCli() = T.command {
    os.remove.all(cliRunOutput)
    os.makeDir.all(cliRunOutput)
    val input = smithyFiles().toList.map(_.path)
    val f = cli.runProtoAux()
    f(input, cliRunOutput)
    cliRunOutput
  }

  def ivyDeps = Agg(
    buildDeps.grpc.netty,
    buildDeps.grpc.services,
    buildDeps.scalapb.runtimeGrpc
  )
}

object transitive extends Cross[TransitiveModule](scalaVersions)
trait TransitiveModule
    extends CrossScalaModule
    with BaseScalaModule
    with BasePublishModule {

  def publishArtifactName = "smithytranslate-transitive"

  def ivyDeps = Agg(
    buildDeps.smithy.model,
    buildDeps.smithy.build,
    buildDeps.collectionsCompat
  )
  object test extends ScalaTests with BaseMunitTests {
    def ivyDeps = super.ivyDeps() ++ Agg(
      buildDeps.scalaJavaCompat
    )
  }
}
