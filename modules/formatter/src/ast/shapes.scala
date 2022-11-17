package smithytranslate
package formatter
package ast

import ast.NodeValue.NodeObject
import ast.shapes.ShapeBody.ListStatement.ListMembers
import ast.shapes.ShapeBody.MapStatement.MapMembers
import ast.shapes.ShapeBody.StructureMembers.{
  StructureMember,
  StructureMemberType
}
import cats.data.NonEmptyList

object shapes {
  // UseSection and ShapeStatements are both Optional , however encoded via Lists . see https://github.com/awslabs/smithy/issues/1249
  case class ShapeSection(
      all: Option[
        (NamespaceStatement, UseSection, ShapeStatements)
      ]
  )

  case class NamespaceStatement(
      namespace: Namespace,
      break: Break
  )

  case class UseSection(uses: List[UseStatement])

  case class UseStatement(
      absoluteRootShapeId: AbsoluteRootShapeId,
      break: Break
  )

  case class ShapeStatements(statements: List[ShapeStatementsCase])

  sealed trait ShapeStatementsCase

  object ShapeStatementsCase {
    case class ShapeStatementCase(statement: ShapeStatement)
        extends ShapeStatementsCase

    case class ApplyStatementCase(apply: ApplyStatement)
        extends ShapeStatementsCase
  }

  case class ShapeStatement(
      traitStatements: TraitStatements,
      shapeBody: ShapeBody,
      br: Break
  )

  /*  case class ShapeMemberKvp(
      traitStatements: TraitStatements,
      identifier: Identifier,
      ws0: Whitespace,
      ws1: Whitespace,
      shapeId: ShapeId
  )

  sealed trait ShapeMembers

  object ShapeMembers {
    case class Empty(ws: Whitespace) extends ShapeMembers

    case class Populated(
        ws0: Whitespace,
        shapeMemberKvp: ShapeMemberKvp,
        ws1: Whitespace,
        additionalMembers: List[(Comma, ShapeMemberKvp, Whitespace)]
    ) extends ShapeMembers
  }*/

  sealed trait ShapeBody

  object ShapeBody {
    case class SimpleTypeName(name: String) extends AnyVal

    case class SimpleShapeStatement(
        decl: SimpleTypeName,
        identifier: Identifier,
        mixins: Option[Mixin]
    ) extends ShapeBody

    case class EnumShapeStatement(
        typeName: String,
        id: Identifier,
        mixin: Option[Mixin],
        whitespace: Whitespace,
        enumShapeMembers: EnumShapeMembers
    ) extends ShapeBody

    case class ListStatement(
        identifier: Identifier,
        mixin: Option[Mixin],
        whitespace: Whitespace,
        members: ListMembers
    ) extends ShapeBody

    object ListStatement {
      sealed trait ListMemberType

      case class ElidedListMember(member: ShapeIdMember) extends ListMemberType

      case class ExplicitListMember(shapeId: ShapeId) extends ListMemberType

      case class ListMembers(
          ws0: Whitespace,
          members: ListMember,
          ws1: Whitespace
      )

      case class ListMember(
          traitStatements: TraitStatements,
          listMemberType: ListMemberType
      )
    }

    case class MapStatement(
        identifier: Identifier,
        mixin: Option[Mixin],
        whitespace: Whitespace,
        members: MapMembers
    ) extends ShapeBody

    object MapStatement {
      case class MapKey(
          traitStatements: Option[TraitStatements],
          mapKeyType: MapKeyType
      )

      sealed trait MapKeyType

      object MapKeyType {
        case class ElidedMapKey(name: ShapeIdMember) extends MapKeyType

        case class ExplicitMapKey(shapeId: ShapeId) extends MapKeyType
      }

      case class MapValue(
          traitStatements: Option[TraitStatements],
          mapValueType: MapValueType
      )

      sealed trait MapValueType

      object MapValueType {
        case class ElidedMapValue(member: ShapeIdMember) extends MapValueType

        case class ExplicitMapValue(shapeId: ShapeId) extends MapValueType
      }

      case class MapMembers(
          ws0: Whitespace,
          mapKey: MapKey,
          break: Break,
          mapValue: MapValue,
          ws1: Whitespace
      )
    }

    case class StructureStatement(
        identifier: Identifier,
        resource: Option[StructureResource],
        mixins: Option[Mixin],
        whitespace: Whitespace,
        members: StructureMembers
    ) extends ShapeBody

    case class StructureResource(shapeId: ShapeId)

    case class StructureMembers(
        ws0: Whitespace,
        members: List[(TraitStatements, StructureMember, Whitespace)]
    )

    object StructureMembers {
      sealed trait StructureMemberType

      object StructureMemberType {
        case class ElidedStructureMember(identifier: Identifier)
            extends StructureMemberType

        case class ExplicitStructureMember(
            identifier: Identifier,
            shapeId: ShapeId
        ) extends StructureMemberType
      }

      case class StructureMember(
          structureMemberType: StructureMemberType,
          valueAssignment: Option[ValueAssignment]
      )
    }

    case class UnionStatement(
        identifier: Identifier,
        mixin: Option[Mixin],
        whitespace: Whitespace,
        members: UnionMembers
    ) extends ShapeBody

    case class UnionMembers(
        whitespace: Whitespace,
        members: List[(TraitStatements, UnionMember, Whitespace)]
    )

    case class UnionMember(structureMemberType: StructureMemberType)

    case class ServiceStatement(
        identifier: Identifier,
        mixin: Option[Mixin],
        whitespace1: Whitespace,
        nodeObject: NodeObject
    ) extends ShapeBody

    case class ResourceStatement(
        identifier: Identifier,
        mixin: Option[Mixin],
        whitespace: Whitespace,
        nodeObject: NodeObject
    ) extends ShapeBody

    case class InlineStructure(
        whitespace: Whitespace,
        traitStatements: TraitStatements,
        mixin: Option[Mixin],
        ws1: Whitespace,
        members: StructureMembers
    )

    case class OperationBody(
        whitespace: Whitespace,
        input: Option[OperationInput],
        output: Option[OperationOutput],
        errors: Option[OperationErrors],
        ws1: Whitespace
    )

    case class OperationInput(
        whitespace: Whitespace,
        either: Either[InlineStructure, (Whitespace, ShapeId)],
        whitespace1: Whitespace
    )

    case class OperationOutput(
        whitespace: Whitespace,
        either: Either[InlineStructure, (Whitespace, ShapeId)],
        whitespace1: Whitespace
    )

    case class OperationErrors(
        ws0: Whitespace,
        ws1: Whitespace,
        list: List[(Whitespace, Identifier)],
        ws2: Whitespace
    )

    case class OperationStatement(
        identifier: Identifier,
        mixin: Option[Mixin],
        whitespace: Whitespace,
        operationBody: OperationBody
    ) extends ShapeBody

    case class Mixin(
        whitespace: Whitespace,
        shapeIds: NonEmptyList[(Whitespace, ShapeId)],
        whitespace1: Whitespace
    )

    case class EnumShapeMembers(
        whitespace: Whitespace,
        members: NonEmptyList[
          (TraitStatements, Identifier, Option[ValueAssignment], Whitespace)
        ]
    )

    case class ValueAssignment(value: NodeValue, break: Break)
  }
}
