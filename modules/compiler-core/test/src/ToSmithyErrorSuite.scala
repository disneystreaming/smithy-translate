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

import cats.data.NonEmptyChain

final class ToSmithyErrorSuite extends munit.FunSuite {

  test("Restriction should have a message") {
    val error = ToSmithyError.Restriction("some restriction")
    assertNotEquals(Option(error.getMessage), None)
  }

  test("ProcessingError should have a message") {
    val error = ToSmithyError.ProcessingError("some processing error")
    assertNotEquals(Option(error.getMessage), None)
  }

  test("SmithyValidationFailed should have a message") {
    val error = ToSmithyError.SmithyValidationFailed(Nil)
    assertNotEquals(Option(error.getMessage), None)
  }

  test("BadRef should have a message") {
    val error = ToSmithyError.BadRef("some/ref")
    assertNotEquals(Option(error.getMessage), None)
  }

  test("OpenApiParseError should have a message") {
    val error = ToSmithyError.OpenApiParseError(
      NonEmptyChain.one("namespace"),
      List("error1", "error2")
    )
    assertNotEquals(Option(error.getMessage), None)
  }
}
