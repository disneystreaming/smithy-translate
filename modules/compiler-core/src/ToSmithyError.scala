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

package smithytranslate.compiler

import scala.util.control.NoStackTrace
import cats.data.NonEmptyChain
import cats.syntax.all._
import software.amazon.smithy.model.validation.ValidationEvent
import java.net.URI

sealed trait ToSmithyError extends Throwable

object ToSmithyError {

  implicit val order: cats.Order[ToSmithyError] = cats.Order.by(_.getMessage())

  final case class Restriction(message: String) extends ToSmithyError {
    override def getMessage(): String = message
  }

  final case class ProcessingError(message: String, errorCause: Option[Throwable] = None) extends ToSmithyError {
    override def getMessage(): String = message
    override def getCause(): Throwable = errorCause.orNull
  }
    
  final case class HttpError(uri: URI, refStack: List[String], error: Throwable) extends ToSmithyError {
    override def getCause(): Throwable = error
    override def getMessage(): String = "Failed to fetch remote schema from " + uri.toString + ". Error: " + error.getMessage
  }

  final case class SmithyValidationFailed(
      smithyValidationEvents: List[ValidationEvent]
  ) extends ToSmithyError {
    override def getMessage(): String = {
      s"Failed to validate the Smithy model:\n${smithyValidationEvents.mkString("\n")}"
    }
  }

  final case class BadRef(ref: String) extends ToSmithyError {
    override def getMessage(): String = s"Unable to parse ref string: $ref"
  }
  
  final case class OpenApiParseError(
      namespace: NonEmptyChain[String],
      errorMessages: List[String]
  ) extends ToSmithyError
      with NoStackTrace {
    override def getMessage(): String =
      s"Unable to parse openapi file located at ${namespace.mkString_("/")} with errors: ${errorMessages
          .mkString(", ")}"
  }
}
