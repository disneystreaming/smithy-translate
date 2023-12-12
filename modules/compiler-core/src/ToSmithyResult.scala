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

import cats.Functor

// Either the smithy validation fails, in which case we get a left with
// the list of errors. Or smithy validation works and we get a pair of
// errors (that still pass smithy validation) and a smithy model
sealed trait ToSmithyResult[+A]

object ToSmithyResult {

  final case class Failure[A](cause: Throwable, errors: List[ToSmithyError])
      extends ToSmithyResult[A]
  final case class Success[A](error: List[ToSmithyError], value: A)
      extends ToSmithyResult[A]

  implicit val functor: Functor[ToSmithyResult] =
    new Functor[ToSmithyResult]() {

      override def map[A, B](
          fa: ToSmithyResult[A]
      )(f: A => B): ToSmithyResult[B] = fa match {
        case Failure(cause, errors) => Failure(cause, errors)
        case Success(error, value)  => Success(error, f(value))
      }

    }
}
