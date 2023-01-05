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
import util.string_ops.indent
import NodeWriter.nodeValueWriter
import ShapeIdWriter.shapeIdWriter
import WhiteSpaceWriter.wsWriter
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
      s"@$id$body"
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
    Writer.write { case ApplyStatementSingular(ws0, shapeId, ws1, strait) =>
      s"apply ${shapeId.write} ${ws0.write}${strait.write}${ws1.write}"
    }
  implicit val applyBlockWriter: Writer[ApplyStatementBlock] = Writer.write {
    case ApplyStatementBlock(shapeId, ws0, traitStatements) =>
      s"apply ${shapeId.write} ${ws0.write}{\n${indent(traitStatements.write, "\n", 4)}\n}"
  }
  implicit val applyStatementWriter: Writer[ApplyStatement] = Writer.write {
    _.either.write
  }

}
