import mill._
import mill.define._
import mill.scalalib._

object alloy {
  val alloyVersion = "dev-SNAPSHOT"
  // val alloyVersion = "0.2.8"
  val core =
    ivy"com.disneystreaming.alloy:alloy-core:$alloyVersion"
  val protobuf =
    ivy"com.disneystreaming.alloy:alloy-protobuf:$alloyVersion"
}
object circe {
  val jawn = ivy"io.circe::circe-jawn:0.14.6"
}
object everit {
  val jsonSchema = ivy"com.github.erosb:everit-json-schema:1.14.3"
}
val slf4j =
  ivy"org.slf4j:slf4j-nop:2.0.9" // needed since swagger-parser relies on slf4j-api
object swagger {
  val parser = Agg(
    ivy"io.swagger.parser.v3:swagger-parser:2.1.19",
    // included to override the version brought in by swagger-parser which has a vulnerability
    ivy"org.mozilla:rhino:1.7.14"
  )
}
object smithy {
  val smithyVersion = "1.41.1"
  val model = ivy"software.amazon.smithy:smithy-model:$smithyVersion"
  val build = ivy"software.amazon.smithy:smithy-build:$smithyVersion"
}
object cats {
  val mtl = ivy"org.typelevel::cats-mtl:1.4.0"
  val parse = ivy"org.typelevel::cats-parse:1.0.0"
}
val ciString = ivy"org.typelevel::case-insensitive:1.4.0"
val decline = ivy"com.monovore::decline:2.4.1"
object lihaoyi {
  val oslib = ivy"com.lihaoyi::os-lib:0.9.2"
  val ujson = ivy"com.lihaoyi::ujson:3.1.3"
}

val collectionsCompat =
  ivy"org.scala-lang.modules::scala-collection-compat:2.11.0"

val scalaJavaCompat = ivy"org.scala-lang.modules::scala-java8-compat:1.0.2"

val munitVersion = "1.0.0-M10"
object grpc {
  val version = "1.59.1"
  val netty = ivy"io.grpc:grpc-netty:$version"
  val services = ivy"io.grpc:grpc-services:$version"
}
object scalapb {
  val version = "0.11.14"
  val runtimeGrpc = ivy"com.thesamet.scalapb::scalapb-runtime-grpc:$version"
  val compilerPlugin =
    ivy"com.thesamet.scalapb::compilerplugin:$version"
  val protocCache = ivy"com.thesamet.scalapb::protoc-cache-coursier:0.9.6"
}
val coursier = ivy"io.get-coursier::coursier:2.1.8"
