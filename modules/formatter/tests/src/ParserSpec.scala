import smithytranslate.formatter.ast.control_parser.control_section
import smithytranslate.formatter.parsers.IdlParser
import smithytranslate.formatter.parsers.MetadataParser.metadata_section

final class ParserSpec extends munit.FunSuite {
  val metadataStatement: String =
    """metadata greeting = "hello"
    metadata "stringList" = ["a", "b", "c"]
    """.stripMargin
  val controlStatement: String = """$version: "1.0"
    """.stripMargin

  test("Parse a metadata statement") {
    val result = metadata_section.parseAll(metadataStatement)
    assert(result.isRight && result.exists(_.metadata.size == 2))
  }

  test("Parse a control statement") {
    val result = control_section.parseAll(controlStatement)
    assert(result.isRight && result.exists(_.list.size == 1))
  }
  test("both") {
    val result = IdlParser.idlParser.parseAll(controlStatement + metadataStatement)
    assert(
      result.isRight && result.exists(res =>
        res.metadata.metadata.size == 2 && res.control.list.size == 1
      )
    )
  }

}