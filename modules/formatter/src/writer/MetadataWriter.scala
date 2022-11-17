package smithytranslate
package formatter
package writers

import ast.{MetadataSection, MetadataStatement}
import NodeWriter.{nodeObjectKeyWriter, nodeValueWriter}
import WhiteSpaceWriter.breakWriter
import Writer.{WriterOps, WriterOpsIterable}

object MetadataWriter {
  implicit val metadataStatementWriter: Writer[MetadataStatement] =
    Writer.write { case MetadataStatement(nok, nv, br) =>
      s"metadata ${nok.write} = ${nv.write}${br.write}"
    }
  implicit val metadataSectionWriter: Writer[MetadataSection] = Writer.write {
    case MetadataSection(metadata) =>
      metadata.writeN("", "", "\n")
  }

  /*
  MetadataSection =
   *(MetadataStatement)

  MetadataStatement =
      %s"metadata" SP NodeObjectKey *SP "=" *SP NodeValue BR

  metadata_statement =
      "metadata" ws node_object_key ws "=" ws node_value br
   */

}
