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

package smithytranslate.runners.openapi

import software.amazon.smithy.model.Model
import smithytranslate.compiler.ToSmithyResult
import smithytranslate.runners.SmithyModelUtils._

object ProcessResult {
  def apply(
      result: ToSmithyResult[Model],
      outputJson: Boolean
  ): Either[String, Map[String, String]] = {
    result match {
      case ToSmithyResult.Failure(error, modelErrors) =>
        val message = if (modelErrors.isEmpty) {
          s"An error occurred while importing your Open API resources.: ${error.getMessage()}"
        } else {
          val errorsSummary = modelErrors.map(_.getMessage).mkString("\n")
          s"""|Failed to validate the produced Smithy model. The following is a list of
              |error messages followed by the validation exception from Smithy:
              |$errorsSummary""".stripMargin
        }
        Left(message)
      case ToSmithyResult.Success(_, model) =>
        val smithyFiles =
          if (outputJson) getSmithyJsonFiles(model) else getSmithyFiles(model)
        Right(smithyFiles.map { case (path, contents) =>
          (path.toString, contents)
        })
    }
  }

}
