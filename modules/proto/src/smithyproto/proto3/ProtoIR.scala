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

package smithyproto.proto3

import software.amazon.smithy.model.shapes.ToShapeId

object ProtoIR {

  final case class CompilationUnit(
      packageName: Option[String],
      statements: List[Statement],
      options: List[TopLevelOption]
  )

  final case class TopLevelOption(key: String, value: String)

  sealed trait Statement
  object Statement {
    final case class ImportStatement(path: String) extends Statement
    final case class TopLevelStatement(s: TopLevelDef) extends Statement
  }

  sealed trait TopLevelDef
  object TopLevelDef {
    final case class MessageDef(message: Message) extends TopLevelDef
    final case class EnumDef(enumValue: Enum) extends TopLevelDef
    final case class ServiceDef(service: Service) extends TopLevelDef
  }

  final case class Message(
      name: String,
      elements: List[MessageElement],
      reserved: List[Reserved]
  )

  sealed trait MessageElement
  object MessageElement {
    final case class FieldElement(field: Field) extends MessageElement
    final case class EnumDefElement(enumValue: Enum) extends MessageElement
    final case class OneofElement(oneof: Oneof) extends MessageElement
  }

  final case class Oneof(name: String, fields: List[Field])

  final case class Field(
      deprecated: Boolean,
      ty: Type,
      name: String,
      number: Int
  )

  sealed trait Reserved
  object Reserved {
    final case class Number(number: Int) extends Reserved
    final case class Name(name: String) extends Reserved
    final case class Range(start: Int, end: Int) extends Reserved
  }

  case class EnumValue(name: String, intValue: Int)
  case class Enum(
      name: String,
      values: List[EnumValue],
      reserved: List[Reserved]
  )

  final case class Service(name: String, rpcs: List[Rpc])

  final case class RpcMessage(fqn: Fqn, importFqn: Fqn)

  final case class Rpc(
      name: String,
      streamingRequest: Boolean,
      request: RpcMessage,
      streamingResponse: Boolean,
      response: RpcMessage
  )

  sealed trait Type {
    def importFqn: Set[Fqn]
  }
  object Type {

    private def protobufFqn(last: String) =
      Fqn(Some(List("google", "protobuf")), last)

    private def alloyFqn(last: String) =
      Fqn(Some(List("alloy", "protobuf")), last)

    sealed trait PrimitiveType extends Type {
      def importFqn: Set[Fqn] = Set.empty
    }

    case object Double extends PrimitiveType
    case object Float extends PrimitiveType
    case object Int32 extends PrimitiveType
    case object Int64 extends PrimitiveType
    case object Uint32 extends PrimitiveType
    case object Uint64 extends PrimitiveType
    case object Sint32 extends PrimitiveType
    case object Sint64 extends PrimitiveType
    case object Fixed32 extends PrimitiveType
    case object Fixed64 extends PrimitiveType
    case object Sfixed32 extends PrimitiveType
    case object Sfixed64 extends PrimitiveType
    case object Bool extends PrimitiveType
    case object String extends PrimitiveType
    case object Bytes extends PrimitiveType
    final case class MapType(
        keyType: Either[Type.Int32.type, Type.String.type],
        valueType: Type
    ) extends Type {
      val foldedKeyType: Type = keyType.fold(identity, identity)
      def importFqn: Set[Fqn] =
        keyType.fold(_.importFqn, _.importFqn) ++ valueType.importFqn
    }
    final case class ListType(valueType: Type) extends Type {
      def importFqn: Set[Fqn] = valueType.importFqn
    }
    final case class RefType(fqn: Fqn, _importFqn: Fqn) extends Type {
      def importFqn: Set[Fqn] = Set(_importFqn)
    }
    object RefType {
      def apply(toShapeId: ToShapeId): RefType = RefType(
        Namespacing.shapeIdToFqn(toShapeId.toShapeId()),
        Namespacing.shapeIdToImportFqn(toShapeId.toShapeId())
      )
    }
    case object Any extends Type {
      def importFqn = Set(protobufFqn("any"))
      val fqn: Fqn = protobufFqn("Any")
    }
    case object Empty extends Type {
      def importFqn = Set(protobufFqn("empty"))
      val fqn: Fqn = protobufFqn("Empty")
    }

    private val alloyTypesImport =
      Fqn(Some(List("alloy", "protobuf")), "types")

    private val alloyWrappersImport =
      Fqn(Some(List("alloy", "protobuf")), "wrappers")

    object AlloyTypes {
      val Timestamp = RefType(
        alloyFqn("Timestamp"),
        alloyTypesImport
      )
      val CompactUUID = RefType(
        alloyFqn("CompactUUID"),
        alloyTypesImport
      )
      val Document = RefType(
        alloyFqn("Document"),
        alloyTypesImport
      )
    }

    object AlloyWrappers {
      val BigInteger = RefType(
        alloyFqn("BigIntegerValue"),
        alloyWrappersImport
      )
      val BigDecimal = RefType(
        alloyFqn("BigDecimalValue"),
        alloyWrappersImport
      )
      val ShortValue = RefType(
        alloyFqn("ShortValue"),
        alloyWrappersImport
      )
      val Fixed32 = RefType(
        alloyFqn("Fixed32Value"),
        alloyWrappersImport
      )
      val SFixed32 = RefType(
        alloyFqn("SFixed32Value"),
        alloyWrappersImport
      )
      val Fixed64 = RefType(
        alloyFqn("Fixed64Value"),
        alloyWrappersImport
      )
      val SFixed64 = RefType(
        alloyFqn("SFixed64Value"),
        alloyWrappersImport
      )
      val SInt32 = RefType(
        alloyFqn("SInt32Value"),
        alloyWrappersImport
      )
      val SInt64 = RefType(
        alloyFqn("SInt64Value"),
        alloyWrappersImport
      )
      val ByteValue = RefType(
        alloyFqn("ByteValue"),
        alloyWrappersImport
      )
      val Timestamp = RefType(
        alloyFqn("TimestampValue"),
        alloyWrappersImport
      )
      val CompactUUID = RefType(
        alloyFqn("CompactUUIDValue"),
        alloyWrappersImport
      )
      val Document = RefType(
        alloyFqn("DocumentValue"),
        alloyWrappersImport
      )
    }

    // https://github.com/protocolbuffers/protobuf/blob/178ebc179ede26bcaa85b39db127ebf099be3ef8/src/google/protobuf/wrappers.proto

    trait GoogleWrappers extends Type {
      def importFqn = Set(protobufFqn("wrappers"))
      def fqn: Fqn
    }
    object GoogleWrappers {
      case object Double extends GoogleWrappers {
        def fqn: Fqn = protobufFqn("DoubleValue")
      }
      case object Float extends GoogleWrappers {
        def fqn: Fqn = protobufFqn("FloatValue")
      }
      case object Int64 extends GoogleWrappers {
        def fqn: Fqn = protobufFqn("Int64Value")
      }
      case object Uint64 extends GoogleWrappers {
        def fqn: Fqn = protobufFqn("UInt64Value")
      }
      case object Int32 extends GoogleWrappers {
        def fqn: Fqn = protobufFqn("Int32Value")
      }
      case object Uint32 extends GoogleWrappers {
        def fqn: Fqn = protobufFqn("UInt32Value")
      }
      case object Bool extends GoogleWrappers {
        def fqn: Fqn = protobufFqn("BoolValue")
      }
      case object String extends GoogleWrappers {
        def fqn: Fqn = protobufFqn("StringValue")
      }
      case object Bytes extends GoogleWrappers {
        def fqn: Fqn = protobufFqn("BytesValue")
      }
    }
  }

  final case class Fqn(packageName: Option[List[String]], name: String) {
    def render: String =
      packageName.map(_.mkString(".")).map(_ + ".").getOrElse("") + name
  }
}
