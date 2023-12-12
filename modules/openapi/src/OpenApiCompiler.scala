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

package smithytranslate.compiler.openapi

import cats.data.Chain
import cats.data.NonEmptyChain
import cats.data.NonEmptyList
import cats.syntax.all._
import io.swagger.parser.OpenAPIParser
import smithytranslate.compiler._
import smithytranslate.compiler.internals._
import smithytranslate.compiler.internals.openapi.OpenApiToIModel

import scala.jdk.CollectionConverters._

import OpenApiCompilerInput._

/** Converts openapi to a smithy model.
  */
object OpenApiCompiler extends AbstractToSmithyCompiler[OpenApiCompilerInput] {

  protected def convertToInternalModel(
      opts: ToSmithyCompilerOptions,
      input: OpenApiCompilerInput
  ): (Chain[ToSmithyError], IModel) = {
    val prepared = input match {
      case UnparsedSpecs(specs) =>
        val parser = new OpenAPIParser()
        specs.map { case FileContents(path, content) =>
          val cleanedPath = removeFileExtension(path)
          val result = parser.readContents(content, null, null)

          val parsed =
            if (
              opts.validateInput && !result
                .getMessages()
                .isEmpty()
            ) {
              Left(result.getMessages().asScala.toList)
            } else {
              // in some cases, the validation error is important enough that
              // parsing fails and `getOpenAPI` returns null. in this case
              // Left is returned with the error messages (even if failOnValidationErrors is false)
              Option(result.getOpenAPI())
                .toRight(result.getMessages().asScala.toList)
            }
          (cleanedPath, parsed)
        }
      case ParsedSpec(path, openapiModel) =>
        val cleanedPath = removeFileExtension(path)
        List(cleanedPath -> Right(openapiModel))
    }
    prepared.foldMap { case (path, parsed) =>
      OpenApiToIModel.compile(NonEmptyChain.fromNonEmptyList(path), parsed)
    }
  }

  private def removeFileExtension(
      path: NonEmptyList[String]
  ): NonEmptyList[String] = {
    val lastSplit = path.last.split('.')
    val newLast =
      if (lastSplit.size > 1) lastSplit.dropRight(1) else lastSplit
    NonEmptyList.fromListUnsafe(
      path.toList.dropRight(1) :+ newLast.mkString(".")
    )
  }

}
