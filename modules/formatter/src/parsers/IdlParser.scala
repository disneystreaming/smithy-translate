package smithytranslate
package formatter
package parsers

import cats.parse.Parser0
import smithytranslate.formatter.ast.whitespace_parser.ws
import smithytranslate.formatter.ast.Idl
import smithytranslate.formatter.ast.control_parser.control_section
import MetadataParser.metadata_section
import ShapeParser.shape_section

object IdlParser {

  val idlParser: Parser0[Idl] =
    (ws ~ control_section ~ metadata_section ~ shape_section).map {
      case (((whitespace, control), metadata), shape) =>
        Idl(whitespace, control, metadata, shape)
    }

}
