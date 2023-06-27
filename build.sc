import $ivy.`com.lihaoyi::mill-contrib-bloop:`
import $ivy.`com.lihaoyi::mill-contrib-scalapblib:`
import $ivy.`io.chris-kipp::mill-ci-release::0.1.9`
import $ivy.`com.lewisjkl::header-mill-plugin::0.0.2`

import coursier.maven.MavenRepository
import header._
import io.kipp.mill.ci.release.CiReleaseModule
import io.kipp.mill.ci.release.SonatypeHost
import mill._
import mill.contrib.scalapblib.ScalaPBModule
import mill.define.Sources
import mill.define.Task
import mill.modules.Assembly
import mill.modules.Jvm
import mill.scalajslib.api.ModuleKind
import mill.scalajslib.ScalaJSModule
import mill.scalalib._
import mill.scalalib.api.Util._
import mill.scalalib.CrossVersion.Binary
import mill.scalalib.publish._
import mill.scalalib.scalafmt.ScalafmtModule
import os._

import scala.Ordering.Implicits._

trait BaseModule extends Module with HeaderModule {
  def millSourcePath: Path = {
    val originalRelativePath = super.millSourcePath.relativeTo(os.pwd)
    os.pwd / "modules" / originalRelativePath
  }

  def includeFileExtensions: List[String] = List("scala", "java")
  def license: HeaderLicense = HeaderLicense.Custom("""|Copyright 2022 Disney Streaming
                                                       |
                                                       |Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
                                                       |you may not use this file except in compliance with the License.
                                                       |You may obtain a copy of the License at
                                                       |
                                                       |   https://disneystreaming.github.io/TOST-1.0.txt
                                                       |
                                                       |Unless required by applicable law or agreed to in writing, software
                                                       |distributed under the License is distributed on an "AS IS" BASIS,
                                                       |WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
                                                       |See the License for the specific language governing permissions and
                                                       |limitations under the License.
                                                       |""".stripMargin)
}

trait BasePublishModule extends BaseModule with CiReleaseModule {
  def artifactName =
    s"smithytranslate-${millModuleSegments.parts.mkString("-")}"

  override def sonatypeHost = Some(SonatypeHost.s01)

  def pomSettings = PomSettings(
    description = "A smithy-translation toolkit",
    organization = "com.disneystreaming.smithy",
    url = "https://github.com/disneystreaming/smithy-translate",
    licenses = Seq(
      License(
        id = "TOST-1.0",
        name = "TOMORROW OPEN SOURCE TECHNOLOGY LICENSE 1.0",
        url = "https://disneystreaming.github.io/TOST-1.0.txt",
        isOsiApproved = false,
        isFsfLibre = false,
        distribution = "repo"
      )
    ),
    versionControl = VersionControl(
      Some("https://github.com/disneystreaming/smithy-translate")
    ),
    developers = Seq(
      Developer(
        "baccata",
        "Olivier Mélois",
        "https://github.com/baccata"
      ),
      Developer(
        "lewisjkl",
        "Jeff Lewis",
        "http://github.com/lewisjkl"
      ),
      Developer(
        "daddykotex",
        "David Francoeur",
        "http://github.com/daddykotex"
      ),
      Developer(
        "yisraelU",
        "Yisrael Union",
        "http://github.com/yisraelU"
      )
    )
  )

  override def javacOptions = T {
    super.javacOptions() ++ Seq(
      "--release",
      "8"
    )
  }
}

trait ScalaVersionModule extends ScalaModule with ScalafmtModule {
  def scalaVersion = T.input("2.13.11")

  def scalacOptions = T {
    super.scalacOptions() ++ scalacOptionsFor(scalaVersion())
  }
}

trait BaseScalaNoPublishModule extends BaseModule with ScalaVersionModule {

  override def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Agg(
    ivy"org.typelevel:::kind-projector:0.13.2"
  )
}

trait BaseScalaModule extends BaseScalaNoPublishModule with BasePublishModule
trait BaseScalaJSModule extends BaseScalaModule with ScalaJSModule {
  def scalaJSVersion = "1.11.0"
  def moduleKind = ModuleKind.CommonJSModule
}

trait BaseJavaNoPublishModule extends BaseModule with JavaModule {}

trait BaseJavaModule extends BaseJavaNoPublishModule with BasePublishModule

trait BaseMunitTests extends TestModule.Munit {
  def ivyDeps =
    Agg(
      ivy"org.scalameta::munit::1.0.0-M7",
      ivy"org.scalameta::munit-scalacheck::1.0.0-M7"
    )
}

object `json-schema` extends BaseScalaModule {
  def moduleDeps = Seq(
    openapi
  )

  def ivyDeps = Agg(
    Deps.circe.jawn,
    Deps.everit.jsonSchema
  )

  object tests extends this.Tests with TestModule.Munit {
    def ivyDeps = Agg(
      Deps.munit,
      Deps.smithy.build,
      Deps.lihaoyi.oslib
    )
  }
}

object openapi extends BaseScalaModule {
  def ivyDeps = Deps.swagger.parser ++ Agg(
    Deps.smithy.model,
    Deps.smithy.build,
    Deps.cats.mtl,
    Deps.ciString,
    Deps.slf4j,
    Deps.alloy.core
  )

  def moduleDeps = Seq(
    traits
  )

  object tests extends this.Tests with TestModule.Munit {
    def ivyDeps = Agg(
      Deps.munit,
      Deps.smithy.build
    )
  }
}

object cli extends BaseScalaModule {
  def ivyDeps = Agg(
    Deps.decline,
    Deps.coursier,
    Deps.lihaoyi.oslib,
    Deps.smithy.build
  )

  def moduleDeps = Seq(openapi, proto.core, `json-schema`, formatter.jvm)

  def runProtoAux = T.task { (inputs: List[Path], output: Path) =>
    val inputArgs = inputs.flatMap { p =>
      "--input" :: p.toString() :: Nil
    }.toList
    val cmd = List("smithy-to-proto")
    val args = cmd ++ inputArgs ++ List(output.toString)

    mill.modules.Jvm.runSubprocess(
      finalMainClass(),
      runClasspath().map(_.path),
      mainArgs = args
    )
  }
}

object formatter extends BaseModule { outer =>
  val deps = Agg(
    ivy"org.typelevel::cats-parse::0.3.9"
  )

  object jvm extends BaseScalaModule {
    override def ivyDeps = T { super.ivyDeps() ++ deps }
    override def millSourcePath = outer.millSourcePath

    object tests extends this.Tests with TestModule.Munit {
      def ivyDeps = Agg(
        Deps.munit,
        Deps.smithy.build,
        Deps.lihaoyi.oslib
      )
    }

    object `parser-test` extends BaseScalaNoPublishModule {
      def moduleDeps = Seq(formatter.jvm)
      override def millSourcePath = outer.millSourcePath / "parser-test"

      def ivyDeps = Agg(
        Deps.decline,
        Deps.lihaoyi.oslib
      )
    }

    object shaded extends BaseJavaModule {
      override def millSourcePath = outer.millSourcePath / "shaded"

      override def localClasspath: T[Seq[PathRef]] =
        formatter.jvm.localClasspath()

      override def resolvedRunIvyDeps: T[Agg[PathRef]] =
        formatter.jvm.resolvedRunIvyDeps()

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

    object `java-api` extends BaseJavaModule {
      override def unmanagedClasspath = T {
        super.unmanagedClasspath() ++ Agg(formatter.jvm.shaded.jar())
      }
      override def publishXmlDeps = T.task {
        Agg(
          mill.scalalib.publish.Dependency(
            formatter.jvm.shaded.publishSelfDependency(),
            Scope.Compile
          )
        )
      }
      override def millSourcePath = outer.millSourcePath / "java-api"
    }
  }

  object js extends BaseScalaJSModule {
    override def ivyDeps = T { super.ivyDeps() ++ deps }
    override def millSourcePath = outer.millSourcePath

    def jsSources = T.sources { millSourcePath / "src-js" }

    override def sources: Sources = T.sources {
      super.sources() ++ jsSources()
    }
  }
}

object traits extends BaseJavaModule {
  def ivyDeps = Agg(
    Deps.smithy.model
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

  object tests extends this.Tests with ScalaVersionModule with BaseMunitTests
}

object `readme-validator` extends BaseScalaNoPublishModule {
  def moduleDeps = Seq(openapi, proto.core, `json-schema`)

  def ivyDeps = Agg(
    Deps.cats.parse,
    Deps.lihaoyi.oslib
  )

  def readmeFile = T.sources { os.pwd / "README.md" }

  def validate() = T.command {
    val args = Seq(readmeFile().head.path.toString)
    mill.modules.Jvm.runSubprocess(
      finalMainClass(),
      runClasspath().map(_.path),
      mainArgs = args
    )
  }
}

object proto extends Module {

  object core extends BaseScalaModule {
    def ivyDeps = Agg(
      Deps.smithy.model,
      Deps.alloy.core
    )
    def moduleDeps = Seq(traits, transitive)
    object tests extends this.Tests with TestModule.Munit with ScalaPBModule {
      def ivyDeps = Agg(
        Deps.munit,
        Deps.smithy.build,
        Deps.scalapb.compilerPlugin,
        Deps.scalapb.protocCache
      )
      def scalaPBVersion = Deps.scalapb.version

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

      def protobufDefinitions = T.sources { Seq(scalaPBUnpackProto()) }

      def resources = T.sources {
        super.resources() ++ protobufDefinitions()
      }
    }
  }

  object examples extends BaseScalaModule with ScalaPBModule {
    def scalaPBVersion = Deps.scalapb.version

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
      Deps.grpc.netty,
      Deps.grpc.services,
      Deps.scalapb.runtimeGrpc
    )
  }
}

object transitive extends BaseScalaModule {
  def ivyDeps = Agg(
    Deps.smithy.model,
    Deps.smithy.build
  )
  object tests extends Tests with BaseMunitTests
}

object Deps {
  object alloy {
    val core =
      ivy"com.disneystreaming.alloy:alloy-core:0.1.22"
  }
  object circe {
    val jawn = ivy"io.circe::circe-jawn:0.14.5"
  }
  object everit {
    val jsonSchema = ivy"com.github.erosb:everit-json-schema:1.14.2"
  }
  val slf4j =
    ivy"org.slf4j:slf4j-nop:2.0.7" // needed since swagger-parser relies on slf4j-api
  object swagger {
    val parser = Agg(
      ivy"io.swagger.parser.v3:swagger-parser:2.1.15",
      // included to override the version brought in by swagger-parser which has a vulnerability
      ivy"org.mozilla:rhino:1.7.14"
    )
  }
  object smithy {
    val smithyVersion = "1.31.0"
    val model = ivy"software.amazon.smithy:smithy-model:$smithyVersion"
    val build = ivy"software.amazon.smithy:smithy-build:$smithyVersion"
  }
  object cats {
    val mtl = ivy"org.typelevel::cats-mtl:1.3.1"
    val parse = ivy"org.typelevel::cats-parse:0.3.9"
  }
  val ciString = ivy"org.typelevel::case-insensitive:1.4.0"
  val decline = ivy"com.monovore::decline:2.4.1"
  object lihaoyi {
    val oslib = ivy"com.lihaoyi::os-lib:0.9.1"
  }

  val munit = ivy"org.scalameta::munit:0.7.29"
  object grpc {
    val version = "1.56.0"
    val netty = ivy"io.grpc:grpc-netty:$version"
    val services = ivy"io.grpc:grpc-services:$version"
  }
  object scalapb {
    val version = "0.11.13"
    val runtimeGrpc = ivy"com.thesamet.scalapb::scalapb-runtime-grpc:$version"
    val compilerPlugin =
      ivy"com.thesamet.scalapb::compilerplugin:$version"
    val protocCache = ivy"com.thesamet.scalapb::protoc-cache-coursier:0.9.6"
  }
  val coursier = ivy"io.get-coursier::coursier:2.1.5"
}

case class ScalaVersion(maj: Int, min: Int, patch: Int)
object ScalaVersion {
  def apply(scalaVersion: String): ScalaVersion = scalaVersion match {
    case ReleaseVersion(major, minor, patch) =>
      ScalaVersion(major.toInt, minor.toInt, patch.toInt)
    case MinorSnapshotVersion(major, minor, patch) =>
      ScalaVersion(major.toInt, minor.toInt, patch.toInt)
    case DottyVersion("0", minor, patch) =>
      ScalaVersion(3, minor.toInt, patch.toInt)
  }

  implicit lazy val ordering: Ordering[ScalaVersion] =
    (x: ScalaVersion, y: ScalaVersion) => {
      if (
        x.maj > y.maj || (x.maj == y.maj && x.min > y.min) || (x.maj == y.maj && x.min == y.min && x.patch > y.patch)
      ) 1
      else if (
        x.maj < y.maj || (x.maj == y.maj && x.min < y.min) || (x.maj == y.maj && x.min == y.min && x.patch < y.patch)
      ) -1
      else 0
    }
}

val v211 = ScalaVersion(2, 11, 0)
val v212 = ScalaVersion(2, 12, 0)
val v213 = ScalaVersion(2, 13, 0)
val v300 = ScalaVersion(3, 0, 0)

case class ScalacOption(
    name: String,
    isSupported: ScalaVersion => Boolean = _ => true
)

// format: off
private val allScalacOptions = Seq(
  ScalacOption("-Xsource:3", isSupported = version => v211 <= version || version < v300),                                                                     // Treat compiler input as Scala source for the specified version, see scala/bug#8126.
  ScalacOption("-deprecation", isSupported = version => version < v213 || v300 <= version),                                // Emit warning and location for usages of deprecated APIs. Not really removed but deprecated in 2.13.
  ScalacOption("-migration", isSupported = v300 <= _),                                                                     // Emit warning and location for migration issues from Scala 2.
  ScalacOption("-explaintypes", isSupported = _ < v300),                                                                   // Explain type errors in more detail.
  ScalacOption("-explain-types", isSupported = v300 <= _),                                                                 // Explain type errors in more detail.
  ScalacOption("-explain", isSupported = v300 <= _),                                                                       // Explain errors in more detail.
  ScalacOption("-feature"),                                                                                                // Emit warning and location for usages of features that should be imported explicitly.
  ScalacOption("-language:existentials", isSupported = _ < v300),                                                          // Existential types (besides wildcard types) can be written and inferred
  ScalacOption("-language:experimental.macros", isSupported = _ < v300),                                                   // Allow macro definition (besides implementation and application)
  ScalacOption("-language:higherKinds", isSupported = _ < v300),                                                           // Allow higher-kinded types
  ScalacOption("-language:implicitConversions", isSupported = _ < v300),                                                   // Allow definition of implicit functions called views
  ScalacOption("-language:existentials,experimental.macros,higherKinds,implicitConversions", isSupported = v300 <= _),     // the four options above, dotty style
  ScalacOption("-unchecked"),                                                                                              // Enable additional warnings where generated code depends on assumptions.
  ScalacOption("-Xcheckinit", isSupported = _ < v300),                                                                     // Wrap field accessors to throw an exception on uninitialized access.
  ScalacOption("-Xfatal-warnings"),                                                                                        // Fail the compilation if there are any warnings.
  ScalacOption("-Xlint", isSupported = _ < v211),                                                                          // Used to mean enable all linting options but now the syntax for that is different (-Xlint:_ I think)
  ScalacOption("-Xlint:adapted-args", isSupported = version => v211 <= version && version < v300),                         // Warn if an argument list is modified to match the receiver.
  ScalacOption("-Xlint:by-name-right-associative", isSupported = version => v211 <= version && version < v213),            // By-name parameter of right associative operator.
  ScalacOption("-Xlint:constant", isSupported = version => v212 <= version && version < v300),                             // Evaluation of a constant arithmetic expression results in an error.
  ScalacOption("-Xlint:delayedinit-select", isSupported = version => v211 <= version && version < v300),                   // Selecting member of DelayedInit.
  ScalacOption("-Xlint:deprecation", isSupported = version => v213 <= version && version < v300),                          // Emit warning and location for usages of deprecated APIs.
  ScalacOption("-Xlint:doc-detached", isSupported = version => v211 <= version && version < v300),                         // A Scaladoc comment appears to be detached from its element.
  ScalacOption("-Xlint:inaccessible", isSupported = version => v211 <= version && version < v300),                         // Warn about inaccessible types in method signatures.
  ScalacOption("-Xlint:infer-any", isSupported = version => v211 <= version && version < v300),                            // Warn when a type argument is inferred to be `Any`.
  ScalacOption("-Xlint:missing-interpolator", isSupported = version => v211 <= version && version < v300),                 // A string literal appears to be missing an interpolator id.
  ScalacOption("-Xlint:nullary-override", isSupported = version => v211 <= version && version < ScalaVersion(2, 13, 3)),   // Warn when non-nullary `def f()' overrides nullary `def f'.
  ScalacOption("-Xlint:nullary-unit", isSupported = version => v211 <= version && version < v300),                         // Warn when nullary methods return Unit.
  ScalacOption("-Xlint:option-implicit", isSupported = version => v211 <= version && version < v300),                      // Option.apply used implicit view.
  ScalacOption("-Xlint:package-object-classes", isSupported = version => v211 <= version && version < v300),               // Class or object defined in package object.
  ScalacOption("-Xlint:poly-implicit-overload", isSupported = version => v211 <= version && version < v300),               // Parameterized overloaded implicit methods are not visible as view bounds.
  ScalacOption("-Xlint:private-shadow", isSupported = version => v211 <= version && version < v300),                       // A private field (or class parameter) shadows a superclass field.
  ScalacOption("-Xlint:stars-align", isSupported = version => v211 <= version && version < v300),                          // Pattern sequence wildcard must align with sequence component.
  ScalacOption("-Xlint:type-parameter-shadow", isSupported = version => v211 <= version && version < v300),                // A local type parameter shadows a type already in scope.
  ScalacOption("-Xlint:unsound-match", isSupported = version => v211 <= version && version < v213),                        // Pattern match may not be typesafe.
  ScalacOption("-Wunused:nowarn", isSupported = version => v213 <= version && version < v300),                             // Ensure that a `@nowarn` annotation actually suppresses a warning.
  ScalacOption("-Yno-adapted-args", isSupported = _ < v213),                                                               // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
  ScalacOption("-Ywarn-dead-code", isSupported = _ < v213),                                                                // Warn when dead code is identified.
  ScalacOption("-Wdead-code", isSupported = version => v213 <= version && version < v300),                                 // ^ Replaces the above
  ScalacOption("-Ywarn-extra-implicit", isSupported = version => v212 <= version && version < v213),                       // Warn when more than one implicit parameter section is defined.
  ScalacOption("-Wextra-implicit", isSupported = version => v213 <= version && version < v300),                            // ^ Replaces the above
  ScalacOption("-Ywarn-inaccessible", isSupported = _ < v211),                                                             // Warn about inaccessible types in method signatures. Alias for -Xlint:inaccessible so can be removed as of 2.11.
  ScalacOption("-Ywarn-nullary-override", isSupported = _ < v213),                                                         // Warn when non-nullary `def f()' overrides nullary `def f'.
  ScalacOption("-Ywarn-nullary-unit", isSupported = _ < v213),                                                             // Warn when nullary methods return Unit.
  ScalacOption("-Ywarn-numeric-widen", isSupported = _ < v213),                                                            // Warn when numerics are widened.
  ScalacOption("-Wnumeric-widen", isSupported = version => v213 <= version && version < v300),                             // ^ Replaces the above
  ScalacOption("-Xlint:implicit-recursion", isSupported = version => ScalaVersion(2, 13, 3) <= version && version < v300), // Warn when an implicit resolves to an enclosing self-definition
  ScalacOption("-Ywarn-unused", isSupported = version => v211 <= version && version < v212),                               // Warn when local and private vals, vars, defs, and types are unused.
  ScalacOption("-Ywarn-unused-import", isSupported = version => v211 <= version && version < v212),                        // Warn if an import selector is not referenced.
  ScalacOption("-Ywarn-unused:implicits", isSupported = version => v212 <= version && version < v213),                     // Warn if an implicit parameter is unused.
  ScalacOption("-Wunused:implicits", isSupported = version => v213 <= version && version < v300),                          // ^ Replaces the above
  ScalacOption("-Wunused:explicits", isSupported = version => v213 <= version && version < v300),                          // Warn if an explicit parameter is unused.
  ScalacOption("-Ywarn-unused:imports", isSupported = version => v212 <= version && version < v213),                       // Warn if an import selector is not referenced.
  ScalacOption("-Wunused:imports", isSupported = version => v213 <= version && version < v300),                            // ^ Replaces the above
  ScalacOption("-Ywarn-unused:locals", isSupported = version => v212 <= version && version < v213),                        // Warn if a local definition is unused.
  ScalacOption("-Wunused:locals", isSupported = version => v213 <= version && version < v300),                             // ^ Replaces the above
  ScalacOption("-Ywarn-unused:params", isSupported = version => v212 <= version && version < v213),                        // Warn if a value parameter is unused.
  ScalacOption("-Wunused:params", isSupported = version => v213 <= version && version < v300),                             // ^ Replaces the above
  ScalacOption("-Ywarn-unused:patvars", isSupported = version => v212 <= version && version < v213),                       // Warn if a variable bound in a pattern is unused.
  ScalacOption("-Wunused:patvars", isSupported = version => v213 <= version && version < v300),                            // ^ Replaces the above
  ScalacOption("-Ywarn-unused:privates", isSupported = version => v212 <= version && version < v213),                      // Warn if a private member is unused.
  ScalacOption("-Wunused:privates", isSupported = version => v213 <= version && version < v300),                           // ^ Replaces the above
  ScalacOption("-Ywarn-value-discard", isSupported = _ < v213),                                                            // Warn when non-Unit expression results are unused.
  ScalacOption("-Wvalue-discard", isSupported = version => v213 <= version && version < v300),                             // ^ Replaces the above
  ScalacOption("-Ykind-projector", isSupported = v300 <= _),                                                               // Enables a subset of kind-projector syntax (see https://github.com/lampepfl/dotty/pull/7775)
  ScalacOption("-Vimplicits", isSupported = version => ScalaVersion(2, 13, 6) <= version && version < v300),               // Enables the tek/splain features to make the compiler print implicit resolution chains when no implicit value can be found
  ScalacOption("-Vtype-diffs", isSupported = version => ScalaVersion(2, 13, 6) <= version && version < v300),              // Enables the tek/splain features to turn type error messages (found: X, required: Y) into colored diffs between the two types
  ScalacOption("-Ypartial-unification", isSupported = version => ScalaVersion(2, 11, 9) <= version && version < v213)      // Enable partial unification in type constructor inference
)
// format: off

def scalacOptionsFor(scalaVersion: String): Seq[String] = {
  val commonOpts = Seq("-encoding", "utf8")
  val scalaVer = ScalaVersion(scalaVersion)
  val versionedOpts = allScalacOptions.filter(_.isSupported(scalaVer)).map(_.name)

  commonOpts ++ versionedOpts
}
