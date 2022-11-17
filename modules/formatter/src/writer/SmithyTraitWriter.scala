package smithytranslate
package formatter
package writers

import ast.{
  ApplyStatement,
  ApplyStatementBlock,
  ApplyStatementSingular,
  SmithyTrait,
  SmithyTraitBodyValue,
  TraitBody,
  TraitStatements,
  TraitStructure,
  TraitStructureKeyValuePair
}
import ast.SmithyTraitBodyValue.{NodeValueCase, SmithyTraitStructureCase}
import util.string_ops.formatEnum
import NodeWriter.nodeValueWriter
import ShapeIdWriter.shapeIdWriter
import WhiteSpaceWriter.{breakWriter, wsWriter}
import Writer.{WriterOps, WriterOpsIterable}

object SmithyTraitWriter {
  implicit val traitStatementsWriter: Writer[TraitStatements] = Writer.write {
    case TraitStatements(traits, ws) =>
      traits.writeN("", "\n", "\n") + ws.write
  }

  implicit val traitWriter: Writer[SmithyTrait] = Writer.write {
    case SmithyTrait(shapeId, traitBody) =>
      val id = shapeId.write
      val body = traitBody.write
      val reformatted = if (id.equalsIgnoreCase("enum")) {
        formatEnum(body)
      } else body
      s"@$id$reformatted"
  }
  implicit val traitBodyWriter: Writer[TraitBody] = Writer.write {
    case TraitBody(ws0, traitBodyValue, ws1) =>
      s"(${ws0.write}${traitBodyValue.write}${ws1.write})"
  }
  implicit val traitBodyValueWriter: Writer[SmithyTraitBodyValue] =
    Writer.write {
      case SmithyTraitStructureCase(traitStructure) =>
        traitStructure.write
      case NodeValueCase(nodeValue) =>
        nodeValue.write
    }
  implicit val traitStructureWriter: Writer[TraitStructure] = Writer.write {
    case TraitStructure(traitStructureKVP, wsCommaDelimited) =>
      s"${traitStructureKVP.write}${wsCommaDelimited.writeN(", ", ", ", "")}"
  }
  implicit val traitStructureKVPWriter: Writer[TraitStructureKeyValuePair] =
    Writer.write {
      case TraitStructureKeyValuePair(nodeObjectKey, ws0, ws1, nodeValue) =>
        showKeyValue(nodeObjectKey, ws0, ws1, nodeValue)
    }

  implicit val applySingularWriter: Writer[ApplyStatementSingular] =
    Writer.write { case ApplyStatementSingular(ws0, shapeId, ws1, strait, br) =>
      s"apply ${shapeId.write} ${ws0.write}${strait.write}${ws1.write}${br.write}"
    }
  implicit val applyBlockWriter: Writer[ApplyStatementBlock] = Writer.write {
    case ApplyStatementBlock(shapeId, ws0, traitStatements, br) =>
      s"apply ${shapeId.write} ${ws0.write}{${traitStatements.write}}${br.write}"
  }
  implicit val applyStatementWriter: Writer[ApplyStatement] = Writer.write {
    _.either.write
  }

}
