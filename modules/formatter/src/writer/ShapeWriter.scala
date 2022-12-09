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

import ast.Comment
import ast.shapes._
import ast.shapes.ShapeBody._
import ast.shapes.ShapeBody.ListStatement._
import ast.shapes.ShapeBody.MapStatement._
import ast.shapes.ShapeBody.MapStatement.MapKeyType.{
  ElidedMapKey,
  ExplicitMapKey
}
import ast.shapes.ShapeBody.MapStatement.MapValueType.{
  ElidedMapValue,
  ExplicitMapValue
}
import ast.shapes.ShapeBody.StructureMembers.{
  StructureMember,
  StructureMemberType
}
import util.string_ops.{indent, suffix, isTooWide}
import NodeWriter.{nodeObjectWriter, nodeValueWriter}
import ShapeIdWriter.{
  absoluteRootShapeIdWriter,
  identifierWriter,
  namespaceWriter,
  shapeIdMemberWriter,
  shapeIdWriter
}
import SmithyTraitWriter.{applyStatementWriter, traitStatementsWriter}
import WhiteSpaceWriter.{breakWriter, wsWriter}
import Writer.{WriterOps, WriterOpsIterable}

object ShapeWriter {

  implicit val namespaceStatementWriter: Writer[NamespaceStatement] =
    Writer.write { case NamespaceStatement(ns, br) =>
      s"namespace ${ns.write}\n${br.write}"
    }
  implicit val useStatementWriter: Writer[UseStatement] = Writer.write {
    case UseStatement(arsi, br) =>
      s"use ${arsi.write}${br.write}"
  }
  implicit val useSectionWriter: Writer[UseSection] = Writer.write {
    case UseSection(use_statements) =>
      use_statements.writeN("", "", "\n")
  }

  implicit val shapeSectionWriter: Writer[ShapeSection] = Writer.write {
    shapeSection =>
      shapeSection.all.write
  }
  implicit val shapeStatementWriter: Writer[ShapeStatement] = Writer.write {
    case ShapeStatement(traits, body, br) =>
      s"${traits.write}${body.write}\n${br.write}"
  }
  implicit val shapeStatementsCaseWriter: Writer[ShapeStatementsCase] =
    Writer.write {
      case ShapeStatementsCase.ShapeStatementCase(statement) =>
        statement.write
      case ShapeStatementsCase.ApplyStatementCase(apply) =>
        apply.write
    }
  implicit val shapeStatementsWriter: Writer[ShapeStatements] = Writer.write {
    case ShapeStatements(shape_statements) =>
      shape_statements.writeN
  }
  implicit val simpleTypeNameWriter: Writer[SimpleTypeName] = Writer.write {
    case SimpleTypeName(name) => name.write
  }
  // *SP %s"with" *WS "[" 1*(*WS ShapeId) *WS "]"
  implicit val mixinWriter: Writer[Mixin] = Writer.write {
    case Mixin(whitespace, shapeIds, whitespace1) =>
      val list = shapeIds.toList
      val useNewLine =
        Comment.whitespacesHaveComments(list.map(_._1)) || isTooWide(list)
      val values =
        if (useNewLine)
          list
            .map(_.write)
            .map(indent(_, "\n", 4))
            .mkString_("[\n", ",\n", s"\n${whitespace1.write}]")
        else list.map(_.write).mkString_("[", ", ", s"${whitespace1.write}]")
      s" with${whitespace.write} $values"
  }
  implicit val mapMemberTypeWriter: Writer[MapKeyType] = Writer.write {
    case ElidedMapKey(member)    => s"$$${member.write}"
    case ExplicitMapKey(shapeId) => s"key: ${shapeId.write}"
  }
  implicit val mapValueTypeWriter: Writer[MapValueType] = Writer.write {
    case ElidedMapValue(member)    => s"$$${member.write}"
    case ExplicitMapValue(shapeId) => s"value: ${shapeId.write}"
  }
  implicit val mapKeyWriter: Writer[MapKey] = Writer.write {
    case MapKey(traitStatements, keyType) =>
      s"${traitStatements.write}${keyType.write}"
  }
  implicit val mapValueWriter: Writer[MapValue] = Writer.write {
    case MapValue(traitStatements, valueType) =>
      s"${traitStatements.write}${valueType.write}"
  }

  implicit val mapMembersWriter: Writer[MapMembers] = Writer.write {
    case MapMembers(ws0, mapKey, ws1, mapValue, ws2) =>
      s"${ws0.write}${mapKey.write}\n${ws1.write}${mapValue.write}${ws2.write}"
  }

  implicit val valueAssignmentWriter: Writer[ValueAssignment] = Writer.write {
    case ValueAssignment(value, whitespace) =>
      s" = ${value.write}${whitespace.write}"
  }

  implicit val enumShapeMembersWriter: Writer[EnumShapeMembers] = Writer.write {
    case EnumShapeMembers(whitespace, members) =>
      val memberLines = members
        .map { case (ts, identifiers, maybeValue, ws) =>
          s"${ts.write}${identifiers.write}${maybeValue.write}${ws.write}"
        }
        .toList
        .mkString_("", "\n", "")
      s"${whitespace.write}${memberLines}"
  }
  implicit val structureMemberTypeWriter: Writer[StructureMemberType] =
    Writer.write {
      case StructureMemberType.ElidedStructureMember(identifier) =>
        s"$$${identifier.write}"
      case StructureMemberType.ExplicitStructureMember(identifier, shapeId) =>
        s"${identifier.write}: ${shapeId.write}"
    }
  implicit val unionMemberWriter: Writer[UnionMember] = Writer.write {
    case UnionMember(structureMemberType) => structureMemberType.write
  }

  implicit val unionMembersWriter: Writer[UnionMembers] = Writer.write {
    case UnionMembers(whitespace, members) =>
      val memberLines = members
        .map { case (traitStatements, structureMember, ws) =>
          s"${traitStatements.write}${structureMember.write}${ws.write}"
        }
        .mkString_("", ",\n", ",")
      s"${whitespace.write}${memberLines}"
  }
  implicit val structureMemberWriter: Writer[StructureMember] = Writer.write {
    case StructureMember(whitespace, maybeAssignment) =>
      s"${whitespace.write}${maybeAssignment.map(_.write).getOrElse("")}"
  }
  implicit val structureMembersWriter: Writer[StructureMembers] = Writer.write {
    case StructureMembers(whitespace, members) =>
      val memberLines = members
        .map { case (traitStatements, structureMember, ws) =>
          s"${traitStatements.write}${structureMember.write}${ws.write}"
        }
        .mkString_("", ",\n", "")
      s"${whitespace.write}${memberLines}"
  }
  implicit val inlineStructureWriter: Writer[InlineStructure] = Writer.write {
    case InlineStructure(whitespace, traitStatements, mixin, ws1, members) =>
      s"${whitespace.write}${traitStatements.write}${mixin.write}${ws1.write}${members.write}"
  }
  implicit val operationInputWriter: Writer[OperationInput] = Writer.write {
    case OperationInput(ws0, members, ws1) =>
      s"input: ${ws0.write}${members.write}${ws1.write}"
  }

  implicit val operationOutputWriter: Writer[OperationOutput] = Writer.write {
    case OperationOutput(ws0, members, ws1) =>
      s"output: ${ws0.write}${members.write}${ws1.write}"
  }
  implicit val operationErrorsWriter: Writer[OperationErrors] = Writer.write {
    case OperationErrors(ws0, ws1, list, _, ws3) =>
      val listLine = list
        .map { case (ws, shapeId) =>
          s"${ws.write}${shapeId.write}"
        }
        .mkString_("[", ", ", ",]")
      s"errors: ${ws0.write}${ws1.write}${listLine}${ws3.write}"
  }
  implicit val operationBodyPart: Writer[OperationBodyPart] = Writer.write {
    case i: OperationInput  => i.write
    case o: OperationOutput => o.write
    case e: OperationErrors => e.write
  }
  implicit val operationBodyWriter: Writer[OperationBody] = Writer.write {
    case OperationBody(whitespace, bodyParts, ws1) =>
      val lines = bodyParts
        .map(_.write)
        .filter(_.nonEmpty)
        .mkString_("", "\n", "")
      s"${whitespace.write}${lines}${ws1.write}"
  }
  implicit val listMemberTypeWriter: Writer[ListMemberType] = Writer.write {
    case ElidedListMember(shapeIdMember) => s"$$${shapeIdMember.write}"
    case ExplicitListMember(shapeId)     => s"    member: ${shapeId.write}"
  }
  implicit val listMemberWriter: Writer[ListMember] = Writer.write {
    case ListMember(traitStatements, listMemberType) =>
      s"${traitStatements.write}${listMemberType.write}"
  }
  implicit val listMembersWriter: Writer[ListMembers] = Writer.write {
    case ListMembers(ws0, members, ws1) =>
      s"${ws0.write}${members.write}${ws1.write}"
  }

  implicit val structureResource: Writer[StructureResource] = Writer.write {
    case StructureResource(shapeId) => s"${shapeId.write}"
  }

  implicit val shapeBodyWriter: Writer[ShapeBody] = Writer.write {
    case SimpleShapeStatement(simpleTypeName, ws0, mixin) =>
      s"${simpleTypeName.write} ${ws0.write}${mixin.write}"
    case EnumShapeStatement(
          typeName,
          id,
          mixin,
          whitespace,
          enumShapeMembers
        ) =>
      s"$typeName ${id.write}${mixin.write}${whitespace.write} {\n${indent(enumShapeMembers.write, "\n", 4)}\n}"

    case MapStatement(identifier, mixin, whitespace, members) =>
      s"map ${identifier.write}${mixin.write}${whitespace.write} {\n${indent(members.write, "\n", 4)}\n}"
    case UnionStatement(identifier, mixin, whitespace, members) =>
      s"union ${identifier.write}${mixin.write}${whitespace.write} {\n${indent(members.write, "\n", 4)}\n}"
    case ServiceStatement(identifier, mixin, whitespace1, nodeObject) =>
      s"service ${identifier.write} ${mixin.write}${whitespace1.write} {\n${nodeObject.write}\n}"
    case ResourceStatement(identifier, mixin, whitespace, nodeObject) =>
      s"resource ${identifier.write} ${mixin.write}${whitespace.write} {\n${nodeObject.write}\n}"
    case OperationStatement(identifier, mixin, whitespace, operationBody) =>
      s"operation ${identifier.write} ${mixin.write}${whitespace.write} {\n${indent(operationBody.write, "\n", 4)}\n}"
    case ListStatement(identifier, mixin, whitespace, members) =>
      s"list ${identifier.write}${mixin.write}${whitespace.write} {\n${members.write}\n}"
    case StructureStatement(
          identifier,
          resource,
          mixins,
          whitespace,
          members
        ) =>
      s"structure ${identifier.write}${resource.write}${mixins.write}${whitespace.write} {\n${indent(suffix(members.write, "\n"), "\n", 4)}\n}"

  }

}
/*
ShapeSection =
    [NamespaceStatement UseSection ShapeStatements]

NamespaceStatement =
    %s"namespace" SP Namespace BR

UseSection =
 *(UseStatement)

UseStatement =
    %s"use" SP AbsoluteRootShapeId BR

ShapeStatements =
 *(ShapeStatement / ApplyStatement)

ShapeStatement =
    TraitStatements ShapeBody BR

ShapeBody =
    SimpleShapeStatement
  / EnumShapeStatement
  / ListStatement
  / MapStatement
  / StructureStatement
  / UnionStatement
  / ServiceStatement
  / OperationStatement
  / ResourceStatement

SimpleShapeStatement =
    SimpleTypeName SP Identifier [Mixins]

SimpleTypeName =
    %s"blob" / %s"boolean" / %s"document" / %s"string"
  / %s"byte" / %s"short" / %s"integer" / %s"long"
  / %s"float" / %s"double" / %s"bigInteger"
  / %s"bigDecimal" / %s"timestamp"

Mixins =
 *SP %s"with" *WS "[" 1*(*WS ShapeId) *WS "]"

EnumShapeStatement =
    EnumTypeName SP Identifier [Mixins] *WS EnumShapeMembers

EnumTypeName =
    %s"enum" / %s"intEnum"

EnumShapeMembers =
    "{" *WS 1*(TraitStatements Identifier [ValueAssignment] `*WS`) "}"

ValueAssignment =
 *SP "=" *SP NodeValue BR

ListStatement =
    %s"list" SP Identifier [Mixins] *WS ListMembers

ListMembers =
    "{" *WS ListMember *WS "}"

ListMember =
    [TraitStatements] (ElidedListMember / ExplicitListMember)

ElidedListMember =
    %s"$member"

ExplicitListMember =
    %s"member" *SP ":" *SP ShapeId

MapStatement =
    %s"map" SP Identifier [Mixins] *WS MapMembers

MapMembers =
    "{" *WS MapKey BR MapValue *WS "}"

MapKey =
    [TraitStatements] (ElidedMapKey / ExplicitMapKey)

MapValue =
    [TraitStatements] (ElidedMapValue / ExplicitMapValue)

ElidedMapKey =
    %s"$key"

ExplicitMapKey =
    %s"key" *SP ":" *SP ShapeId

ElidedMapValue =
    %s"$value"

ExplicitMapValue =
    %s"value" *SP ":" *SP ShapeId

StructureStatement =
    %s"structure" SP Identifier [StructureResource]
          [Mixins] *WS StructureMembers

StructureResource =
    SP %s"for" SP ShapeId

StructureMembers =
    "{" *WS *(TraitStatements StructureMember *WS) "}"

StructureMember =
    (ExplicitStructureMember / ElidedStructureMember) [ValueAssignment]

ExplicitStructureMember =
    Identifier *SP ":" *SP ShapeId

ElidedStructureMember =
    "$" Identifier

UnionStatement =
    %s"union" SP Identifier [Mixins] *WS UnionMembers

UnionMembers =
    "{" *WS *(TraitStatements UnionMember *WS) "}"

UnionMember =
    (ExplicitStructureMember / ElidedStructureMember)

ServiceStatement =
    %s"service" SP Identifier [Mixins] *WS NodeObject

ResourceStatement =
    %s"resource" SP Identifier [Mixins] *WS NodeObject

OperationStatement =
    %s"operation" SP Identifier [Mixins] *WS OperationBody

OperationBody =
    "{" *WS
 *([OperationInput] / [OperationOutput] / [OperationErrors])
 *WS "}"
      ; only one of each property can be specified.

OperationInput =
    %s"input" *WS (InlineStructure / (":" *WS ShapeId)) WS

OperationOutput =
    %s"output" *WS (InlineStructure / (":" *WS ShapeId)) WS

OperationErrors =
    %s"errors" *WS ":" *WS "[" *(*WS Identifier) *WS "]" WS

InlineStructure =
    ":=" *WS TraitStatements [Mixins] *WS StructureMembers
 */
