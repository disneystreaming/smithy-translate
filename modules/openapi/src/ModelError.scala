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

package smithytranslate.openapi

import scala.util.control.NoStackTrace
import cats.data.NonEmptyChain
import cats.syntax.all._

sealed trait ModelError extends Throwable

object ModelError {

  implicit val order: cats.Order[ModelError] = cats.Order.by(_.getMessage())

  case class Restriction(message: String) extends ModelError {
    override def getMessage(): String = message
  }

  case class ProcessingError(message: String) extends ModelError {
    override def getMessage(): String = message
  }

  case class SmithyValidationFailed(
      smithyValidationError: Throwable,
      otherErrors: List[ModelError]
  ) extends ModelError {
    override def getMessage(): String = {
      val otherMessages = otherErrors.map(_.getMessage).mkString("\n")
      "Failed to validate the Smithy model. Previous error message follows:\n" +
        otherMessages
    }
    override def getCause(): Throwable = smithyValidationError

  }

  case class BadRef(ref: String) extends ModelError

  case class OpenApiParseError(
      namespace: NonEmptyChain[String],
      errorMessages: List[String]
  ) extends ModelError
      with NoStackTrace {
    override def getMessage(): String =
      s"Unable to parse openapi file located at ${namespace.mkString_("/")} with errors: ${errorMessages
          .mkString(", ")}"
  }

}
