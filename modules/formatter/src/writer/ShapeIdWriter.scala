package smithytranslate
package formatter
package writers

import ast.{
  AbsoluteRootShapeId,
  Identifier,
  IdentifierChar,
  IdentifierStart,
  Namespace,
  RootShapeId,
  ShapeId,
  ShapeIdMember
}

import writers.Writer.WriterOps

object ShapeIdWriter {
  implicit val identifierStartWriter: Writer[IdentifierStart] =
    Writer.write[IdentifierStart] { case IdentifierStart(underscores, char) =>
      s"${underscores.map(_ => "_").mkString}$char"
    }
  implicit val identifierCharsWriter: Writer[IdentifierChar] =
    Writer.write[IdentifierChar](_.char.write)
  implicit val identifierWriter: Writer[Identifier] = Writer.write {
    case Identifier(start, chars) =>
      s"${start.write}${chars.map(_.write).mkString}"
  }
  implicit val namespaceWriter: Writer[Namespace] = Writer.write {
    case Namespace(identifier, suffix) =>
      s"${identifier.write}${suffix.map("." + _.write).mkString}"
  }

  implicit val shapeIdMemberWriter: Writer[ShapeIdMember] =
    Writer.write[ShapeIdMember](shapeIdMember => {
      "$" + shapeIdMember.identifier.write
    })
  implicit val absoluteRootShapeIdWriter: Writer[AbsoluteRootShapeId] =
    Writer.write[AbsoluteRootShapeId] {
      case AbsoluteRootShapeId(namespace, identifier) =>
        s"${namespace.write}#${identifier.write}"
    }
  implicit val rootShapeIdWriter: Writer[RootShapeId] =
    Writer.write[RootShapeId] { root =>
      root.either match {
        case Left(absoluteRootShapeId) => absoluteRootShapeId.write
        case Right(identifier)         => identifier.write
      }
    }

  implicit val shapeIdWriter: Writer[ShapeId] = Writer.write[ShapeId] {
    case ShapeId(rootShapeId, shapeIdMember) =>
      s"${rootShapeId.write}${shapeIdMember.map(_.write).getOrElse("")}"
  }
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
