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

import cats.parse.Parser
import cats.parse.Parser._
import cats.parse.Rfc5234._
import cats.syntax.all._
import cats.data.NonEmptyList
import cats.parse.Parser0

object ReadmeParser {

  type ParserError = String

  sealed trait Example extends Product with Serializable
  object Example {
    final case class OpenApi(openapi: String, smithy: String) extends Example
    final case class JsonSchema(json: String, smithy: String) extends Example
    final case class Proto(smithy: String, proto: NonEmptyList[String])
        extends Example
  }
  final case class ParserResult(examples: List[Example])

  private val tripleTick = string("```")

  private def header(title: String, language: String): Parser[Unit] =
    (Parser.ignoreCase(title) ~ lf ~ tripleTick ~ string(language)).void

  private def part(
      header: Parser[Unit],
      maybeRepeatStopper: Option[Parser0[Unit]] = None
  ): Parser[NonEmptyList[String]] = {
    val body = anyChar.repUntil(tripleTick).string <* tripleTick
    val p: Parser0[Unit] => Parser[String] = stopper =>
      (anyChar.repUntil(stopper) ~ header *> body)
    maybeRepeatStopper match {
      case Some(stopper) => p(header | stopper).backtrack.rep
      case None          => p(header).map(NonEmptyList.of(_))
    }
  }

  private def sectionParser(
      inputHeader: Parser[Unit],
      outputHeader: Parser[Unit],
      maybeOutputRepeatStopper: Option[Parser0[Unit]] = None
  ): Parser[(String, NonEmptyList[String])] =
    part(inputHeader).map(_.head) ~ part(
      outputHeader,
      maybeOutputRepeatStopper
    )

  def parse(input: String): Either[ParserError, ParserResult] = {
    val openapiHeader = header("OpenAPI:", "yaml")
    val smithyHeader = header("Smithy:", "kotlin")
    val protoHeader = header("Proto:", "proto")
    val jsonHeader = header("JSON Schema:", "json")

    val openapiSection =
      sectionParser(openapiHeader, smithyHeader)
        .map(_.map(_.head))
        .map(Example.OpenApi.tupled)
    val jsonSection =
      sectionParser(jsonHeader, smithyHeader)
        .map(_.map(_.head))
        .map(Example.JsonSchema.tupled)
    val protoSection =
      sectionParser(smithyHeader, protoHeader, Some(string("Smithy:"))).map(
        Example.Proto.tupled
      )
    val section = openapiSection.backtrack
      .orElse(protoSection)
      .backtrack
      .orElse(jsonSection)
      .backtrack
    val exampleParser =
      section.rep <* lf.rep0 // possible empty new lines at end of file
    exampleParser
      .parseAll(input)
      .bimap(_.toString, in => ParserResult(in.toList))
  }

}
