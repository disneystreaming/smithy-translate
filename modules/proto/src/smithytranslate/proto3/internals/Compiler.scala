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

import alloy.proto._
import smithytranslate.closure.ModelOps._
import software.amazon.smithy.model.loader.Prelude
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes._
import software.amazon.smithy.model.traits.DeprecatedTrait
import software.amazon.smithy.model.traits.EnumTrait
import software.amazon.smithy.model.traits.UnitTypeTrait
import software.amazon.smithy.model.traits.TraitDefinition

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._
import software.amazon.smithy.model.neighbor.NeighborProvider
import software.amazon.smithy.model.neighbor.Walker
import alloy.OpenEnumTrait
import software.amazon.smithy.model.traits.EnumValueTrait
import alloy.proto.ProtoTimestampFormatTrait
import alloy.proto.ProtoTimestampFormatTrait.TimestampFormat

private[proto3] class Compiler(model: Model, allShapes: Boolean) {

  // Reference:
  // 1. https://github.com/protocolbuffers/protobuf/blob/master/docs/field_presence.md

  import ProtoIR._

  private lazy val allRelevantShapes: Set[Shape] = {
    if (allShapes) {
      model
        .shapes()
        .iterator()
        .asScala
        .filterNot(ShapeFiltering.exclude)
        .toSet
    } else {
      val walker = new Walker(NeighborProvider.of(model))
      val protoEnabledShapes =
        model.getShapesWithTrait(classOf[ProtoEnabledTrait]).asScala
      val grpcShapes = model.getShapesWithTrait(classOf[GrpcTrait]).asScala
      val allRoots = protoEnabledShapes ++ grpcShapes
      val allTransitiveShapes: Set[Shape] = allRoots
        .flatMap((shape: Shape) => walker.walkShapes(shape).asScala)
        .toSet
      (allRoots ++ allTransitiveShapes).toSet
    }
  }

  private lazy val conflictingEnumValues: Set[MemberShape] = {
    val enumMembers =
      allRelevantShapes.collect { case m: MemberShape => m }.filter { m =>
        val container = model.expectShape(m.getContainer())
        container.isIntEnumShape() || container.isEnumShape()
      }
    def getKey(m: MemberShape) = m.getId().getNamespace() -> m.getMemberName()
    val conflicting = enumMembers.groupBy(getKey).filter(_._2.size > 1).keySet
    enumMembers.filter(m => conflicting(getKey(m)))
  }

  private def enumValueName(m: MemberShape): String = if (
    conflictingEnumValues(m)
  )
    m.getId().getName().toUpperCase + "_" + m.getMemberName()
  else m.getMemberName()

  /** these exclusions are performed as a last step to avoid shapes like
    * `structure protoEnabled {}` to be rendered as proto messages.
    *
    * this is done here, rather than in pre-processing, because removing the
    * shape entirely from the model would remove the trait it represents and
    * affect the compiler behaviour
    */
  object ShapeFiltering {

    private def excludeInternal(shape: Shape): Boolean = {
      val excludeNs = Set("alloy.proto", "alloy", "smithytranslate")
      excludeNs.contains(shape.getId().getNamespace())
    }

    def traitShapes(s: Shape): Boolean = {
      s.hasTrait(classOf[TraitDefinition])
    }

    def exclude(s: Shape): Boolean =
      excludeInternal(s) || Prelude.isPreludeShape(s) || traitShapes(s)

    def include(s: Shape): Boolean = allShapes || allRelevantShapes(s)
  }

  def compile(): List[OutputFile] = {
    val allProtocOptions = MetadataProcessor.extractProtocOptions(model)
    model.toShapeSet.toList
      .filter(ShapeFiltering.include)
      .filterNot(ShapeFiltering.exclude)
      .groupBy(_.getId().getNamespace())
      .flatMap { case (ns, shapes) =>
        val mappings = shapes.flatMap { shape =>
          shape
            .accept(topLevelDefsVisitor)
            .map(m => Statement.TopLevelStatement(m))
        }
        if (mappings.nonEmpty) {
          val options =
            allProtocOptions
              .getOrElse(ns, Map.empty)
              .map { case (key, value) =>
                TopLevelOption(key, value)
              }
              .toList
          val currentFqn = Namespacing.namespaceToFqn(ns)
          val imports = mappings
            .map(resolveImports)
            .flatMap(_.toList)
            .filter(_ != currentFqn)
            .map { case fqn =>
              Statement.ImportStatement(filePath(fqn).mkString("/"))
            }
            .distinct
          val unit = CompilationUnit(Some(ns), imports ++ mappings, options)
          List(OutputFile(filePath(currentFqn), unit))
        } else Nil
      }
      .toList
  }

  private def filePath(fqn: Fqn): List[String] = {
    val fileName = s"${toSnakeCase(fqn.name)}.proto"
    val prefix = fqn.packageName.toList.flatten
    prefix :+ fileName
  }

  private def toSnakeCase(name: String): String = {
    val (_, result) = name
      .foldLeft((false, "")) { case ((wasLastUpper, acc), i) =>
        val hasTrailingUnderscore = acc.lastOption.contains('_')
        val maybeUnderscore =
          if (
            i.isUpper &&
            acc.nonEmpty &&
            !wasLastUpper &&
            !hasTrailingUnderscore
          ) "_"
          else ""
        (i.isUpper, acc + maybeUnderscore + i.toLower)
      }
    result
  }

  private def findFieldIndex(m: MemberShape): Option[Int] =
    m.getTrait(classOf[ProtoIndexTrait]).toScala.map(_.getNumber)

  private def hasProtoWrapped(m: Shape): Boolean =
    m.hasTrait(classOf[alloy.proto.ProtoWrappedTrait])

  private def isProtoService(ss: ServiceShape): Boolean =
    ss.hasTrait(classOf[ProtoEnabledTrait])

  @annotation.nowarn("msg=class EnumTrait in package (.*)traits is deprecated")
  private def getEnumTrait(s: Shape): Option[EnumTrait] =
    s.getTrait(classOf[EnumTrait]).toScala

  private def resolveImports(statement: Statement): Set[Fqn] = {
    def resolveMessage(message: Message): Set[Fqn] =
      message.elements
        .map(resolveMessageElement)
        .foldLeft[Set[Fqn]](Set.empty)(_ ++ _)
    def resolveMessageElement(elem: MessageElement): Set[Fqn] =
      elem match {
        case MessageElement.FieldElement(field) => resolveField(field)
        case MessageElement.OneofElement(oneof) => resolveOneof(oneof)
        case _                                  => Set.empty
      }
    def resolveOneof(oneof: Oneof): Set[Fqn] =
      oneof.fields.map(resolveField).foldLeft[Set[Fqn]](Set.empty)(_ ++ _)

    def resolveField(field: Field): Set[Fqn] = field.ty.importFqn

    def resolveService(service: Service): Set[Fqn] =
      service.rpcs
        .map(s => Set(s.request.importFqn, s.response.importFqn))
        .foldLeft[Set[Fqn]](Set.empty)(_ ++ _)
    def resolveTopLevelDef(topLevelDef: TopLevelDef): Set[Fqn] =
      topLevelDef match {
        case TopLevelDef.MessageDef(msg) => resolveMessage(msg)
        case TopLevelDef.EnumDef(_)      => Set.empty
        case TopLevelDef.ServiceDef(s)   => resolveService(s)
      }
    statement match {
      case Statement.TopLevelStatement(t) => resolveTopLevelDef(t)
      case _                              => Set.empty
    }
  }

  type TopLevelDefs = List[TopLevelDef]
  type UnionMappings = Map[ShapeId, TopLevelDef]

  private object topLevelDefsVisitor
      extends ShapeVisitor.Default[TopLevelDefs] {
    private def topLevelMessage(shape: Shape, ty: Type) = {
      val name = shape.getId.getName
      val isDeprecated = shape.hasTrait(classOf[DeprecatedTrait])
      val field =
        Field(deprecated = isDeprecated, ty, "value", 1)
      val message =
        Message(name, List(MessageElement.FieldElement(field)), Nil)
      List(TopLevelDef.MessageDef(message))
    }

    private def isSimpleShape(shape: Shape): Boolean =
      shape.getType().getCategory() == ShapeType.Category.SIMPLE

    override def getDefault(shape: Shape): TopLevelDefs =
      if (isSimpleShape(shape) && hasProtoWrapped(shape)) {
        val maybeNumType = shape
          .getTrait(classOf[ProtoNumTypeTrait])
          .toScala
          .map(_.getNumType())
        val maybeType = shape.accept(typeVisitor(false, maybeNumType))
        maybeType.toList.flatMap(topLevelMessage(shape, _))
      } else Nil

    // TODO: streaming requests and response types
    override def serviceShape(shape: ServiceShape): TopLevelDefs =
      // TODO: is this the best place to do the filtering? or should it be done in a preprocessing phase
      if (isProtoService(shape)) {
        val operations = shape.getOperations.asScala.toList
          .map(model.expectShape(_))

        val defs = operations.flatMap(_.accept(this))
        val rpcs = operations.flatMap(_.accept(rpcVisitor))
        val service = Service(shape.getId.getName, rpcs)

        List(TopLevelDef.ServiceDef(service)) ++ defs
      } else Nil

    @annotation.nowarn(
      "msg=class EnumTrait in package (.*)traits is deprecated"
    )
    override def stringShape(shape: StringShape): TopLevelDefs = {
      val name = shape.getId.getName
      getEnumTrait(shape).map { (et: EnumTrait) =>
        val reserved = getReservedValues(shape)
        val elements = et
          .getValues()
          .asScala
          .toList
          .zipWithIndex
          .map { case (ed, edFieldNumber) =>
            val eName = ed
              .getName()
              .toScala
              .getOrElse(
                sys.error(
                  s"Error on shape: ${shape.getId()}: `enum` should have `name` defined."
                )
              )

            EnumValue(eName, edFieldNumber)
          }

        List(TopLevelDef.EnumDef(Enum(name, elements, reserved)))
      } getOrElse {
        if (shape.hasTrait(classOf[ProtoWrappedTrait])) {
          topLevelMessage(shape, Type.String)
        } else Nil
      }
    }

    private def shouldWrapCollection(shape: Shape): Boolean = {
      val hasWrapped = hasProtoWrapped(shape)
      val membersTargetingThis =
        model.getMemberShapes().asScala.filter(_.getTarget() == shape.getId())
      val isTargetedByWrappedMember =
        membersTargetingThis.exists(hasProtoWrapped(_))
      // oneofs cannot have lists / maps fields
      val isTargetedByUnionMember =
        membersTargetingThis.exists(member =>
          model.expectShape(member.getContainer()).isUnionShape
        )

      hasWrapped || isTargetedByWrappedMember || isTargetedByUnionMember
    }

    override def listShape(shape: ListShape): TopLevelDefs = {
      if (shouldWrapCollection(shape)) {
        shape.getMember().accept(typeVisitor()).toList.flatMap { tpe =>
          topLevelMessage(shape, Type.ListType(tpe))
        }
      } else Nil
    }

    override def mapShape(shape: MapShape): TopLevelDefs = {
      if (shouldWrapCollection(shape)) {
        for {
          keyType <- shape.getKey().accept(typeVisitor()).toList
          valueType <- shape.getValue().accept(typeVisitor()).toList
          result <- topLevelMessage(shape, Type.MapType(keyType, valueType))
        } yield result
      } else Nil
    }

    override def enumShape(shape: EnumShape): TopLevelDefs = {
      if (shape.hasTrait(classOf[OpenEnumTrait])) {
        Nil
      } else {
        val reserved: List[Reserved] = getReservedValues(shape)
        val elements: List[EnumValue] =
          shape.members.asScala.toList.zipWithIndex
            .map { case (member, edFieldNumber) =>
              val fieldIndex = findFieldIndex(member).getOrElse(edFieldNumber)
              EnumValue(enumValueName(member), fieldIndex)
            }
        List(
          TopLevelDef.EnumDef(
            Enum(shape.getId.getName, elements, reserved)
          )
        )
      }
    }

    override def intEnumShape(shape: IntEnumShape): TopLevelDefs = {
      if (shape.hasTrait(classOf[OpenEnumTrait])) {
        Nil
      } else {
        val reserved: List[Reserved] = getReservedValues(shape)
        val elements = shape.members.asScala.toList.map { member =>
          val enumValue =
            member.expectTrait(classOf[EnumValueTrait]).expectIntValue()
          val protoIndex = member
            .getTrait(classOf[ProtoIndexTrait])
            .toScala
            .map(_.getNumber())
            .getOrElse(enumValue)
          EnumValue(enumValueName(member), protoIndex)
        }
        List(
          TopLevelDef.EnumDef(
            Enum(shape.getId.getName, elements, reserved)
          )
        )
      }
    }

    private def unionShouldBeInlined(shape: UnionShape): Boolean = {
      shape.hasTrait(classOf[alloy.proto.ProtoInlinedOneOfTrait])
    }

    override def unionShape(shape: UnionShape): TopLevelDefs = {
      if (!unionShouldBeInlined(shape)) {
        val element =
          MessageElement.OneofElement(processUnion("definition", shape, 1))
        val name = shape.getId.getName
        val reserved = getReservedValues(shape)
        val message = Message(name, List(element), reserved)
        List(TopLevelDef.MessageDef(message))
      } else {
        List.empty
      }
    }

    override def structureShape(shape: StructureShape): TopLevelDefs = {
      val name = shape.getId.getName
      val messageElements =
        shape.members.asScala.toList
          // using foldLeft to accumulate the field count when we fork to
          // process a union
          .foldLeft((List.empty[MessageElement], 0)) {
            case ((fields, fieldCount), m) =>
              val fieldName = m.getMemberName
              val fieldIndex = findFieldIndex(m).getOrElse(fieldCount + 1)
              val targetShape = model.expectShape(m.getTarget)
              targetShape
                .asUnionShape()
                .toScala
                .filter(unionShape => unionShouldBeInlined(unionShape))
                .map { union =>
                  val field = MessageElement.OneofElement(
                    processUnion(fieldName, union, fieldIndex)
                  )
                  (fields :+ field, fieldCount + field.oneof.fields.size)
                }
                .getOrElse {
                  val isDeprecated = m.hasTrait(classOf[DeprecatedTrait])
                  val fieldType =
                    if (hasProtoWrapped(targetShape)) {
                      Type.RefType(targetShape)
                    } else {
                      val numType = extractNumType(m)
                      val maybeTimestampFormat = extractTimestampFormat(m)
                      val wrapped = hasProtoWrapped(m)
                      targetShape
                        .accept(
                          typeVisitor(
                            isWrapped = wrapped,
                            numType = numType,
                            timestampFormat = maybeTimestampFormat
                          )
                        )
                        .get
                    }
                  val field = MessageElement.FieldElement(
                    Field(
                      deprecated = isDeprecated,
                      fieldType,
                      fieldName,
                      fieldIndex
                    )
                  )
                  (fields :+ field, fieldCount + 1)
                }
          }
          ._1

      val reserved = getReservedValues(shape)
      val message = Message(name, messageElements, reserved)
      List(TopLevelDef.MessageDef(message))
    }

    private def processUnion(
        name: String,
        shape: UnionShape,
        indexStart: Int
    ): Oneof = {
      val fields = shape.members.asScala.toList.zipWithIndex.map {
        case (m, fn) =>
          val fieldName = m.getMemberName
          val fieldIndex = findFieldIndex(m).getOrElse(indexStart + fn)
          // We assume the model is well-formed so the result should be non-null
          val targetShape = model.expectShape(m.getTarget)
          val numType = extractNumType(m)
          val isWrapped = {
            val memberHasWrapped = hasProtoWrapped(m)
            val targetHasWrapped = hasProtoWrapped(targetShape)
            // repeated / map fields cannot be in oneofs
            val isList = targetShape.isListShape()
            val isMap = targetShape.isMapShape()
            memberHasWrapped || targetHasWrapped || isList || isMap
          }
          val fieldType =
            targetShape
              .accept(typeVisitor(isWrapped = isWrapped, numType))
              .get
          val isDeprecated = m.hasTrait(classOf[DeprecatedTrait])
          Field(
            deprecated = isDeprecated,
            fieldType,
            fieldName,
            fieldIndex
          )
      }
      Oneof(name, fields)
    }
  }

  private def getReservedValues(shape: Shape): List[Reserved] =
    shape
      .getTrait(classOf[ProtoReservedFieldsTrait])
      .toScala
      .fold(List.empty[ProtoReservedFieldsTraitValue])(t =>
        t.getReserved().asScala.toList
      )
      .collect {
        case r if r.isNumber => Reserved.Number(r.number)
        case r if r.isName   => Reserved.Name(r.name)
        case r if r.isRange  => Reserved.Range(r.range.start, r.range.end)
      }

  // TODO: collisions in synthesized name

  private object rpcVisitor extends ShapeVisitor.Default[Option[Rpc]] {
    override def getDefault(shape: Shape): Option[Rpc] = None
    override def operationShape(shape: OperationShape): Option[Rpc] = {
      val maybeInputShapeId = shape.getInput()
      val outputShapeId = shape.getOutput().get()
      val request = maybeInputShapeId.toScala
        .map { inputShapeId =>
          RpcMessage(
            Namespacing.shapeIdToFqn(inputShapeId),
            Namespacing.namespaceToFqn(inputShapeId.getNamespace())
          )
        }
        .getOrElse {
          RpcMessage(Type.Empty.fqn, Type.Empty.fqn)
        }

      val response = RpcMessage(
        Namespacing.shapeIdToFqn(outputShapeId),
        Namespacing.namespaceToFqn(outputShapeId.getNamespace())
      )
      Some(Rpc(shape.getId.getName, false, request, false, response))
    }
  }

  private def extractNumType(
      shape: Shape
  ): Option[ProtoNumTypeTrait.NumType] = {
    shape
      .getTrait(classOf[ProtoNumTypeTrait])
      .toScala
      .map { _.getNumType() }
  }

  private def extractTimestampFormat(
      shape: Shape
  ): Option[ProtoTimestampFormatTrait.TimestampFormat] = {
    shape
      .getTrait(classOf[ProtoTimestampFormatTrait])
      .toScala
      .map(_.getTimestampFormat())
  }

  private def isUnit(shape: StructureShape): Boolean = {
    shape
      .getTrait(classOf[UnitTypeTrait])
      .isPresent()
  }

  // Mapping between Smithy types and Protobuf 3 types:
  // https://developers.google.com/protocol-buffers/docs/proto3#scalar
  // https://awslabs.github.io/smithy/1.0/spec/core/model.html#simple-shapes
  // TODO: namespace in type?
  private def typeVisitor(
      isWrapped: Boolean = false,
      numType: Option[ProtoNumTypeTrait.NumType] = None,
      timestampFormat: Option[ProtoTimestampFormatTrait.TimestampFormat] = None
  ): ShapeVisitor[Option[Type]] =
    new ShapeVisitor[Option[Type]] {
      def bigDecimalShape(shape: BigDecimalShape): Option[Type] = Some {
        if (!isWrapped) Type.String
        else Type.AlloyWrappers.BigDecimal
      }
      def bigIntegerShape(shape: BigIntegerShape): Option[Type] = Some {
        if (!isWrapped) Type.String
        else Type.AlloyWrappers.BigInteger
      }
      def blobShape(shape: BlobShape): Option[Type] = Some {
        if (!isWrapped) Type.Bytes
        else Type.GoogleWrappers.Bytes
      }
      def booleanShape(shape: BooleanShape): Option[Type] = Some {
        if (!isWrapped) Type.Bool
        else Type.GoogleWrappers.Bool
      }
      def byteShape(shape: ByteShape): Option[Type] = Some {
        if (!isWrapped) Type.Int32
        else Type.AlloyWrappers.ByteValue
      }
      def documentShape(shape: DocumentShape): Option[Type] = Some {
        if (!isWrapped) Type.GoogleValue
        else Type.AlloyWrappers.Document
      }

      def doubleShape(shape: DoubleShape): Option[Type] = Some {
        if (!isWrapped) Type.Double
        else Type.GoogleWrappers.Double
      }
      def floatShape(shape: FloatShape): Option[Type] = Some {
        if (!isWrapped) Type.Float
        else Type.GoogleWrappers.Float
      }
      def shortShape(shape: ShortShape): Option[Type] = Some {
        if (!isWrapped) Type.Int32
        else Type.AlloyWrappers.ShortValue
      }
      def integerShape(shape: IntegerShape): Option[Type] = Some {
        NumberType.resolveInt(isWrapped, numType)
      }
      def longShape(shape: LongShape): Option[Type] = Some {
        NumberType.resolveLong(isWrapped, numType)
      }

      def listShape(shape: ListShape): Option[Type] = {
        if (isWrapped) Some(Type.RefType(shape))
        else shape.getMember().accept(typeVisitor()).map(Type.ListType(_))
      }

      def mapShape(shape: MapShape): Option[Type] = {
        if (isWrapped) Some(Type.RefType(shape))
        else
          for {
            key <- shape.getKey().accept(typeVisitor())
            value <- shape.getValue().accept(typeVisitor())
          } yield Type.MapType(key, value)
      }

      def memberShape(shape: MemberShape): Option[Type] = {
        val target = model.expectShape(shape.getTarget())
        val memberHasWrapped = shape.hasTrait(classOf[ProtoWrappedTrait])
        val targetHasWrapped = target.hasTrait(classOf[ProtoWrappedTrait])
        val isWrapped = memberHasWrapped || targetHasWrapped
        val numType =
          shape
            .getTrait(classOf[ProtoNumTypeTrait])
            .or(() => target.getTrait(classOf[ProtoNumTypeTrait]))
            .toScala
            .map(_.getNumType())
        val timestampFormatValue = extractTimestampFormat(shape)

        target.accept(
          typeVisitor(
            isWrapped = isWrapped,
            numType = numType,
            timestampFormat = timestampFormatValue
          )
        )
      }

      def operationShape(shape: OperationShape): Option[Type] = None
      def resourceShape(shape: ResourceShape): Option[Type] = None
      def serviceShape(shape: ServiceShape): Option[Type] = None

      def stringShape(shape: StringShape): Option[Type] = Some {
        val hasUUIDFormat = shape.hasTrait(classOf[alloy.UuidFormatTrait])
        val hasProtoCompactUUID =
          shape.hasTrait(classOf[alloy.proto.ProtoCompactUUIDTrait])
        if (hasUUIDFormat && hasProtoCompactUUID) Type.AlloyTypes.CompactUUID
        else if (!isWrapped) Type.String
        else Type.GoogleWrappers.String
      }
      override def enumShape(shape: EnumShape): Option[Type] = {
        if (shape.hasTrait(classOf[OpenEnumTrait])) {
          Some(Type.String)
        } else {
          Some(Type.RefType(shape))
        }
      }
      override def intEnumShape(shape: IntEnumShape): Option[Type] = {
        if (shape.hasTrait(classOf[OpenEnumTrait])) {
          Some(Type.Int32)
        } else {
          Some(Type.RefType(shape))
        }
      }

      def structureShape(shape: StructureShape): Option[Type] = Some {
        if (isUnit(shape))
          Type.Empty
        else
          Type.RefType(shape)
      }

      def timestampShape(shape: TimestampShape): Option[Type] = Some {
        val format =
          extractTimestampFormat(shape)
            .orElse(timestampFormat)
            .getOrElse(ProtoTimestampFormatTrait.TimestampFormat.PROTOBUF)

        format match {
          case TimestampFormat.PROTOBUF | TimestampFormat.UNKNOWN =>
            if (isWrapped) Type.AlloyWrappers.Timestamp
            else Type.GoogleTimestamp
          case TimestampFormat.EPOCH_MILLIS =>
            if (isWrapped) Type.AlloyWrappers.EpochMillisTimestamp
            else Type.AlloyTypes.EpochMillisTimestamp
        }
      }

      def unionShape(shape: UnionShape): Option[Type] = Some(
        Type.RefType(shape)
      )
    }

  // TODO: Traits for big decimal, big integer, timestamp serialization into proto
  // PRoto3 metatrait
  // TODO: validation events specifically for proto

  private object NumberType {
    def resolveLong(
        isWrapped: Boolean,
        maybeNumType: Option[ProtoNumTypeTrait.NumType]
    ): Type = {
      import ProtoNumTypeTrait.NumType._
      (isWrapped, maybeNumType) match {
        case (false, Some(SIGNED))       => Type.Sint64
        case (false, Some(UNSIGNED))     => Type.Uint64
        case (false, Some(FIXED))        => Type.Fixed64
        case (false, Some(FIXED_SIGNED)) => Type.Sfixed64
        case (false, Some(UNKNOWN))      => Type.Int64
        case (false, None)               => Type.Int64
        case (true, Some(SIGNED))        => Type.AlloyWrappers.SInt64
        case (true, Some(UNSIGNED))      => Type.GoogleWrappers.Uint64
        case (true, Some(FIXED))         => Type.AlloyWrappers.Fixed64
        case (true, Some(FIXED_SIGNED))  => Type.AlloyWrappers.SFixed64
        case (true, Some(UNKNOWN))       => Type.GoogleWrappers.Int64
        case (true, None)                => Type.GoogleWrappers.Int64
      }
    }
    def resolveInt(
        isWrapped: Boolean,
        maybeNumType: Option[ProtoNumTypeTrait.NumType]
    ): Type = {
      import ProtoNumTypeTrait.NumType._
      (isWrapped, maybeNumType) match {
        case (false, Some(SIGNED))       => Type.Sint32
        case (false, Some(UNSIGNED))     => Type.Uint32
        case (false, Some(FIXED))        => Type.Fixed32
        case (false, Some(FIXED_SIGNED)) => Type.Sfixed32
        case (false, Some(UNKNOWN))      => Type.Int32
        case (false, None)               => Type.Int32
        case (true, Some(SIGNED))        => Type.AlloyWrappers.SInt32
        case (true, Some(UNSIGNED))      => Type.GoogleWrappers.Uint32
        case (true, Some(FIXED))         => Type.AlloyWrappers.Fixed32
        case (true, Some(FIXED_SIGNED))  => Type.AlloyWrappers.SFixed32
        case (true, Some(UNKNOWN))       => Type.GoogleWrappers.Int32
        case (true, None)                => Type.GoogleWrappers.Int32
      }
    }
  }
}
