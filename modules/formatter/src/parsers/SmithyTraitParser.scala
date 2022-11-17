package smithytranslate.formatter.parsers

import smithytranslate.formatter.ast.{
  closeCurly,
  closeParentheses,
  openCurly,
  openParentheses,
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
import smithytranslate.formatter.ast.SmithyTraitBodyValue.{
  NodeValueCase,
  SmithyTraitStructureCase
}
import smithytranslate.formatter.ast.node_parser.{node_object_key, node_value}
import smithytranslate.formatter.ast.whitespace_parser.{br, sp, ws}
import cats.parse.{Parser, Parser0}
import smithytranslate.formatter.parsers.ShapeIdParser.shape_id

object SmithyTraitParser {

  //    node_object_key ws ":" ws node_value
  val trait_structure_kvp: Parser[TraitStructureKeyValuePair] =
    ((node_object_key ~ ws <* Parser.char(':')) ~ ws ~ node_value).map {
      case (((a, b), c), d) => TraitStructureKeyValuePair(a, b, c, d)
    }
  //       TraitStructureKvp *(*WS TraitStructureKvp)
  val trait_structure: Parser[TraitStructure] =
    (trait_structure_kvp ~ (ws.with1 ~ trait_structure_kvp).backtrack.rep0)
      .map(TraitStructure.tupled)

  //    trait_structure / node_value
  val strait_body_value: Parser0[SmithyTraitBodyValue] =
    trait_structure.backtrack.map(SmithyTraitStructureCase) | node_value.map(
      NodeValueCase
    )
  //  "(" ws trait_body_value ws ")"
  val strait_body: Parser0[TraitBody] =
    ((openParentheses *> ws ~ strait_body_value ~ ws) <* closeParentheses)
      .map { case ((a, b), c) =>
        TraitBody(a, b, c)
      }
  //  "@" shape_id [trait_body]
  val strait: Parser[SmithyTrait] =
    Parser.char('@') *> (shape_id ~ strait_body.?).map { SmithyTrait.tupled }
  // *(ws trait) ws
  val trait_statements: Parser0[TraitStatements] =
    ((ws.with1 ~ strait).backtrack.rep0 ~ ws).map { case (traits, ws) =>
      TraitStatements(traits, ws)
    }
  val apply_singular: Parser[ApplyStatementSingular] =
    (Parser.string("apply") *> (ws ~ shape_id ~ ws ~ strait ~ br)).map {
      case ((((a, b), c), d), e) => ApplyStatementSingular(a, b, c, d, e)
    }

  val apply_block: Parser[ApplyStatementBlock] =
    Parser.string(
      "apply"
    ) *> ((sp *> shape_id ~ ws <* openCurly) ~ trait_statements ~ (closeCurly *> br))
      .map { case (((a, b), c), d) =>
        ApplyStatementBlock(a, b, c, d)
      }
  val apply_statement: Parser[ApplyStatement] =
    apply_block.backtrack.eitherOr(apply_singular).map(ApplyStatement)

}

/*
TraitStatements =
 *(*WS Trait) *WS

Trait =
    "@" ShapeId [TraitBody]

TraitBody =
    "(" *WS [TraitBodyValue] *WS ")"

TraitBodyValue =
    TraitStructure / NodeValue

TraitStructure =
    TraitStructureKvp *(*WS TraitStructureKvp)

TraitStructureKvp =
    NodeObjectKey *WS ":" *WS NodeValue

ApplyStatement =
    (ApplyStatementSingular / ApplyStatementBlock)

ApplyStatementSingular =
    %s"apply" WS ShapeId WS Trait BR

ApplyStatementBlock =
    %s"apply" SP ShapeId WS "{" TraitStatements "}" BR
 */
