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
    case MetadataSection(metadata) => metadata.writeN
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
