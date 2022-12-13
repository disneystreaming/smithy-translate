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

import alloy.proto._
import smithytranslate.closure.ModelOps._
import software.amazon.smithy.model.loader.Prelude
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes._
import software.amazon.smithy.model.traits.DeprecatedTrait
import software.amazon.smithy.model.traits.EnumTrait
import software.amazon.smithy.model.traits.RequiredTrait
import software.amazon.smithy.model.traits.SparseTrait
import software.amazon.smithy.model.traits.UnitTypeTrait
import software.amazon.smithy.model.traits.TraitDefinition

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

class Compiler() {

  // Reference:
  // 1. https://github.com/protocolbuffers/protobuf/blob/master/docs/field_presence.md

  import ProtoIR._

  /** these exclusions are performed as a last step to avoid shapes like
    * `structure protoEnabled {}` to be rendered as proto messages.
    *
    * this is done here, rather than in pre-processing, because removing the
    * shape entirely from the model would remove the trait it represents and
    * affect the compiler behaviour
    */
  object ShapeFiltering {

    private val passthroughShapeIds: Set[ShapeId] =
      Set(
        "BigInteger",
        "BigDecimal",
        "Timestamp",
        "UUID"
      ).map(name => ShapeId.fromParts("smithytranslate", name))
    private def excludeInternal(shape: Shape): Boolean = {
      val excludeNs = Set("alloy.proto", "alloy", "smithytranslate")
      excludeNs.contains(shape.getId().getNamespace()) &&
      !passthroughShapeIds(shape.getId())
    }

    def traitShapes(s: Shape): Boolean = {
      s.hasTrait(classOf[TraitDefinition])
    }

    def exclude(s: Shape): Boolean =
      excludeInternal(s) || Prelude.isPreludeShape(s) || traitShapes(s)
  }

  /** Unused union shape are not exported.
    *
    * Union shapes used exactly once are exported within the structure that uses
    * them.
    *
    * If they're used more than once, this function will throw.
    */
  private def validateUnionShapes(model: Model): Unit = {
    model
      .getUnionShapes()
      .asScala
      .foreach { shape =>
        val count = unionUsageCount(model, shape)
        if (count > 1) {
          sys.error(
            s"Protobuf unions are defined within a message. Therefore, the union shape can only be used within at most one structure."
          )
        }
      }
  }

  def compile(model: Model): List[OutputFile] = {
    validateUnionShapes(model)

    val allProtocOptions = MetadataProcessor.extractProtocOptions(model)

    model.toShapeSet.toList
      .filterNot(ShapeFiltering.exclude)
      .groupBy(_.getId().getNamespace())
      .flatMap { case (ns, shapes) =>
        val mappings = shapes.flatMap { shape =>
          shape
            .accept(compileVisitor(model))
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

  private def toSnakeCase(name: String): String =
    name.split("(?=\\p{Upper})").map(_.toLowerCase).mkString("_")

  private def findFieldIndex(m: MemberShape): Option[Int] =
    m.getTrait(classOf[ProtoIndexTrait]).toScala.map(_.getNumber)

  private def isRequired(m: Shape): Boolean =
    m.hasTrait(classOf[RequiredTrait])

  private def isProtoService(ss: ServiceShape): Boolean =
    ss.hasTrait(classOf[ProtoEnabledTrait])

  @annotation.nowarn("msg=class EnumTrait in package traits is deprecated")
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

  type Mappings = List[TopLevelDef]

  private def unionUsageCount(model: Model, shape: UnionShape): Int = {
    model
      .getMemberShapes()
      .asScala
      .count(_.getTarget() == shape.getId())
  }

  private def compileVisitor(model: Model): ShapeVisitor[Mappings] =
    new ShapeVisitor.Default[Mappings] {
      private def topLevelMessage(shape: Shape, ty: Type) = {
        val name = shape.getId.getName
        val isDeprecated = shape.hasTrait(classOf[DeprecatedTrait])
        val field =
          Field(repeated = false, deprecated = isDeprecated, ty, "value", 1)
        val message =
          Message(name, List(MessageElement.FieldElement(field)), Nil)
        List(TopLevelDef.MessageDef(message))
      }
      override def getDefault(shape: Shape): Mappings = Nil

      override def bigIntegerShape(shape: BigIntegerShape): Mappings = {
        topLevelMessage(shape, Type.BigInteger)
      }

      override def bigDecimalShape(shape: BigDecimalShape): Mappings = {
        topLevelMessage(shape, Type.BigDecimal)
      }
      override def timestampShape(shape: TimestampShape): Mappings = {
        topLevelMessage(shape, Type.Timestamp)
      }

      // TODO: streaming requests and response types
      override def serviceShape(shape: ServiceShape): Mappings =
        // TODO: is this the best place to do the filtering? or should it be done in a preprocessing phase
        if (isProtoService(shape)) {
          val operations = shape.getOperations.asScala.toList
            .map(model.expectShape(_))

          val defs = operations.flatMap(_.accept(this))
          val rpcs = operations.flatMap(_.accept(rpcVisitor))
          val service = Service(shape.getId.getName, rpcs)

          List(TopLevelDef.ServiceDef(service)) ++ defs
        } else Nil

      override def booleanShape(shape: BooleanShape): Mappings = {
        topLevelMessage(shape, Type.Bool)
      }

      override def blobShape(shape: BlobShape): Mappings = {
        topLevelMessage(shape, Type.Bytes)
      }

      override def integerShape(shape: IntegerShape): Mappings = {
        topLevelMessage(shape, Type.Int32)
      }

      override def longShape(shape: LongShape): Mappings = {
        topLevelMessage(shape, Type.Int64)
      }

      override def doubleShape(shape: DoubleShape): Mappings = {
        topLevelMessage(shape, Type.Double)
      }

      override def shortShape(shape: ShortShape): Mappings = {
        topLevelMessage(shape, Type.Int32)
      }

      override def floatShape(shape: FloatShape): Mappings = {
        topLevelMessage(shape, Type.Float)
      }

      override def documentShape(shape: DocumentShape): Mappings = {
        topLevelMessage(shape, Type.Any)
      }

      override def stringShape(shape: StringShape): Mappings = {
        val name = shape.getId.getName
        getEnumTrait(shape).map { et =>
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
          topLevelMessage(shape, Type.String)
        }
      }

      override def enumShape(shape: EnumShape): Mappings = {
        val reserved: List[Reserved] = getReservedValues(shape)
        val elements: List[EnumValue] =
          shape.getAllMembers.asScala.toList.zipWithIndex
            .map { case ((name, member), edFieldNumber) =>
              val fieldIndex = findFieldIndex(member).getOrElse(edFieldNumber)
              EnumValue(name, fieldIndex)
            }
        List(
          TopLevelDef.EnumDef(
            Enum(shape.getId.getName, elements, reserved)
          )
        )
      }

      override def intEnumShape(shape: IntEnumShape): Mappings = {
        val reserved: List[Reserved] = getReservedValues(shape)
        val elements = shape.getEnumValues.asScala.toList.map {
          case (name, value) =>
            EnumValue(name, value)
        }
        List(
          TopLevelDef.EnumDef(
            Enum(shape.getId.getName, elements, reserved)
          )
        )
      }

      override def structureShape(shape: StructureShape): Mappings = {
        val name = shape.getId.getName
        val messageElements =
          shape.members.asScala.toList
            // using foldLeft to accumulate the field count when we fork to
            // process a union
            .foldLeft((List.empty[MessageElement], 0)) {
              case ((fields, fieldCount), m) =>
                val fieldName = m.getMemberName
                val fieldIndex = findFieldIndex(m).getOrElse(fieldCount + 1)
                // We assume the model is well-formed so the result should be non-null
                val targetShape = model.getShape(m.getTarget).get
                targetShape
                  .asUnionShape()
                  .toScala
                  .map { union =>
                    val field = MessageElement.OneofElement(
                      proccessUnion(fieldName, union, fieldIndex)
                    )
                    (fields :+ field, fieldCount + field.oneof.fields.size)
                  }
                  .getOrElse {
                    val isDeprecated = m.hasTrait(classOf[DeprecatedTrait])
                    val isBoxed = isRequired(m) || isRequired(targetShape)
                    val numType = extractNumType(m)
                    val fieldType =
                      targetShape
                        .accept(typeVisitor(model, isBoxed, numType))
                        .get
                    val field = MessageElement.FieldElement(
                      Field(
                        repeated = false,
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

      private def proccessUnion(
          name: String,
          shape: UnionShape,
          indexStart: Int
      ): Oneof = {
        val fields = shape.members.asScala.toList.zipWithIndex.map {
          case (m, fn) =>
            val fieldName = m.getMemberName
            val fieldIndex = findFieldIndex(m).getOrElse(indexStart + fn)
            // We assume the model is well-formed so the result should be non-null
            val targetShape = model.getShape(m.getTarget).get
            val numType = extractNumType(m)
            val fieldType =
              targetShape
                .accept(typeVisitor(model, isRequired = true, numType))
                .get
            val isDeprecated = m.hasTrait(classOf[DeprecatedTrait])
            Field(
              repeated = false,
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

  private def rpcVisitor: ShapeVisitor[Option[Rpc]] =
    new ShapeVisitor.Default[Option[Rpc]] {
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

  private def isSparse(shape: Shape): Boolean = {
    shape
      .getTrait(classOf[SparseTrait])
      .isPresent()
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
      model: Model,
      isRequired: Boolean,
      numType: Option[ProtoNumTypeTrait.NumType]
  ): ShapeVisitor[Option[Type]] =
    new ShapeVisitor[Option[Type]] {
      def bigDecimalShape(shape: BigDecimalShape): Option[Type] = Some({
        if (Prelude.isPreludeShape(shape.getId())) {
          Type.BigDecimal
        } else {
          Type.MessageType(
            Namespacing.shapeIdToFqn(shape.getId),
            Namespacing.shapeIdToImportFqn(shape.getId())
          )
        }
      })
      def bigIntegerShape(shape: BigIntegerShape): Option[Type] = Some({
        if (Prelude.isPreludeShape(shape.getId())) {
          Type.BigInteger
        } else {
          Type.MessageType(
            Namespacing.shapeIdToFqn(shape.getId),
            Namespacing.shapeIdToImportFqn(shape.getId())
          )
        }
      })
      def blobShape(shape: BlobShape): Option[Type] = Some(
        if (isRequired && Prelude.isPreludeShape(shape.getId())) {
          Type.Bytes
        } else if (Prelude.isPreludeShape(shape.getId())) {
          Type.Wrappers.Bytes
        } else {
          Type.MessageType(
            Namespacing.shapeIdToFqn(shape.getId),
            Namespacing.shapeIdToImportFqn(shape.getId())
          )
        }
      )
      def booleanShape(shape: BooleanShape): Option[Type] = Some(
        if (isRequired && Prelude.isPreludeShape(shape.getId())) {
          Type.Bool
        } else if (Prelude.isPreludeShape(shape.getId())) {
          Type.Wrappers.Bool
        } else {
          Type.MessageType(
            Namespacing.shapeIdToFqn(shape.getId),
            Namespacing.shapeIdToImportFqn(shape.getId())
          )
        }
      )
      def byteShape(shape: ByteShape): Option[Type] =
        if (Prelude.isPreludeShape(shape.getId())) {
          Some(NumberType.resolveInt(isRequired, numType))
        } else {
          Some(
            Type.MessageType(
              Namespacing.shapeIdToFqn(shape.getId),
              Namespacing.shapeIdToImportFqn(shape.getId())
            )
          )
        }
      def documentShape(shape: DocumentShape): Option[Type] =
        Some(Type.Any)
      def doubleShape(shape: DoubleShape): Option[Type] =
        if (isRequired && Prelude.isPreludeShape(shape.getId()))
          Some(Type.Double)
        else if (Prelude.isPreludeShape(shape.getId()))
          Some(Type.Wrappers.Double)
        else
          Some(
            Type.MessageType(
              Namespacing.shapeIdToFqn(shape.getId),
              Namespacing.shapeIdToImportFqn(shape.getId())
            )
          )
      def floatShape(shape: FloatShape): Option[Type] =
        if (isRequired && Prelude.isPreludeShape(shape.getId()))
          Some(Type.Float)
        else if (Prelude.isPreludeShape(shape.getId()))
          Some(Type.Wrappers.Float)
        else
          Some(
            Type.MessageType(
              Namespacing.shapeIdToFqn(shape.getId),
              Namespacing.shapeIdToImportFqn(shape.getId())
            )
          )
      def integerShape(shape: IntegerShape): Option[Type] = {
        if (Prelude.isPreludeShape(shape.getId())) {
          Some(NumberType.resolveInt(isRequired, numType))
        } else {
          Some(
            Type.MessageType(
              Namespacing.shapeIdToFqn(shape.getId),
              Namespacing.shapeIdToImportFqn(shape.getId())
            )
          )
        }
      }
      def listShape(shape: ListShape): Option[Type] = {
        val memberShape = model.getShape(shape.getMember().getTarget()).get
        // to do sparse & numtype
        memberShape
          .accept(
            typeVisitor(model, isRequired = !isSparse(shape), numType = None)
          )
          .map(Type.ListType(_))
      }
      def longShape(shape: LongShape): Option[Type] = {
        if (Prelude.isPreludeShape(shape.getId())) {
          Some(NumberType.resolveLong(isRequired, numType))
        } else {
          Some(
            Type.MessageType(
              Namespacing.shapeIdToFqn(shape.getId),
              Namespacing.shapeIdToImportFqn(shape.getId())
            )
          )
        }
      }
      def mapShape(shape: MapShape): Option[Type] = {
        for {
          valueShape <- model.getShape(shape.getValue.getTarget).toScala
          valueType <- valueShape.accept(
            typeVisitor(model, isRequired = !isSparse(shape), numType = None)
          )
        } yield Type.MapType(Right(Type.String), valueType)
      }
      def memberShape(shape: MemberShape): Option[Type] = None
      def operationShape(shape: OperationShape): Option[Type] = None
      def resourceShape(shape: ResourceShape): Option[Type] = None
      def serviceShape(shape: ServiceShape): Option[Type] = None

      @annotation.nowarn("msg=class SetShape in package shapes is deprecated")
      override def setShape(shape: SetShape): Option[Type] = Some(
        Type.MessageType(
          Namespacing.shapeIdToFqn(shape.getId),
          Namespacing.shapeIdToImportFqn(shape.getId())
        )
      )
      def shortShape(shape: ShortShape): Option[Type] =
        if (Prelude.isPreludeShape(shape.getId())) {
          Some(NumberType.resolveInt(isRequired, numType))
        } else {
          Some(
            Type.MessageType(
              Namespacing.shapeIdToFqn(shape.getId),
              Namespacing.shapeIdToImportFqn(shape.getId())
            )
          )
        }
      // TODO: we are diverging from the spec here
      def stringShape(shape: StringShape): Option[Type] = Some(
        if (isRequired && Prelude.isPreludeShape(shape.getId())) {
          Type.String
        } else if (Prelude.isPreludeShape(shape.getId())) {
          Type.Wrappers.String
        } else {
          Type.MessageType(
            Namespacing.shapeIdToFqn(shape.getId),
            Namespacing.shapeIdToImportFqn(shape.getId())
          )
        }
      )
      override def enumShape(shape: EnumShape): Option[Type] = Some(
        Type.EnumType(
          Namespacing.shapeIdToFqn(shape.getId()),
          Namespacing.shapeIdToImportFqn(shape.getId())
        )
      )
      override def intEnumShape(shape: IntEnumShape): Option[Type] = Some(
        Type.EnumType(
          Namespacing.shapeIdToFqn(shape.getId()),
          Namespacing.shapeIdToImportFqn(shape.getId())
        )
      )
      def structureShape(shape: StructureShape): Option[Type] = {
        if (isUnit(shape)) {
          Some(Type.Empty)
        } else {
          Some(
            Type.MessageType(
              Namespacing.shapeIdToFqn(shape.getId),
              Namespacing.shapeIdToImportFqn(shape.getId())
            )
          )
        }
      }
      def timestampShape(shape: TimestampShape): Option[Type] = Some(
        if (Prelude.isPreludeShape(shape.getId())) {
          Type.Timestamp
        } else {
          Type.MessageType(
            Namespacing.shapeIdToFqn(shape.getId),
            Namespacing.shapeIdToImportFqn(shape.getId())
          )
        }
      )
      def unionShape(shape: UnionShape): Option[Type] = Some(
        Type.MessageType(
          Namespacing.shapeIdToFqn(shape.getId),
          Namespacing.shapeIdToImportFqn(shape.getId())
        )
      )
    }

  // TODO: Traits for big decimal, big integer, timestamp serialization into proto
  // PRoto3 metatrait
  // TODO: validation events specifically for proto

  private object NumberType {
    def resolveLong(
        isRequired: Boolean,
        maybeNumType: Option[ProtoNumTypeTrait.NumType]
    ): Type = {
      import ProtoNumTypeTrait.NumType._
      (isRequired, maybeNumType) match {
        case (true, Some(SIGNED))       => Type.Sint64
        case (true, Some(UNSIGNED))     => Type.Uint64
        case (true, Some(FIXED))        => Type.Fixed64
        case (true, Some(FIXED_SIGNED)) => Type.Sfixed64
        case (true, Some(UNKNOWN))      => Type.Int64
        case (true, None)               => Type.Int64
        case (false, Some(UNSIGNED))    => Type.Wrappers.Uint64
        case (false, Some(_))           => Type.Wrappers.Int64
        case (false, None)              => Type.Wrappers.Int64
      }
    }
    def resolveInt(
        isRequired: Boolean,
        maybeNumType: Option[ProtoNumTypeTrait.NumType]
    ): Type = {
      import ProtoNumTypeTrait.NumType._
      (isRequired, maybeNumType) match {
        case (true, Some(SIGNED))       => Type.Sint32
        case (true, Some(UNSIGNED))     => Type.Uint32
        case (true, Some(FIXED))        => Type.Fixed32
        case (true, Some(FIXED_SIGNED)) => Type.Sfixed32
        case (true, Some(UNKNOWN))      => Type.Int32
        case (true, None)               => Type.Int32
        case (false, Some(UNSIGNED))    => Type.Wrappers.Uint32
        case (false, Some(_))           => Type.Wrappers.Int32
        case (false, None)              => Type.Wrappers.Int32
      }
    }
  }
}
