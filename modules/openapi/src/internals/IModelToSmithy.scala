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

package smithytranslate.openapi.internals

import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.{EnumShape, _}

import scala.jdk.CollectionConverters._
import alloy.UntaggedUnionTrait
import alloy.DiscriminatedUnionTrait
import alloy.UuidFormatTrait
import software.amazon.smithy.model.traits.HttpTrait
import software.amazon.smithy.model.pattern.UriPattern
import software.amazon.smithy.model.traits._
import smithytranslate.OpenApiExtensionsTrait
import software.amazon.smithy.model.node.Node
import smithytranslate.{ErrorMessageTrait, DateOnlyTrait}
import smithytranslate.openapi.internals.TimestampFormat.DateTime
import smithytranslate.openapi.internals.TimestampFormat.SimpleDate
import smithytranslate.ContentTypeTrait
import smithytranslate.ContentTypeDiscriminatedTrait
import smithytranslate.openapi.internals.SecurityScheme
import smithytranslate.openapi.internals.ApiKeyLocation
import smithytranslate.DefaultValueTrait
import smithytranslate.NullableTrait
import cats.syntax.all._
import smithytranslate.NullFormatTrait
import smithytranslate.openapi.internals.Hint.Header
import smithytranslate.openapi.internals.Hint.QueryParam

final class IModelToSmithy(useEnumTraitSyntax: Boolean)
    extends (IModel => Model) {

  def apply(iModel: IModel): Model = {
    val shapes = iModel.definitions.map[Shape] {
      case Structure(id, fields, _, structHints) =>
        val members = fields.map { case Field(id, tpe, hints) =>
          val memName = id.memberName.value.toString
          val nameWillNeedChange =
            memName.headOption.exists(_.isDigit) || memName.contains("-")
          def isHeaderOrQuery = hints.exists {
            case Header(_)     => true
            case QueryParam(_) => true
            case _             => false
          }
          val jsonNameHint =
            if (nameWillNeedChange && !isHeaderOrQuery)
              List(Hint.JsonName(memName))
            else List.empty
          MemberShape
            .builder()
            .id(id.toSmithy)
            .target(tpe.toSmithy)
            .addHints(hints ++ jsonNameHint)
            .build()
        }
        val builder = StructureShape
          .builder()
          .id(id.toSmithy)
          .addHints(structHints)
        members.foreach(builder.addMember(_))
        builder.build()
      case MapDef(id, key, value, hints) =>
        MapShape
          .builder()
          .id(id.toSmithy)
          .key(key.toSmithy)
          .value(value.toSmithy)
          .addHints(hints)
          .build()
      case ListDef(id, member, hints) =>
        ListShape
          .builder()
          .id(id.toSmithy)
          .member(member.toSmithy)
          .addHints(hints)
          .build()
      case SetDef(id, member, hints) =>
        ListShape
          .builder()
          .id(id.toSmithy)
          .member(member.toSmithy)
          .addHints(hints)
          .addTrait(new UniqueItemsTrait())
          .build()
      case Union(id, altNames, unionKind, hints) =>
        val builder =
          UnionShape.builder().id(id.toSmithy).addHints(hints)
        altNames.foreach { alt =>
          val member = MemberShape
            .builder()
            .id(alt.id.toSmithy)
            .target(alt.tpe.toSmithy)
            .addHints(alt.hints)
            .build()
          builder.addMember(member)
        }
        unionKind match {
          case UnionKind.Discriminated(d) =>
            builder.addTrait(new DiscriminatedUnionTrait(d))
          case UnionKind.Untagged => builder.addTrait(new UntaggedUnionTrait())
          case UnionKind.ContentTypeDiscriminated =>
            builder.addTrait(new ContentTypeDiscriminatedTrait())
          case UnionKind.Tagged => ()
        }
        builder.build()
      case Newtype(id, target, hints) =>
        val builder = target.name.segments.last.value.toString match {
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
          case other => sys.error(s"error processing $id, found $other")
        }
        builder.id(id.toSmithy)
        hintsToTraits(hints).foreach(builder.addTrait(_))
        builder.build()
      case OperationDef(id, input, output, errors, hints) =>
        val builder =
          OperationShape.builder().id(id.toSmithy)
        output.foreach(o => builder.output(o.toSmithy))
        input.foreach(i => builder.input(i.toSmithy))
        hintsToTraits(hints).foreach(builder.addTrait(_))
        errors.foreach(e => builder.addError(e.toSmithy))
        builder.build()
      case ServiceDef(id, operations, hints) =>
        val builder = ServiceShape
          .builder()
          .id(id.toSmithy)
          .addHints(hints)
        operations.foreach(o => builder.addOperation(o.toSmithy))
        builder.build()
      case e: Enumeration => buildEnum(e)
      case other =>
        throw new IllegalArgumentException(s"Unexpected input: $other")
    }

    val builder = Model.builder()
    if (iModel.suppressions.nonEmpty) {
      builder.putMetadataProperty("suppressions", iModel.suppressions.toNode)
    }
    builder.addShapes(shapes.asJava)
    builder.build()
  }

  private def buildEnum(e: Enumeration): Shape = {
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
      values.foreach(v =>
        enumBuilder.addMember(
          MemberShape
            .builder()
            .id(s"${id.toSmithy}$$$v")
            .target(UnitTypeTrait.UNIT)
            .build()
        )
      )
      enumBuilder.addHints(hints).build()
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
          sanitizeForDigitStart(memberName.value.toString).replaceAll("-", "_")
        )
    }
  }

  // if name starts with a number, prefix with n
  private def sanitizeForDigitStart(id: String): String =
    if (id.headOption.exists(_.isDigit)) s"n$id"
    else id

  private def sanitizeNamespace(id: String): String = {
    sanitizeForDigitStart(id).replaceAll("-", "_")
  }

  private def sanitizeName(id: String): String = {
    StringUtil.toCamelCase(sanitizeForDigitStart(id))
  }

  private def sanitizeMemberName(id: String): String = {
    sanitizeForDigitStart(id).replaceAll("-", "_")
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

  private def hintsToTraits(hints: List[Hint]) = hints.flatMap[Trait] {
    case Hint.Description(value)      => List(new DocumentationTrait(value))
    case Hint.Body                    => List(new HttpPayloadTrait())
    case Hint.Header(name)            => List(new HttpHeaderTrait(name))
    case Hint.PathParam(_)            => List(new HttpLabelTrait())
    case Hint.QueryParam(name)        => List(new HttpQueryTrait(name))
    case Hint.Required                => List(new RequiredTrait())
    case Hint.Pattern(value)          => List(new PatternTrait(value))
    case Hint.Sensitive               => List(new SensitiveTrait())
    case Hint.ContentTypeLabel(value) => List(new ContentTypeTrait(value))
    case Hint.DefaultValue(value)     => List(new DefaultValueTrait(value))
    case Hint.UniqueItems             => List(new UniqueItemsTrait())
    case Hint.Nullable                => List(new NullableTrait())
    case Hint.JsonName(value)         => List(new JsonNameTrait(value))
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
        case DateTime   => List(new TimestampFormatTrait("date-time"))
        case SimpleDate => List(new DateOnlyTrait())
      }
    case _ => List.empty
  }

  implicit class ShapeBuilderOps[A <: AbstractShapeBuilder[A, S], S <: Shape](
      builder: AbstractShapeBuilder[A, S]
  ) {
    def addHints(hints: List[Hint]): A = {
      hintsToTraits(hints).foreach(builder.addTrait)
      builder.asInstanceOf[A]
    }
  }

}
