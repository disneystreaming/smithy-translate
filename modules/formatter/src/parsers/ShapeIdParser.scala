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

import smithytranslate.formatter.ast.{
  AbsoluteRootShapeId,
  Identifier,
  IdentifierChar,
  IdentifierStart,
  Namespace,
  RootShapeId,
  ShapeId,
  ShapeIdMember,
  Underscore
}
import cats.parse.Parser
import cats.parse.Rfc5234.{alpha, digit}

object ShapeIdParser {
  val underscore: Parser[Underscore] = Parser.charIn('_').as(Underscore)
  val identifier_chars: Parser[IdentifierChar] =
    (Parser.charIn('_') | digit | alpha).map(IdentifierChar)
  val identifier_start: Parser[IdentifierStart] =
    (underscore.rep0.with1 ~ alpha).map(IdentifierStart.tupled)
  val identifier: Parser[Identifier] =
    (identifier_start ~ identifier_chars.rep0).map(Identifier.tupled)
  val shape_id_member: Parser[ShapeIdMember] =
    (Parser.char('$') *> identifier).map(ShapeIdMember)
  val namespace: Parser[Namespace] =
    (identifier ~ (Parser.charIn('.') *> identifier).rep0)
      .map(Namespace.tupled)
  val absolute_root_shape_id: Parser[AbsoluteRootShapeId] =
    ((namespace <* Parser.char('#')) ~ identifier)
      .map(AbsoluteRootShapeId.apply.tupled)
  val root_shape_id: Parser[RootShapeId] =
    absolute_root_shape_id.backtrack
      .eitherOr(identifier)
      .map(either => RootShapeId(either.swap))
  val shape_id: Parser[ShapeId] =
    (root_shape_id ~ shape_id_member.backtrack.?).map(ShapeId.tupled)
}

/*
  shape_id =
      root_shape_id [shape_id_member]

  root_shape_id =
      absolute_root_shape_id / identifier

  absolute_root_shape_id =
      namespace "#" identifier

  namespace =
      identifier *("." identifier)

  identifier =
      identifier_start *identifier_chars

  identifier_start =
 *"_" ALPHA

  identifier_chars =
      ALPHA / DIGIT / "_"

  shape_id_member =
      "$" identifier
 */
