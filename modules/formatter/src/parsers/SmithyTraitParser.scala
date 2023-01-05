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

import cats.parse.{Parser, Parser0}
import smithytranslate.formatter.ast.SmithyTraitBodyValue.{
  NodeValueCase,
  SmithyTraitStructureCase
}
import smithytranslate.formatter.parsers.WhitespaceParser.{sp, ws}
import smithytranslate.formatter.ast.*
import smithytranslate.formatter.parsers.NodeParser.{
  node_object_key,
  node_value
}
import smithytranslate.formatter.parsers.ShapeIdParser.shape_id

object SmithyTraitParser {

  //    node_object_key ws ":" ws node_value
  val trait_structure_kvp: Parser[TraitStructureKeyValuePair] =
    ((node_object_key ~ ws <* colon) ~ ws ~ node_value).map {
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
    ((openParentheses *> ws ~ strait_body_value.? ~ ws) <* closeParentheses)
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
    (Parser.string("apply") *> (ws ~ shape_id ~ ws ~ strait)).map {
      case (((a, b), c), d) => ApplyStatementSingular(a, b, c, d)
    }

  val apply_block: Parser[ApplyStatementBlock] =
    (Parser.string(
      "apply"
    ) *> ((sp *> shape_id ~ ws <* openCurly) ~ trait_statements <* closeCurly))
      .map { case ((a, b), c) =>
        ApplyStatementBlock(a, b, c)
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
    ApplyStatementSingular / ApplyStatementBlock

ApplyStatementSingular =
    %s"apply" WS ShapeId WS Trait

ApplyStatementBlock =
    %s"apply" SP ShapeId WS "{" TraitStatements "}"
 */
