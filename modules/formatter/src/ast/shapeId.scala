package smithytranslate.formatter.ast

case class ShapeId(
    rootShapeId: RootShapeId,
    shapeIdMember: Option[ShapeIdMember]
)

case class RootShapeId(either: Either[AbsoluteRootShapeId, Identifier])

case class AbsoluteRootShapeId(nameSpace: Namespace, identifier: Identifier)

case class Namespace(identifier: Identifier, suffix: List[Identifier])

case class Identifier(start: IdentifierStart, chars: List[IdentifierChar])

case class IdentifierStart(underScores: List[Underscore], char: Char)

case class ShapeIdMember(identifier: Identifier)

case class IdentifierChar(
    char: Char
)
