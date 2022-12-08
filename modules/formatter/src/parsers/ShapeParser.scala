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
import smithytranslate.formatter.ast.{ShapeId, Break, Whitespace, shapes}
import smithytranslate.formatter.ast.shapes._
import smithytranslate.formatter.ast.shapes.ShapeBody._
import smithytranslate.formatter.ast.shapes.ShapeBody.ListStatement.{
  ElidedListMember,
  ExplicitListMember,
  ListMember,
  ListMembers
}
import smithytranslate.formatter.ast.shapes.ShapeBody.MapStatement.MapKeyType.{
  ElidedMapKey,
  ExplicitMapKey
}
import smithytranslate.formatter.ast.shapes.ShapeBody.MapStatement.MapValueType.{
  ElidedMapValue,
  ExplicitMapValue
}
import smithytranslate.formatter.ast.shapes.ShapeBody.MapStatement.{
  MapKey,
  MapMembers,
  MapValue
}
import smithytranslate.formatter.ast.shapes.ShapeBody.StructureMembers.StructureMember
import smithytranslate.formatter.ast.shapes.ShapeBody.StructureMembers.StructureMemberType.{
  ElidedStructureMember,
  ExplicitStructureMember
}
import smithytranslate.formatter.ast.shapes.ShapeStatementsCase.{
  ApplyStatementCase,
  ShapeStatementCase
}
import smithytranslate.formatter.parsers.WhitespaceParser.{br, sp, sp0, ws}
import smithytranslate.formatter.parsers.NodeParser._
import smithytranslate.formatter.parsers.ShapeIdParser._
import smithytranslate.formatter.parsers.ShapeParser.list_parsers.list_statement
import smithytranslate.formatter.parsers.ShapeParser.map_parsers.map_statement
import smithytranslate.formatter.parsers.ShapeParser.operation_parsers.operation_statement
import smithytranslate.formatter.parsers.ShapeParser.structure_parsers.structure_statement
import smithytranslate.formatter.parsers.ShapeParser.union_parsers.union_statement
import smithytranslate.formatter.parsers.SmithyTraitParser.{
  apply_statement,
  trait_statements
}

object ShapeParser {

  val simpleNames: Set[String] = Set(
    "blob",
    "boolean",
    "document",
    "string",
    "byte",
    "short",
    "integer",
    "long",
    "float",
    "double",
    "bigInteger",
    "bigDecimal",
    "timestamp"
  )

  val enumTypeNames: Set[String] = Set("enum", "intEnum")

  val simple_type_name: Parser[SimpleTypeName] =
    Parser.stringIn(simpleNames).map(SimpleTypeName)
  val enum_type_name: Parser[String] = Parser.stringIn(enumTypeNames)

  // see comments on ValueAssignment
  val value_assigments: Parser[ValueAssignment] =
    (sp *> Parser.char('=') *> sp *> node_value ~ ws).map { case (l, r) =>
      ValueAssignment(l, r)
    }

  val mixins: Parser[Mixin] =
    ((sp.with1 *> Parser.string(
      "with"
    ) *> ws <* openSquare) ~ (ws.with1 ~ shape_id).backtrack.rep ~ ws <* closeSquare)
      .map { case ((ws0, values), ws1) =>
        Mixin(ws0, values, ws1)
      }

  val mixinBT = mixins.backtrack

  val simple_shape_statement: Parser[SimpleShapeStatement] =
    ((simple_type_name <* sp) ~ (identifier ~ mixinBT.?)).map {
      case (simpleTypeName, (b, mixin)) =>
        SimpleShapeStatement(simpleTypeName, b, mixin)
    }

  val enum_shape_members: Parser[EnumShapeMembers] =
    (openCurly *> ws.with1 ~ (trait_statements.with1 ~ identifier ~ value_assigments.? ~ ws).rep <* closeCurly)
      .map { case (ws, members) =>
        EnumShapeMembers(
          ws,
          members.map { case (((traits, id), va), ws) =>
            (traits, id, va, ws)
          }
        )
      }

  // EnumTypeName SP Identifier [Mixins] *WS EnumShapeMembers
  val enum_shape_statement: Parser[EnumShapeStatement] =
    ((enum_type_name <* sp) ~ identifier ~ mixinBT.? ~ ws ~ enum_shape_members)
      .map { case ((((enumTypeName, id), mixin), ws), members) =>
        EnumShapeStatement(enumTypeName, id, mixin, ws, members)
      }

  object list_parsers {

    val explicit_list_member: Parser[ExplicitListMember] =
      Parser.string("member") *> sp.rep0 *> Parser.string(":") *> sp *> shape_id
        .map(
          ExplicitListMember
        )

    val elided_list_member: Parser[ElidedListMember] =
      shape_id_member.map(ElidedListMember)

    val list_member: Parser[ListMember] =
      (trait_statements.with1.soft ~ (explicit_list_member.backtrack | elided_list_member))
        .map { case (traits, member) =>
          ListMember(traits, member)
        }

    val list_members: Parser[ListMembers] =
      (openCurly *> ws ~ list_member ~ ws <* closeCurly).map {
        case ((ws0, members), ws1) => ListMembers(ws0, members, ws1)
      }

    val list_statement: Parser[ListStatement] =
      ((Parser.string(
        "list"
      ) <* sp0) *> identifier ~ mixinBT.? ~ ws ~ list_members).map {
        case (((id, mixin), ws), members) =>
          ListStatement(id, mixin, ws, members)
      }
  }

  object map_parsers {
    val elided_map_key: Parser[ElidedMapKey] =
      shape_id_member.map(ElidedMapKey)
    val explicit_map_key: Parser[ExplicitMapKey] =
      Parser.string("key") *> sp0 *> colon *> sp0 *> shape_id.map(
        ExplicitMapKey
      )
    val elided_map_value: Parser[ElidedMapValue] =
      shape_id_member.map(ElidedMapValue)
    val explicit_map_value: Parser[ExplicitMapValue] =
      Parser.string("value") *> sp0 *> colon *> sp0 *> shape_id.map(
        ExplicitMapValue
      )
    val map_value: Parser[MapValue] =
      (trait_statements.?.with1.soft ~ (explicit_map_value.backtrack | elided_map_value))
        .map { case (traits, member) =>
          MapValue(traits, member)
        }
    val map_key: Parser[MapKey] =
      (trait_statements.?.with1.soft ~ (explicit_map_key.backtrack | elided_map_key))
        .map { case (traits, member) =>
          MapKey(traits, member)
        }

    val map_members =
      (openCurly *> (ws ~ map_key ~ ws ~ map_value ~ ws) <* closeCurly).map {
        case ((((ws0, key), br), value), ws2) =>
          MapMembers(ws0, key, br, value, ws2)
      }
    val map_statement: Parser[MapStatement] =
      (Parser.string("map") *> sp0 *> identifier ~ mixinBT.? ~ ws ~ map_members)
        .map { case (((id, mixins), ws), members) =>
          MapStatement(id, mixins, ws, members)
        }
  }

  object structure_parsers {
    val explicit_structure_member: Parser[ExplicitStructureMember] =
      ((identifier <* sp0 <* Parser.string(":") <* sp0) ~ shape_id)
        .map(ExplicitStructureMember.tupled)
    val elided_structure_member: Parser[ElidedStructureMember] =
      (Parser.string("$") *> identifier).map(ElidedStructureMember)
    val structure_member: Parser[StructureMember] = {
      ((explicit_structure_member.backtrack | elided_structure_member) ~ value_assigments.backtrack.?)
        .map { case (member, va) =>
          StructureMember(member, va)
        }
    }
    val structure_members: Parser[StructureMembers] =
      (openCurly *> ws ~ (trait_statements.with1 ~ structure_member ~ ws).rep0 <* closeCurly)
        .map { case (ws0, members) =>
          StructureMembers(
            ws0,
            members.map { case ((a, b), ws) =>
              (a, b, ws)
            }
          )
        }
    val structure_resource: Parser[StructureResource] =
      (sp0.with1 *> Parser.string("for") *> sp0 *> shape_id)
        .map(StructureResource)
    val structure_statement: Parser[StructureStatement] = (Parser.string(
      "structure"
    ) *> sp0 *> identifier ~ structure_resource.backtrack.? ~ mixinBT.? ~ ws ~ structure_members)
      .map { case ((((id, resource), mixins), ws), members) =>
        StructureStatement(id, resource, mixins, ws, members)
      }
  }

  object union_parsers {
    val union_member: Parser[UnionMember] =
      (structure_parsers.explicit_structure_member | structure_parsers.elided_structure_member)
        .map(UnionMember)
    val union_members: Parser[UnionMembers] =
      (openCurly *> ws ~ (trait_statements.with1 ~ union_member ~ ws).rep0 <* closeCurly)
        .map { case (ws0, members) =>
          UnionMembers(
            ws0,
            members.map { case ((a, b), ws) =>
              (a, b, ws)
            }
          )
        }
    val union_statement: Parser[UnionStatement] =
      (Parser.string(
        "union"
      ) *> sp *> identifier ~ mixinBT.? ~ ws ~ union_members).map {
        case (((id, mixins), ws), members) =>
          UnionStatement(id, mixins, ws, members)
      }
  }

  object operation_parsers {
    val ir: Parser[(Whitespace, ShapeId)] = colon *> ws ~ shape_id
    val operation_input: Parser[OperationInput] =
      (Parser.string("input") *> ws ~ ir.backtrack.eitherOr(
        inline_structure
      ) ~ ws).map { case ((ws0, either), ws1) =>
        OperationInput(ws0, either, ws1)
      }
    val operation_output: Parser[OperationOutput] =
      (Parser.string("output") *> ws ~ ir.backtrack.eitherOr(
        inline_structure
      ) ~ ws).map { case ((ws0, either), ws1) =>
        OperationOutput(ws0, either, ws1)
      }
    val operation_errors: Parser[OperationErrors] =
      ((((Parser.string("errors") *> ws <* Parser.char(
        ':'
      )) ~ ws <* openSquare) ~ (ws.with1 ~ identifier).backtrack.rep0 ~ ws <* closeSquare) ~ ws)
        .map { case ((((ws0, ws1), indentifiers), ws2), ws3) =>
          OperationErrors(ws0, ws1, indentifiers, ws2, ws3)
        }

    val opBodyParts: Parser0[List[OperationBodyPart]] =
      operation_input
        .eitherOr(operation_output)
        .eitherOr(operation_errors)
        .rep0(0, 3)
        .map {
          _.map {
            case Left(err)           => err
            case Right(Left(output)) => output
            case Right(Right(input)) => input
          }
        }

    val operation_body: Parser[OperationBody] =
      (openCurly *> ws ~ opBodyParts ~ ws <* closeCurly)
        .map { case ((ws0, bodyParts), ws1) =>
          OperationBody(ws0, bodyParts, ws1)
        }

    val operation_statement: Parser[OperationStatement] =
      (Parser.string(
        "operation"
      ) *> sp *> identifier ~ mixinBT.? ~ ws ~ operation_body).map {
        case (((id, mixins), ws), body) =>
          OperationStatement(id, mixins, ws, body)
      }
  }

  val resource_statement: Parser[ResourceStatement] =
    (Parser.string(
      "resource"
    ) *> sp *> identifier ~ mixinBT.? ~ ws ~ nodeObject)
      .map { case (((id, mixins), ws), nodeObject) =>
        ResourceStatement(id, mixins, ws, nodeObject)
      }

  val inline_structure: Parser[InlineStructure] = (Parser.string(
    ":="
  ) *> ws ~ trait_statements ~ mixinBT.? ~ ws ~ structure_parsers.structure_members)
    .map { case ((((ws0, traits), mixins), ws1), members) =>
      InlineStructure(ws0, traits, mixins, ws1, members)
    }

  // %s"service" SP Identifier [Mixins] *WS NodeObject
  val service_statement: Parser[ServiceStatement] =
    (Parser.string("service") *> sp *> identifier ~ mixinBT.? ~ ws ~ nodeObject)
      .map { case (((id, mixins), ws), node_object) =>
        ServiceStatement(id, mixins, ws, node_object)
      }

  val shape_body: Parser[shapes.ShapeBody] =
    simple_shape_statement | enum_shape_statement | list_statement | map_statement | structure_statement | union_statement | service_statement | operation_statement | resource_statement
  val shape_statement: Parser[ShapeStatement] = {
    val traitAndBody = trait_statements.with1 ~ shape_body
    val interspersedBr =
      (traitAndBody.soft ~ br) | traitAndBody.map(_ -> Break(Nil))
    interspersedBr.map { case ((a, b), c) =>
      ShapeStatement(a, b, c)
    }
  }
  val shape_statements: Parser0[ShapeStatements] = (shape_statement
    .map(
      ShapeStatementCase
    ) | apply_statement.map(
    ApplyStatementCase
  )).rep0.map {
    ShapeStatements
  }

  val namespace_statement: Parser[NamespaceStatement] =
    Parser.string("namespace") *> (sp *> namespace ~ br).map { case (ns, br) =>
      NamespaceStatement(ns, br)
    }
  val use_statement: Parser[UseStatement] =
    (Parser.string("use") *> sp *> absolute_root_shape_id ~ br).map {
      case (arsi, br) => UseStatement(arsi, br)
    }
  val use_section: Parser0[UseSection] = use_statement.rep0.map(UseSection)
  val shape_section: Parser0[ShapeSection] =
    (namespace_statement ~ use_section ~ shape_statements).?.map { op =>
      ShapeSection(op.map { case ((ns, use), ss) =>
        (ns, use, ss)
      })
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
    "{" *WS MapKey WS MapValue *WS "}"

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
