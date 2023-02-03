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
    final case class MessageType(fqn: Fqn, _importFqn: Fqn) extends Type {
      def importFqn: Set[Fqn] = Set(_importFqn)
    }
    final case class EnumType(fqn: Fqn, _importFqn: Fqn) extends Type {
      def importFqn: Set[Fqn] = Set(_importFqn)
    }
    case object Any extends Type {
      def importFqn = Set(protobufFqn("any"))
      val fqn: Fqn = protobufFqn("Any")
    }
    case object Empty extends Type {
      def importFqn = Set(protobufFqn("empty"))
      val fqn: Fqn = protobufFqn("Empty")
    }

    private val smithyTranslateImportFqn =
      Namespacing.namespaceToFqn("smithytranslate")

    val BigInteger = MessageType(
      Fqn(Some(List("smithytranslate")), "BigInteger"),
      smithyTranslateImportFqn
    )
    val BigDecimal = MessageType(
      Fqn(Some(List("smithytranslate")), "BigDecimal"),
      smithyTranslateImportFqn
    )
    val Timestamp = MessageType(
      Fqn(Some(List("smithytranslate")), "Timestamp"),
      smithyTranslateImportFqn
    )

    // https://github.com/protocolbuffers/protobuf/blob/178ebc179ede26bcaa85b39db127ebf099be3ef8/src/google/protobuf/wrappers.proto

    trait Wrappers extends Type {
      def importFqn = Set(protobufFqn("wrappers"))
      def fqn: Fqn
    }
    object Wrappers {
      case object Double extends Wrappers {
        def fqn: Fqn = protobufFqn("DoubleValue")
      }
      case object Float extends Wrappers {
        def fqn: Fqn = protobufFqn("FloatValue")
      }
      case object Int64 extends Wrappers {
        def fqn: Fqn = protobufFqn("Int64Value")
      }
      case object Uint64 extends Wrappers {
        def fqn: Fqn = protobufFqn("UInt64Value")
      }
      case object Int32 extends Wrappers {
        def fqn: Fqn = protobufFqn("Int32Value")
      }
      case object Uint32 extends Wrappers {
        def fqn: Fqn = protobufFqn("UInt32Value")
      }
      case object Bool extends Wrappers {
        def fqn: Fqn = protobufFqn("BoolValue")
      }
      case object String extends Wrappers {
        def fqn: Fqn = protobufFqn("StringValue")
      }
      case object Bytes extends Wrappers {
        def fqn: Fqn = protobufFqn("BytesValue")
      }
    }
  }

  final case class Fqn(packageName: Option[List[String]], name: String) {
    def render: String =
      packageName.map(_.mkString(".")).map(_ + ".").getOrElse("") + name
  }
}
