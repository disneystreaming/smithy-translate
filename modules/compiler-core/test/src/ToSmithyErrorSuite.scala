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
