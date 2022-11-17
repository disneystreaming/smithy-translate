package smithytranslate.formatter.parsers

import smithytranslate.formatter.ast.node_parser.{node_object_key, node_value}
import smithytranslate.formatter.ast.whitespace_parser.{br, sp, sp0}
import cats.parse.{Parser, Parser0}
import smithytranslate.formatter.ast.MetadataStatement
import smithytranslate.formatter.ast.MetadataSection

object MetadataParser {
  val metadata_statement: Parser[MetadataStatement] =
    (((Parser.ignoreCase("metadata") ~ sp) *> node_object_key <* (sp0 ~ Parser
      .char(
        '='
      )) ~ sp0) ~ node_value ~ br)
      .map { case ((nok, nv), br) =>
        MetadataStatement(nok, nv, br)
      }
  val metadata_section: Parser0[MetadataSection] =
    metadata_statement.rep0.map(MetadataSection.apply)
}

/*
MetadataSection =
 *(MetadataStatement)

MetadataStatement =
    %s"metadata" SP NodeObjectKey *SP "=" *SP NodeValue BR

metadata_statement =
    "metadata" ws node_object_key ws "=" ws node_value br
 */
