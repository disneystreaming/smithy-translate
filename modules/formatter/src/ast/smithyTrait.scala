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
package ast

import NodeValue.NodeObjectKey

case class TraitStatements(
    list: List[(Whitespace, SmithyTrait)],
    after: Whitespace
)

case class SmithyTrait(shapeId: ShapeId, traitBody: Option[TraitBody])

case class TraitBody(
    ws0: Whitespace,
    traitBodyValue: Option[SmithyTraitBodyValue],
    ws1: Whitespace
)

sealed trait SmithyTraitBodyValue

object SmithyTraitBodyValue {
  case class SmithyTraitStructureCase(traitStructure: TraitStructure)
      extends SmithyTraitBodyValue

  case class NodeValueCase(nodeValue: NodeValue) extends SmithyTraitBodyValue
}

case class TraitStructure(
    traitStructureKVP: TraitStructureKeyValuePair,
    additionalTraits: List[(Whitespace, TraitStructureKeyValuePair)]
)

case class TraitStructureKeyValuePair(
    nodeObjectKey: NodeObjectKey,
    ws0: Whitespace,
    ws1: Whitespace,
    nodeValue: NodeValue
)
//ApplyStatement =
//    (ApplyStatementSingular / ApplyStatementBlock)
//
//ApplyStatementSingular =
//    %s"apply" WS ShapeId WS Trait BR
//
//ApplyStatementBlock =
//    %s"apply" SP ShapeId WS "{" TraitStatements "}" BR

case class ApplyStatementSingular(
    ws0: Whitespace,
    shapeId: ShapeId,
    ws1: Whitespace,
    strait: SmithyTrait,
    break: Break
)
case class ApplyStatementBlock(
    shapeId: ShapeId,
    ws0: Whitespace,
    traitStatements: TraitStatements,
    break: Break
)
case class ApplyStatement(
    either: Either[ApplyStatementSingular, ApplyStatementBlock]
)
