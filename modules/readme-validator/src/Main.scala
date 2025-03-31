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

object Main {

  def main(args: Array[String]): Unit =
    args.toList.foreach(inputPath => runValidation(os.Path(inputPath)))

  private def runValidation(inputPath: os.Path): Unit = {
    val input = os.read(inputPath)
    ReadmeParser.parse(input) match {
      case Left(error) =>
        System.err.print(s"Error encountered while parsing readme: $error")
      case Right(parsed) => validateAndPrintResults(parsed)
    }
  }

  private def validateAndPrintResults(
      parsed: ReadmeParser.ParserResult
  ): Unit = {
    Validator.validate(parsed) match {
      case Nil =>
        System.err.println(
          s"Successfully checked ${parsed.examples.size} examples, README is properly formatted"
        )
      case errors =>
        System.err.println(errors.mkString("\n"))
        sys.exit(1)
    }
  }
}
