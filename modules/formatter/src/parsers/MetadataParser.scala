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
package smithytranslate
package formatter
package parsers

import smithytranslate.formatter.parsers.NodeParser.{
  node_object_key,
  node_value
}
import smithytranslate.formatter.parsers.WhitespaceParser.{br, sp, sp0}
import cats.parse.{Parser, Parser0}
import smithytranslate.formatter.ast.MetadataStatement
import smithytranslate.formatter.ast.MetadataSection

object MetadataParser {
  private val metadata = Parser.ignoreCase("metadata")
  val metadata_statement: Parser[MetadataStatement] =
    ((((metadata ~ sp) *> node_object_key <* (sp0 ~ equal) ~ sp0) ~ node_value) ~ br.?)
      .map { case ((nok, nv), br) =>
        MetadataStatement(nok, nv, br)
      }

  val metadata_section: Parser0[MetadataSection] =
    metadata_statement.rep0.map(MetadataSection(_))
}

/*
MetadataSection =
 *(MetadataStatement)

MetadataStatement =
    %s"metadata" SP NodeObjectKey *SP "=" *SP NodeValue BR

metadata_statement =
    "metadata" ws node_object_key ws "=" ws node_value br
 */
