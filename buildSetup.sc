import $ivy.`com.lihaoyi::mill-contrib-bloop:`
import $ivy.`com.lihaoyi::mill-contrib-scalapblib:`
import $ivy.`com.lihaoyi::mill-contrib-buildinfo:`
import $ivy.`com.lewisjkl::header-mill-plugin::0.0.3`
import $file.buildDeps

import header._
import $file.plugins.ci.CiReleaseModules
import CiReleaseModules.{CiReleaseModule, SonatypeHost, ReleaseModule}
import mill._
import mill.scalajslib.api.ModuleKind
import mill.scalajslib.ScalaJSModule
import mill.scalalib._
import mill.scalalib.publish._
import mill.scalalib.scalafmt.ScalafmtModule
import mill.contrib.buildinfo.BuildInfo
import mill.scalalib.api.ZincWorkerUtil

import scala.Ordering.Implicits._

object ScalaVersions {
  val scala212 = "2.12.18"
  val scala213 = "2.13.12"
  val scala3 = "3.3.1"

  val scalaVersions = List(scala213, scala212, scala3)
}

trait BaseModule extends Module with HeaderModule {

  def millSourcePath: os.Path = {
    val originalRelativePath = super.millSourcePath.relativeTo(os.pwd)
    os.pwd / "modules" / originalRelativePath
  }

  def includeFileExtensions: List[String] = List("scala", "java")
  def license: HeaderLicense = HeaderLicense.Custom(
    """|Copyright 2022 Disney Streaming
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
       |""".stripMargin
  )
}

trait BasePublishModule extends BaseModule with CiReleaseModule {

  def publishArtifactName: T[String]
  override final def artifactName = publishArtifactName

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

trait BaseScala213Module extends BaseScalaModule {
  override def scalaVersion = T.input(ScalaVersions.scala213)
}

trait BaseScalaModule extends ScalaModule with BaseModule with ScalafmtModule {

  override def scalacPluginIvyDeps = T {
    val sv = scalaVersion()
    val plugins =
      if (sv.startsWith("2.")) Agg(ivy"org.typelevel:::kind-projector:0.13.2")
      else Agg.empty
    super.scalacPluginIvyDeps() ++ plugins
  }

  def scalacOptions = T {
    super.scalacOptions() ++ scalacOptionsFor(scalaVersion())
  }
}

trait BaseScalaJSModule extends BaseScalaModule with ScalaJSModule {
  def scalaJSVersion = "1.11.0"
  def moduleKind = ModuleKind.CommonJSModule
}

trait BaseJavaModule extends BaseModule with JavaModule {}

trait BaseMunitTests extends ScalafmtModule with TestModule.Munit {
  def ivyDeps =
    Agg(
      ivy"org.scalameta::munit::${buildDeps.munitVersion}",
      ivy"org.scalameta::munit-scalacheck::${buildDeps.munitVersion}"
    )
}

case class ScalaVersion(maj: Int, min: Int, patch: Int)
object ScalaVersion {
  def apply(scalaVersion: String): ScalaVersion = scalaVersion match {
    case ZincWorkerUtil.ReleaseVersion(major, minor, patch) =>
      ScalaVersion(major.toInt, minor.toInt, patch.toInt)
    case ZincWorkerUtil.MinorSnapshotVersion(major, minor, patch) =>
      ScalaVersion(major.toInt, minor.toInt, patch.toInt)
    case ZincWorkerUtil.DottyVersion("0", minor, patch) =>
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
  ScalacOption("-Xsource:3", isSupported = version => v211 <= version && version < v300),                                  // Treat compiler input as Scala source for the specified version, see scala/bug#8126.
  ScalacOption("-deprecation", isSupported = version => version < v213 || v300 <= version),                                // Emit warning and location for usages of deprecated APIs. Not really removed but deprecated in 2.13.
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
  ScalacOption("-Xfatal-warnings", isSupported = _ < v300),                                                                // Fail the compilation if there are any warnings. Disabled for scala3 because some warnings can't be `nowarn`ed
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
//  ScalacOption("-Wunused:imports", isSupported = version => v213 <= version && version < v300),                            // ^ Replaces the above
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
