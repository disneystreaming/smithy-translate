package smithytranslate.json_schema

final class PrimitiveSpec extends munit.FunSuite {

  test("freeform document") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "TestMap",
                           |  "type": "object",
                           |  "additionalProperties": true
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |document TestMap
                            |""".stripMargin

    TestUtils.runConversionTest(jsonSchString, expectedString)
  }
}
