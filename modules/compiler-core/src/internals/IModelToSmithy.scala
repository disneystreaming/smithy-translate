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

package smithytranslate.compiler.internals

import alloy.DiscriminatedUnionTrait
import alloy.NullableTrait
import alloy.DataExamplesTrait
import alloy.openapi.OpenApiExtensionsTrait
import alloy.UntaggedUnionTrait
import alloy.UuidFormatTrait
import cats.syntax.all._
import smithytranslate.ContentTypeDiscriminatedTrait
import smithytranslate.ContentTypeTrait
import smithytranslate.ErrorMessageTrait
import smithytranslate.NullFormatTrait
import smithytranslate.compiler.internals.Hint.Header
import smithytranslate.compiler.internals.Hint.QueryParam
import smithytranslate.compiler.internals.TimestampFormat._
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.pattern.UriPattern
import software.amazon.smithy.model.traits._
import software.amazon.smithy.model.traits.HttpTrait
import software.amazon.smithy.model.shapes._
import software.amazon.smithy.model.shapes.{Shape => JShape}

import scala.jdk.CollectionConverters._
import smithytranslate.ConstTrait

private[compiler] final class IModelToSmithy(useEnumTraitSyntax: Boolean)
    extends (IModel => Model) {

  def toStructure(s: Structure): JShape = {
    val members = s.localFields.map { case Field(id, tpe, hints) =>
      val memName = id.memberName.value.toString
      val nameWillNeedChange = sanitizeMemberName(memName) != memName
      def isHeaderOrQuery = hints.exists {
        case Header(_)     => true
        case QueryParam(_) => true
        case _             => false
      }
      val jsonNameHint =
        if (nameWillNeedChange && !isHeaderOrQuery)
          List(Hint.JsonName(memName))
        else List.empty

      val memberBuilder = MemberShape
        .builder()
        .id(id.toSmithy)
        .target(tpe.toSmithy)

      hintsToTraits(hints ++ jsonNameHint).foreach(memberBuilder.addTrait(_))
      memberBuilder.build()
    }
    val mixins = s.hints.collect { case Hint.HasMixin(defId) =>
      StructureShape.builder.id(defId.toSmithy).build()
    }.asJava
    val builder = StructureShape
      .builder()
      .id(s.id.toSmithy)

    hintsToTraits(s.hints).foreach(builder.addTrait(_))
    members.foreach(builder.addMember(_))
    mixins.forEach { m =>
      val _ = builder.addMixin(m)
    }
    builder.build()
  }

  def toMap(m: MapDef): JShape = {
    val builder = MapShape
      .builder()
      .id(m.id.toSmithy)
      .key(m.key.toSmithy)
      .value(m.value.toSmithy)

    hintsToTraits(m.hints).foreach(builder.addTrait(_))
    builder.build()
  }

  def toList(l: ListDef): JShape = {
    val builder = ListShape
      .builder()
      .id(l.id.toSmithy)
      .member(l.member.toSmithy)
    hintsToTraits(l.hints).foreach(builder.addTrait(_))
    builder.build()
  }

  def toSet(s: SetDef): JShape = {
    val builder = ListShape
      .builder()
      .id(s.id.toSmithy)
      .member(s.member.toSmithy)
      .addTrait(new UniqueItemsTrait())

    hintsToTraits(s.hints).foreach(builder.addTrait(_))
    builder.build()
  }

  def toUnion(u: Union): JShape = {
    val builder =
      UnionShape.builder().id(u.id.toSmithy)
    u.alts.foreach { alt =>
      val memName = alt.id.memberName.value.toString
      val nameWillNeedChange = sanitizeMemberName(memName) != memName
      val jsonNameHint =
        if (nameWillNeedChange)
          List(Hint.JsonName(memName))
        else List.empty
      val memberBuilder = MemberShape
        .builder()
        .id(alt.id.toSmithy)
        .target(alt.tpe.toSmithy)

      hintsToTraits(alt.hints ++ jsonNameHint)
        .foreach(memberBuilder.addTrait(_))
      builder.addMember(memberBuilder.build())
    }
    u.kind match {
      case UnionKind.Discriminated(d) =>
        builder.addTrait(new DiscriminatedUnionTrait(d))
      case UnionKind.Untagged => builder.addTrait(new UntaggedUnionTrait())
      case UnionKind.ContentTypeDiscriminated =>
        builder.addTrait(new ContentTypeDiscriminatedTrait())
      case UnionKind.Tagged => ()
    }
    hintsToTraits(u.hints).foreach(builder.addTrait(_))
    builder.build()
  }

  def toPrimitive(n: Newtype): JShape = {
    val builder
        : AbstractShapeBuilder[_ <: AbstractShapeBuilder[_, _], _ <: JShape] =
      n.target.name.segments.last.value.toString match {
        case "String"     => StringShape.builder()
        case "Integer"    => IntegerShape.builder()
        case "Long"       => LongShape.builder()
        case "BigInteger" => BigIntegerShape.builder()
        case "BigDecimal" => BigDecimalShape.builder()
        case "Short"      => ShortShape.builder()
        case "Float"      => FloatShape.builder()
        case "Double"     => DoubleShape.builder()
        case "Boolean"    => BooleanShape.builder()
        case "Byte"       => ByteShape.builder()
        case "Timestamp"  => TimestampShape.builder()
        case "Document"   => DocumentShape.builder()
        case "UUID" => StringShape.builder().addTrait(new UuidFormatTrait())
        case "Null" =>
          StructureShape.builder().addTrait(new NullFormatTrait())
        case other =>
          sys.error(
            s"error processing ${n.id}, found $other"
          )
      }
    hintsToTraits(n.hints).foreach(builder.addTrait(_))
    builder.id(n.id.toSmithy)
    builder.build()
  }

  def toOperation(op: OperationDef): JShape = {
    val builder =
      OperationShape.builder().id(op.id.toSmithy)
    op.output.foreach(o => builder.output(o.toSmithy))
    op.input.foreach(i => builder.input(i.toSmithy))
    hintsToTraits(op.hints).foreach(builder.addTrait(_))
    op.errors.foreach(e => builder.addError(e.toSmithy))
    builder.build()
  }

  def toService(s: ServiceDef): JShape = {
    val builder = ServiceShape
      .builder()
      .id(s.id.toSmithy)
    hintsToTraits(s.hints).foreach(builder.addTrait(_))
    s.operations.foreach(o => builder.addOperation(o.toSmithy))
    builder.build()
  }

  def apply(iModel: IModel): Model = {
    val shapes: Vector[JShape] = iModel.definitions.map {
      case s: Structure     => toStructure(s)
      case m: MapDef        => toMap(m)
      case l: ListDef       => toList(l)
      case s: SetDef        => toSet(s)
      case u: Union         => toUnion(u)
      case n: Newtype       => toPrimitive(n)
      case op: OperationDef => toOperation(op)
      case s: ServiceDef    => toService(s)
      case e: Enumeration   => buildEnum(e)
    }
    val builder = Model.builder()
    if (iModel.suppressions.nonEmpty) {
      builder.putMetadataProperty("suppressions", iModel.suppressions.toNode)
    }
    builder.addShapes(shapes.asJava)
    builder.build()
  }

  private def buildEnum(e: Enumeration): JShape = {
    val Enumeration(id, values, hints) = e
    if (useEnumTraitSyntax) {
      val enumTraitBuilder = EnumTrait.builder(): @annotation.nowarn(
        "msg=class EnumTrait in package traits is deprecated"
      )
      values.foreach(v =>
        enumTraitBuilder.addEnum(EnumDefinition.builder.value(v).build())
      )
      StringShape
        .builder()
        .id(id.toSmithy)
        .addTrait(enumTraitBuilder.build())
        .build()
    } else {
      val enumBuilder = EnumShape.builder().id(id.toSmithy)
      values.zipWithIndex.foreach { case (value, idx) =>
        val name = sanitizeEnumMember(value, idx)
        enumBuilder.addMember(name, value)
      }

      hintsToTraits(hints).foreach(enumBuilder.addTrait(_))
      enumBuilder.build()
    }

  }

  implicit class SuppressionOps(sups: Vector[Suppression]) {
    def toNode: Node = Node.arrayNode(
      sups
        .map { sup =>
          Node
            .objectNodeBuilder()
            .withMember("id", sup.id)
            .withMember("namespace", sanitizeNamespace(sup.namespace.show))
            .withMember("reason", sup.reason)
            .build()
        }: _*
    )
  }

  implicit class IdOps(id: Id) {
    def toSmithy: ShapeId = id match {
      case DefId(namespace, name) =>
        ShapeId.fromParts(
          sanitizeNamespace(namespace.show),
          sanitizeName(name.segments.mkString_(""))
        )
      case MemberId(modelId, memberName) =>
        ShapeId.fromParts(
          sanitizeNamespace(modelId.namespace.show),
          sanitizeName(modelId.name.segments.mkString_("")),
          sanitizeMemberName(memberName.value.toString)
        )
    }
  }

  private def removeInvalidCharactersForName(s: String): String = {
    s.filter(c =>
      c.isLetterOrDigit || c == '_'
    ) // names can only contain letters, digits, and underscores
  }

  private def removeInvalidCharactersForNamespace(s: String): String = {
    s.filter(c =>
      c.isLetterOrDigit || c == '_' || c == '.'
    ) // names can only contain letters, digits, underscores, and dots
  }

  // if name starts with a number, prefix with n
  private def sanitizeForDigitStart(id: String): String =
    if (id.headOption.exists(_.isDigit)) s"n$id"
    else id

  private def sanitizeNamespace(id: String): String = {
    removeInvalidCharactersForNamespace(
      sanitizeForDigitStart(id).replaceAll("-", "_")
    )
  }

  private def sanitizeName(id: String): String = {
    removeInvalidCharactersForName(
      StringUtil.toCamelCase(sanitizeForDigitStart(id))
    )
  }

  private def sanitizeMemberName(id: String): String = {
    sanitizeForDigitStart(
      removeInvalidCharactersForName(id.replaceAll("-", "_"))
    )
  }

  private def sanitizeEnumMember(value: String, idx: Int): String = {
    val out = sanitizeMemberName(value)
    if (out.isEmpty()) s"MEMBER_$idx"
    else out
  }

  /** Used to replace things like `/path/{some-case}/rest with
    * `/path/{some_case}/rest`. This ensures the memberName matches the uri
    * segment.
    */
  private def sanitizedUriPath(uriPath: String): String = {
    val parts = uriPath.split("/")
    if (parts.isEmpty) {
      uriPath
    } else {
      parts
        .filter(_.trim.nonEmpty)
        .map { part =>
          if (part.startsWith("{") && part.endsWith("}")) {
            val safe = sanitizeMemberName(part.drop(1).dropRight(1))
            s"{$safe}"
          } else {
            part
          }
        }
        .mkString("/", "/", "")
    }
  }

  private def hintsToTraits(hints: List[Hint]) = hints.flatMap {
    case Hint.Description(value)      => List(new DocumentationTrait(value))
    case Hint.Body                    => List(new HttpPayloadTrait())
    case Hint.Header(name)            => List(new HttpHeaderTrait(name))
    case Hint.PathParam(_)            => List(new HttpLabelTrait())
    case Hint.QueryParam(name)        => List(new HttpQueryTrait(name))
    case Hint.Required                => List(new RequiredTrait())
    case Hint.Pattern(value)          => List(new PatternTrait(value))
    case Hint.Sensitive               => List(new SensitiveTrait())
    case Hint.ContentTypeLabel(value) => List(new ContentTypeTrait(value))
    case Hint.DefaultValue(value)     => List(new DefaultTrait(value))
    case Hint.UniqueItems             => List(new UniqueItemsTrait())
    case Hint.Nullable                => List(new NullableTrait())
    case Hint.JsonName(value)         => List(new JsonNameTrait(value))
    case Hint.IsMixin                 => List(MixinTrait.builder.build)
    case Hint.ExternalDocs(desc, url) =>
      List(
        ExternalDocumentationTrait
          .builder()
          .addUrl(desc.getOrElse("documentation"), url)
          .build()
      )
    case Hint.Examples(examples) =>
      val trt = DataExamplesTrait.builder
      examples.foreach(ex =>
        trt.addExample(
          new DataExamplesTrait.DataExample(
            DataExamplesTrait.DataExampleType.JSON,
            ex
          )
        )
      )
      List(trt.build)
    case Hint.Auth(schemes) =>
      val shapeIds = schemes.map {
        case _: SecurityScheme.ApiKey     => HttpApiKeyAuthTrait.ID
        case SecurityScheme.BasicAuth     => HttpBasicAuthTrait.ID
        case _: SecurityScheme.BearerAuth => HttpBearerAuthTrait.ID
      }
      if (shapeIds.nonEmpty) List(new AuthTrait(shapeIds.toSet.asJava))
      else List(new AuthTrait(Set.empty[ShapeId].asJava))
    case Hint.Security(schemes) =>
      schemes.flatMap {
        case SecurityScheme.ApiKey(location, name) =>
          val in = location match {
            case ApiKeyLocation.Header => HttpApiKeyAuthTrait.Location.HEADER
            case ApiKeyLocation.Query  => HttpApiKeyAuthTrait.Location.QUERY
          }
          List(HttpApiKeyAuthTrait.builder().name(name).in(in).build())
        case SecurityScheme.BasicAuth => Some(new HttpBasicAuthTrait)
        case SecurityScheme.BearerAuth(bearerFormat) =>
          val format = bearerFormat
            .map(bf => new DocumentationTrait(s"Bearer Format: $bf"))
            .toList
          List(new HttpBearerAuthTrait()) ++ format
      }
    case Hint.Length(min, max) =>
      val b = LengthTrait.builder()
      min.foreach(b.min(_))
      max.foreach(b.max(_))
      List(b.build())
    case Hint.Range(min, max) =>
      val b = RangeTrait.builder()
      min.foreach(m => b.min(m.bigDecimal))
      max.foreach(m => b.max(m.bigDecimal))
      List(b.build())
    case Hint.Error(code, isServerError) =>
      List(
        new ErrorTrait(if (isServerError) "server" else "client"),
        new HttpErrorTrait(code)
      )
    case Hint.Http(method, path, maybeStatusCode) =>
      val builder = HttpTrait
        .builder()
        .method(method.toString)
        .uri(UriPattern.parse(sanitizedUriPath(path)))
      maybeStatusCode.foreach(builder.code(_))
      List(
        builder.build()
      )
    case Hint.ErrorMessage(message) =>
      List(new ErrorMessageTrait(message))

    case Hint.OpenApiExtension(map) =>
      val nodeMap = map.map { case (key, value) =>
        Node.from(key) -> value
      }.asJava
      val objNode = Node.objectNode(nodeMap)
      List(new OpenApiExtensionsTrait(objNode))
    case Hint.Timestamp(format) =>
      format match {
        case DateTime       => List(new TimestampFormatTrait("date-time"))
        case SimpleDate     => List(new alloy.DateFormatTrait())
        case LocalDate      => List(new alloy.DateFormatTrait())
        case LocalTime      => List(new alloy.LocalTimeFormatTrait())
        case LocalDateTime  => List(new alloy.LocalDateTimeFormatTrait())
        case OffsetDateTime => List(new alloy.OffsetDateTimeFormatTrait())
        case OffsetTime     => List(new alloy.OffsetTimeFormatTrait())
        case ZoneId         => List(new alloy.ZoneIdFormatTrait())
        case ZoneOffset     => List(new alloy.ZoneOffsetFormatTrait())
        case ZonedDateTime  => List(new alloy.ZonedDateTimeFormatTrait())
        case Year           => List(new alloy.YearFormatTrait())
        case YearMonth      => List(new alloy.YearMonthFormatTrait())
        case MonthDay       => List(new alloy.MonthDayFormatTrait())
        case Duration       => List(new alloy.DurationSecondsFormatTrait())
      }
    case Hint.Tags(values) =>
      List(TagsTrait.builder.values(values.asJava).build())
    case Hint.Const(value) =>
      List(ConstTrait.builder.value(value).build())
    case _ => List.empty
  }

}
