package smithytranslate.json_schema

final class PrimitiveSpec extends munit.FunSuite {

  test("freeform document") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "FreeForm",
                           |  "type": "object",
                           |  "additionalProperties": true
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |document FreeForm
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }
  test("freeform document nested") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "FreeFormWrapper",
                           |  "type": "object",
                           |  "properties": {
                           |    "freeform": {
                           |       "type": "object",
                           |       "additionalProperties": true
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |structure FreeFormWrapper {
                            |  freeform: Document
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }
}
