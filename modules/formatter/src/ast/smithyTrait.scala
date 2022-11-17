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
    traitBodyValue: SmithyTraitBodyValue,
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
