package smithytranslate.formatter.parsers

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
  val underscore: Parser[Underscore] = Parser.charWhere(_ == '_').as(Underscore)
  val identifier_chars: Parser[IdentifierChar] =
    (Parser.charWhere(_ == '_') | digit | alpha).map(IdentifierChar)
  val identifier_start: Parser[IdentifierStart] =
    (underscore.rep0.with1 ~ alpha).map(IdentifierStart.tupled)
  val identifier: Parser[Identifier] =
    (identifier_start ~ identifier_chars.rep0).map(Identifier.tupled)
  val shape_id_member: Parser[ShapeIdMember] =
    (Parser.char('$') *> identifier).map(ShapeIdMember)
  val namespace: Parser[Namespace] =
    (identifier ~ (Parser.charWhere(_.==('.')) *> identifier).rep0)
      .map(Namespace.tupled)
  val absolute_root_shape_id: Parser[AbsoluteRootShapeId] =
    ((namespace <* Parser.char('#')) ~ identifier)
      .map(AbsoluteRootShapeId.tupled)
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
