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

package smithytranslate.proto3.internals

import software.amazon.smithy.model.shapes.ToShapeId
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.ShapeId

private[internals] object ProtoIR {

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
      reserved: List[Reserved],
      doc: Option[String] = None
  )

  sealed trait MessageElement
  object MessageElement {
    final case class FieldElement(field: Field) extends MessageElement
    final case class EnumDefElement(enumValue: Enum) extends MessageElement
    final case class OneofElement(oneof: Oneof) extends MessageElement
  }

  final case class Oneof(name: String, fields: List[Field], doc: Option[String] = None)

  final case class Field(
      deprecated: Boolean,
      ty: Type,
      name: String,
      number: Int,
      doc: Option[String] = None
  )

  sealed trait Reserved
  object Reserved {
    final case class Number(number: Int) extends Reserved
    final case class Name(name: String) extends Reserved
    final case class Range(start: Int, end: Int) extends Reserved
  }

  case class EnumValue(name: String, intValue: Int, doc: Option[String] = None)
  case class Enum(
      name: String,
      values: List[EnumValue],
      reserved: List[Reserved],
      doc: Option[String] = None
  )

  final case class Service(name: String, rpcs: List[Rpc], doc: Option[String] = None)

  final case class RpcMessage(fqn: Fqn, importFqn: Fqn)

  final case class Rpc(
      name: String,
      streamingRequest: Boolean,
      request: RpcMessage,
      streamingResponse: Boolean,
      response: RpcMessage,
      doc: Option[String] = None
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
        keyType: Type,
        valueType: Type
    ) extends Type {
      def importFqn: Set[Fqn] =
        keyType.importFqn ++ valueType.importFqn
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

    object Alloy {
      // Helper trait to be able select between different protobuf types for a given smithy type, the different types would be
      // a wrapped value type
      // a compacted/efficient protobuf representation of a type
      // a wrapped value of the compacted protobuf type
      trait TypeMatcher {
        def matchingTrait: ShapeId
        type IsCompact = Boolean
        type IsWrapped = Boolean
        val mapShapeToType: PartialFunction[(IsCompact, IsWrapped), Type]
      }

      case class WrappedTypeMatcher(
        val matchingTrait: ShapeId,
        wrappedType: Type
      ) extends TypeMatcher {
        val mapShapeToType = {
          case (false, true) => wrappedType
        }
      }

      object TypeMatcher {
        val uuidMatcher = new TypeMatcher {
          val matchingTrait = alloy.UuidFormatTrait.ID
          val mapShapeToType = {
            case (true, true) => AlloyWrappers.CompactUUID
            case (true, false) => AlloyTypes.CompactUUID
          }
        }

        val localDateMatcher = new TypeMatcher {
          val matchingTrait = alloy.DateFormatTrait.ID
          val mapShapeToType = {
            case (true, true) => AlloyWrappers.CompactLocalDate
            case (true, false) => AlloyTypes.CompactLocalDate
            case (false, true) => AlloyWrappers.LocalDate
          }
        }

        val yearMonthMatcher = new TypeMatcher {
          val matchingTrait = alloy.YearMonthFormatTrait.ID
          val mapShapeToType = {
            case (true, true) => AlloyWrappers.CompactYearMonth
            case (true, false) => AlloyTypes.CompactYearMonth
            case (false, true) => AlloyWrappers.YearMonth
          }
        }

        val monthDayMatcher = new TypeMatcher {
          val matchingTrait = alloy.MonthDayFormatTrait.ID
          val mapShapeToType = {
            case (true, true) => AlloyWrappers.CompactMonthDay
            case (true, false) => AlloyTypes.CompactMonthDay
            case (false, true) => AlloyWrappers.MonthDay
          }
        }

        val offsetDateTimeMatcher = new TypeMatcher {
          val matchingTrait = alloy.OffsetDateTimeFormatTrait.ID
          val mapShapeToType = {
            case (true, true) => AlloyWrappers.CompactOffsetDateTime
            case (true, false) => AlloyTypes.CompactOffsetDateTime
            case (false, true) => AlloyWrappers.OffsetDateTime
            case (false, false) => Type.String
          }
        }

        val durationMatcher = new TypeMatcher {
          val matchingTrait = alloy.DurationSecondsFormatTrait.ID
          val mapShapeToType = { 
            case (_, true) => AlloyWrappers.Duration
            case (_, false) => AlloyTypes.Duration
          }
        }

        val localTimeMatcher = new TypeMatcher {
          val matchingTrait = alloy.LocalTimeFormatTrait.ID
          val mapShapeToType = { 
            case (true, true) => AlloyWrappers.CompactLocalTime
            case (true, false) => AlloyTypes.CompactLocalTime
            case (false, true) => AlloyWrappers.LocalTime
          }
        }

        val localDateTimeMatcher = WrappedTypeMatcher(
          alloy.LocalDateTimeFormatTrait.ID,
          AlloyWrappers.LocalDateTime
        )

        val offsetTimeMatcher = WrappedTypeMatcher(
          alloy.OffsetTimeFormatTrait.ID,
          AlloyWrappers.OffsetTime
        )

        val zoneIdMatcher = WrappedTypeMatcher(
          alloy.ZoneIdFormatTrait.ID,
          AlloyWrappers.ZoneId
        )

        val zoneOffsetMatcher = WrappedTypeMatcher(
          alloy.ZoneOffsetFormatTrait.ID,
          AlloyWrappers.ZoneOffset
        )

        val zonedDateTimeMatcher = WrappedTypeMatcher(
          alloy.ZonedDateTimeFormatTrait.ID,
          AlloyWrappers.ZonedDateTime
        )

        val yearMatcher = WrappedTypeMatcher(
          alloy.YearFormatTrait.ID,
          AlloyWrappers.Year
        )

        val all = List(
          uuidMatcher,
          localDateMatcher,
          yearMonthMatcher,
          monthDayMatcher,
          offsetDateTimeMatcher,
          localTimeMatcher,
          localDateTimeMatcher,
          offsetTimeMatcher,
          zoneIdMatcher,
          zoneOffsetMatcher,
          zonedDateTimeMatcher,
          yearMatcher,
          durationMatcher
        )
      }

      def fromShape(shape: Shape, isWrapped: Boolean, isCompact: Boolean): Option[Type] = {
        TypeMatcher.all.collectFirst { case matcher if shape.hasTrait(matcher.matchingTrait) => 
          matcher.mapShapeToType.lift((isCompact, isWrapped)) 
        }.flatten
      }
    }

    object AlloyTypes {
      val CompactUUID = RefType(
        alloyFqn("CompactUUID"),
        alloyTypesImport
      )
      val EpochMillisTimestamp = RefType(
        alloyFqn("EpochMillisTimestamp"),
        alloyTypesImport
      )
      val DayOfWeek = RefType(
        alloyFqn("DayOfWeek"),
        alloyTypesImport
      )
      val Month = RefType(
        alloyFqn("Month"),
        alloyTypesImport
      )
      val CompactLocalDate = RefType(
        alloyFqn("CompactLocalDate"),
        alloyTypesImport
      )
      val CompactLocalTime = RefType(
        alloyFqn("CompactLocalTime"),
        alloyTypesImport
      )
      val CompactYearMonth = RefType(
        alloyFqn("CompactYearMonth"),
        alloyTypesImport
      )
      val CompactMonthDay = RefType(
        alloyFqn("CompactMonthDay"),
        alloyTypesImport
      )
      val CompactOffsetDateTime = RefType(
        alloyFqn("CompactOffsetDateTime"),
        alloyTypesImport
      )
      val Duration = RefType(
        alloyFqn("Duration"),
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
      val EpochMillisTimestamp = RefType(
        alloyFqn("EpochMillisTimestampValue"),
        alloyWrappersImport
      )
      val CompactUUID = RefType(
        alloyFqn("CompactUUIDValue"),
        alloyWrappersImport
      )
      val CompactLocalDate = RefType(
        alloyFqn("CompactLocalDateValue"),
        alloyWrappersImport
      )
      val CompactLocalTime = RefType(
        alloyFqn("CompactLocalTimeValue"),
        alloyWrappersImport
      )
      val CompactYearMonth = RefType(
        alloyFqn("CompactYearMonthValue"),
        alloyWrappersImport
      )
      val CompactMonthDay = RefType(
        alloyFqn("CompactMonthDayValue"),
        alloyWrappersImport
      )
      val CompactOffsetDateTime = RefType(
        alloyFqn("CompactOffsetDateTimeValue"),
        alloyWrappersImport
      )
      val Document = RefType(
        alloyFqn("DocumentValue"),
        alloyWrappersImport
      )
      val LocalDate = RefType(
        alloyFqn("LocalDateValue"),
        alloyWrappersImport
      )
      val LocalTime = RefType(
        alloyFqn("LocalTimeValue"),
        alloyWrappersImport
      )
      val LocalDateTime = RefType(
        alloyFqn("LocalDateTimeValue"),
        alloyWrappersImport
      )
      val OffsetDateTime = RefType(
        alloyFqn("OffsetDateTimeValue"),
        alloyWrappersImport
      )
      val OffsetTime = RefType(
        alloyFqn("OffsetTimeValue"),
        alloyWrappersImport
      )
      val ZoneId = RefType(
        alloyFqn("ZoneIdValue"),
        alloyWrappersImport
      )
      val ZoneOffset = RefType(
        alloyFqn("ZoneOffsetValue"),
        alloyWrappersImport
      )
      val ZonedDateTime = RefType(
        alloyFqn("ZonedDateTimeValue"),
        alloyWrappersImport
      )
      val Year = RefType(
        alloyFqn("YearValue"),
        alloyWrappersImport
      )
      val YearMonth = RefType(
        alloyFqn("YearMonthValue"),
        alloyWrappersImport
      )
      val MonthDay = RefType(
        alloyFqn("MonthDayValue"),
        alloyWrappersImport
      )
      val Duration = RefType(
        alloyFqn("DurationValue"),
        alloyWrappersImport
      )
    }

    // https://github.com/protocolbuffers/protobuf/blob/178ebc179ede26bcaa85b39db127ebf099be3ef8/src/google/protobuf/wrappers.proto

    sealed trait PredefinedType extends Type {
      def fqn: Fqn
    }
    sealed trait GoogleWrappers extends PredefinedType {
      def importFqn = Set(protobufFqn("wrappers"))
    }

    case object GoogleValue extends PredefinedType {
      def importFqn: Set[Fqn] = Set(protobufFqn("struct"))
      def fqn = protobufFqn("Value")
    }

    case object GoogleTimestamp extends PredefinedType {
      def importFqn: Set[Fqn] = Set(protobufFqn("timestamp"))
      def fqn = protobufFqn("Timestamp")
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
