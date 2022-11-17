package smithytranslate
package formatter
package writers

import ast.Idl
import ControlWriter.controlSectionWriter
import MetadataWriter.metadataSectionWriter
import ShapeWriter.shapeSectionWriter
import WhiteSpaceWriter.wsWriter
import Writer.WriterOps

object IdlWriter {
  implicit val idlWriter: Writer[Idl] = Writer.write { idl =>
    s"${idl.ws.write}${idl.control.write}${idl.metadata.write}${idl.shape.write}"
  }

}
