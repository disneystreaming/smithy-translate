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

package smithytranslate.compiler
package internals
package openapi

import cats.Parallel
import cats.mtl.Tell
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media._

import scala.jdk.CollectionConverters._
import cats.data._
import Primitive._
import ToSmithyError._
import cats.syntax.all._
import cats.Monad
import org.typelevel.ci._
import GetExtensions.HasExtensions
import cats.catsParallelForId

private[compiler] object OpenApiToIModel {

  def compile(
      namespace: Path,
      openAPI: Either[List[String], OpenAPI]
  ): (Chain[ToSmithyError], IModel) = {
    type ErrorLayer[A] = Writer[Chain[ToSmithyError], A]
    type WriterLayer[A] =
      WriterT[ErrorLayer, Chain[Either[Suppression, Definition]], A]

    val (errors, (data, _)) = compileF[WriterLayer](namespace, openAPI).run.run
    val definitions = data.collect { case Right(d) => d }
    val suppressions = data.collect { case Left(s) => s }
    (errors, IModel(definitions.toVector, suppressions.toVector))
  }

  def compileF[F[_]: Parallel: TellShape: TellError](
      namespace: Path,
      maybeOpenApi: Either[List[String], OpenAPI]
  ): F[Unit] = {
    maybeOpenApi match {
      case Right(openApi) =>
        val parser = new OpenApiToIModel[F](namespace, openApi)
        parser.recordAll
      case Left(errors) =>
        Tell.tell(
          Chain.one((ToSmithyError.OpenApiParseError(namespace, errors)))
        )
    }
  }
}

private[openapi] class OpenApiToIModel[F[_]: Parallel: TellShape: TellError](
    namespace: Path,
    openApi: OpenAPI
) {

  implicit val F: Monad[F] = Parallel[F].monad

  private val CaseRef = new CaseRefBuilder(namespace) {}

  private val (securityErrors, securitySchemes) =
    ParseSecuritySchemes(openApi)

  private val operations: ParseOperationsResult =
    ParseOperations(securitySchemes, openApi, CaseRef)

  private val componentSchemas: Vector[Local] = (for {
    components <- Option(openApi.getComponents)
    schemas <- Option(components.getSchemas)
  } yield schemas.asScala.toVector.map { case (name, schema) =>
    val segments = Name(
      Segment.Arbitrary(ci"components"),
      Segment.Arbitrary(ci"schemas"),
      Segment.Derived(CIString(name))
    )
    Local(segments, schema).addHints(List(Hint.TopLevel))
  }).getOrElse(Vector.empty)

  private val recordResponses: F[Unit] = {
    val allResponses = for {
      components <- Option(openApi.getComponents).toIterable
      responses <- Option(components.getResponses).toIterable
      refsAndMessages <- responses.asScala.map { case (name, response) =>
        val segments = Name(
          Segment.Arbitrary(ci"components"),
          Segment.Arbitrary(ci"responses"),
          Segment.Derived(CIString(name))
        )
        val opHints =
          operations.results
            .flatMap(r => r.outputs)
            .collect {
              case Output(_, Left(ref), hints) if ref === segments.asRef =>
                hints
            }
            .flatten
        ApiResponseToParams(segments, response)
          .map(_.mapHints(_ ++ opHints))
      }
    } yield refsAndMessages
    allResponses.toVector.traverse_(recordRefOrMessage(_, None))
  }

  private val componentRequestBodySchemas: Vector[Local] = (for {
    components <- Option(openApi.getComponents)
    requests <- Option(components.getRequestBodies())
    schemas = requests.asScala
      .flatMap { case (name, requestBody) =>
        val maybeRequired =
          if (requestBody.getRequired()) List(Hint.Required) else Nil
        val bodies = ContentToSchemaOpt(requestBody.getContent)
        val segments = Name(
          Segment.Arbitrary(ci"components"),
          Segment.Arbitrary(ci"requestBodies"),
          Segment.Derived(CIString(name))
        )
        (bodies.toList match {
          case Nil => None
          case (contentType, sch) :: Nil =>
            Some(
              Local(segments, sch).addHints(Hint.ContentTypeLabel(contentType))
            )
          case many =>
            Some(
              Local(segments, ContentTypeDiscriminatedSchema(many.toMap))
            )
        }).map(_.addHints(maybeRequired))
      }
  } yield schemas.toVector)
    .getOrElse(Vector.empty)

  private val headerSchemas: Vector[Local] = for {
    components <- Option(openApi.getComponents).toVector
    headers <- Option(components.getHeaders()).toVector
    headerParam <- HeadersToParams(headers.asScala)
  } yield {
    val segments = Name(
      Segment.Arbitrary(ci"components"),
      Segment.Arbitrary(ci"headers"),
      Segment.Derived(CIString(headerParam.name))
    )
    val context = Context(segments, headerParam.hints, Nil)
    headerParam.refOrSchema match {
      case Left(ref)    => Local(context, RefParser.toSchema(ref))
      case Right(value) => Local(context, value)
    }
  }

  private val allSchemas: Vector[Local] =
    componentSchemas ++ componentRequestBodySchemas ++ headerSchemas

  /** Refolds the schema, aggregating found definitions in Tell.
    */
  private val refoldSchemas: F[Unit] =
    allSchemas.parTraverse_(refoldOne)

  private val recordOperations: F[Unit] = {
    operations.results
      .parTraverse(recordOperation)
      .flatMap(recordService) *>
      operations.errors.parTraverse(recordError).void *>
      operations.suppressions
        .map(sFor => sFor(Namespace(namespace.toList)))
        .parTraverse(recordSuppression)
        .void
  }

  val recordAll =
    refoldSchemas *>
      recordOperations *>
      recordResponses *>
      recordSecurityErrors

  private def recordSecurityErrors: F[Unit] =
    securityErrors.traverse(recordError).void

  private def fold = new PatternFolder[F](namespace).fold _

  private def refoldOne(start: Local): F[DefId] = {
    // Refolding each top value under openapi's "component/schema"
    // into a type, recording definitions using Tell as we go, during
    // the collapse (fold).
    def unfoldAndAddExts(local: Local) =
      unfold(local).map(transformPattern(local))

    recursion.refoldPar(unfoldAndAddExts _, fold)(start)
  }

  private def transformPattern[A](
      local: Local
  ): OpenApiPattern[A] => OpenApiPattern[A] = {
    val maybeHints = GetExtensions.from(HasExtensions.unsafeFrom(local.schema))
    (pattern: OpenApiPattern[A]) =>
      pattern.mapContext(_.addHints(maybeHints, retainTopLevel = true))
  }

  private def recordService(opDefIds: Vector[DefId]): F[Unit] = {
    val security =
      Option(openApi.getSecurity())
        .map(_.asScala.toSet)
        .getOrElse(Set.empty)
        .flatMap(_.keySet().asScala)
    val auth = security.flatMap(securitySchemes.get).toVector
    val authHint = if (auth.nonEmpty) List(Hint.Auth(auth)) else Nil
    val schemes = securitySchemes.values.toVector
    val securityHint =
      if (schemes.nonEmpty) List(Hint.Security(schemes)) else Nil
    val exts = GetExtensions.from(HasExtensions.unsafeFrom(openApi))
    val infoExts =
      GetExtensions.from(HasExtensions.unsafeFrom(openApi.getInfo()))
    val externalDocs = Option(openApi.getExternalDocs()).map(e =>
      Hint.ExternalDocs(Option(e.getDescription()), e.getUrl())
    )
    val service = ServiceDef(
      DefId(
        Namespace(namespace.toList),
        Name.derived(
          StringUtil.toCamelCase(namespace.last.capitalize)
        ) ++ Name.arbitrary("Service")
      ),
      opDefIds,
      exts ++ infoExts ++ securityHint ++ authHint ++ externalDocs
    )
    if (opDefIds.nonEmpty) recordDef(service) else F.unit
  }

  private def recordOperation(
      opInfo: OperationInfo
  ): F[DefId] = {
    val opName = opInfo.name
    val ns = Namespace(namespace.toList)
    val opId = DefId(ns, opName)
    val recordInput: F[Option[DefId]] =
      recordHttpMessage(opInfo.input, None)
    val recordOutput: F[Option[(Int, DefId)]] = opInfo.outputs.filter {
      output =>
        val code = output.code
        code >= 200 && code < 300
    }.toList match {
      case output :: Nil =>
        val code = output.code
        recordRefOrMessage(output.refOrMessage, None)
          .map(
            _.map((code, _))
          )
      case output :: tail =>
        val code = output.code
        val recordFirst = recordRefOrMessage(output.refOrMessage, None)
          .map(
            _.map((code, _))
          )
        val recordRest = tail
          .traverse { i =>
            val errorMessage =
              s"Multiple success responses are not supported. Found status code ${i.code} when ${output.code} was already recorded"
            recordError(ToSmithyError.Restriction(errorMessage)) *>
              recordRefOrMessage(
                i.refOrMessage.map(m =>
                  m.copy(hints = m.hints :+ Hint.ErrorMessage(errorMessage))
                ),
                Some(errorNamespace)
              )
          }
        recordFirst <* recordRest
      case Nil => F.pure(None)
    }

    val recordErrors: F[Vector[DefId]] = opInfo.outputs
      .filter { output =>
        val code = output.code
        code < 200 || code >= 300
      }
      .flatTraverse { output =>
        recordRefOrMessage(output.refOrMessage, None).map(_.toVector)
      }

    for {
      maybeInput <- recordInput
      output <- recordOutput
      errors <- recordErrors
      (maybeSuccessCode, maybeOutputId) = output.unzip
      http = Hint.Http(opInfo.method, opInfo.path, maybeSuccessCode)
      opDef = OperationDef(
        opId,
        maybeInput,
        maybeOutputId,
        errors,
        http :: opInfo.hints
      )
      _ <- recordDef(opDef)
    } yield opId
  }

  def recordHttpMessage(
      message: HttpMessageInfo,
      maybeNamespace: Option[Namespace]
  ): F[Option[DefId]] = {
    val ns = maybeNamespace.getOrElse(Namespace(namespace.toList))
    val defId = DefId(ns, message.name)
    message.params
      .traverse { param =>
        val paramName = Segment.Derived(CIString(param.name))
        val name = message.name :+ paramName
        val computeId: F[DefId] = param.refOrSchema match {
          case Left(ref) =>
            CaseRef(ref) match {
              case Left(error) =>
                val id = errorId(Context(name, Nil, Nil))
                recordError(error).as(id)
              case Right(parsedRef) => F.pure(parsedRef.id)
            }
          case Right(schema) => refoldOne(Local(name, schema))
        }
        computeId.map { tpe =>
          Field(MemberId(defId, paramName), tpe, param.hints)
        }
      }
      .map { fields =>
        Structure(defId, fields, Vector.empty, message.hints).some
      }
      .flatMap(_.traverse(recordDef).map(_.as(defId)))
  }

  def recordRefOrMessage(
      refOrMessage: RefOr[HttpMessageInfo],
      maybeNamespace: Option[Namespace]
  ): F[Option[DefId]] =
    refOrMessage match {
      case Left(ref) =>
        CaseRef(ref) match {
          case Right(parsedRef) =>
            F.pure(Some(parsedRef.id))
          case Left(error) => recordError(error).as(None)
        }
      case Right(message) => recordHttpMessage(message, maybeNamespace)
    }

  private def getCollectionLengthConstraint(
      sch: Schema[_]
  ): Option[Hint.Length] = {
    val minL = Option(sch.getMinLength())
      .orElse(Option(sch.getMinItems()))
      .map(_.toLong)
    val maxL = Option(sch.getMaxLength())
      .orElse(Option(sch.getMaxItems()))
      .map(_.toLong)
    if (minL.isDefined || maxL.isDefined) Some(Hint.Length(minL, maxL))
    else None
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Unfold
  // ///////////////////////////////////////////////////////////////////////////

  private implicit class WithDescriptionSyntax(p: OpenApiPattern[Local]) {
    def withDescription(local: Local): OpenApiPattern[Local] = {
      val maybeDesc =
        Option(local.schema.getDescription()).map(Hint.Description(_)).toList
      p.mapContext(_.addHints(maybeDesc, retainTopLevel = true))
    }
  }

  private def getRangeTrait(
      local: Local,
      prim: Primitive
  ): (Option[Hint], Option[ToSmithyError]) = {
    val minR = Option(local.schema.getMinimum()).map(BigDecimal(_))
    val maxR = Option(local.schema.getMaximum()).map(BigDecimal(_))
    val range =
      if (minR.isDefined || maxR.isDefined) Some(Hint.Range(minR, maxR))
      else None

    val exclusiveMinModifier =
      Option(local.schema.getExclusiveMinimum())
        .map(m => if (m) 1L else 0L)
        .getOrElse(0L)

    val exclusiveMaxModifier =
      Option(local.schema.getExclusiveMinimum())
        .map(m => if (m) -1L else 0L)
        .getOrElse(0L)

    val (updatedRange, error) = range.map { r =>
      val isWholeNumberType = prim match {
        case PLong | PInt => true
        case _            => false
      }
      if (isWholeNumberType) {
        val newR = r.copy(
          min = r.min.map(_ + exclusiveMinModifier),
          max = r.max.map(_ + exclusiveMaxModifier)
        )
        (newR, None)
      } else {
        val primType = prim.toString.drop("P".length())
        val errorMessage =
          s"Unable to automatically account for exclusiveMin/Max on decimal type $primType"
        (r, Some(ToSmithyError.Restriction(errorMessage)))
      }
    }.unzip
    (updatedRange, error.flatten)
  }

  private def getExamplesHint(schema: Schema[_]): Option[Hint.Examples] =
    Option(schema.getExample())
      .map(GetExtensions.anyToNode)
      .map(n => Hint.Examples(List(n)))

  private def getExternalDocsHint(
      schema: Schema[_]
  ): Option[Hint.ExternalDocs] =
    Option(schema.getExternalDocs())
      .map(e => Hint.ExternalDocs(Option(e.getDescription()), e.getUrl()))

  /*
   * The goal here is to match one layer of "Schema[_]" and
   * assign it to the corresponding pattern.
   *
   * We do not we try to deference anything either, as
   * that is the responsibility of a another piece of logic that checks
   * our model implementation complies with laws.
   *
   * Errors are raised when we enconter "Schema[_]" instances that do
   * not fit in our metamodel.
   */
  def unfold(local: Local): F[OpenApiPattern[Local]] = {
    local.schema match {
      // S:
      //   type: string
      //   enum: [a, b ,c]
      case CaseEnum(values) =>
        F.pure(
          OpenApiEnum(local.context.copy(hints = Nil), values)
            .withDescription(local)
        )

      // Primitive types
      case CasePrimitive(prim) =>
        val (hints, errors) = {
          val minL = Option(local.schema.getMinLength()).map(_.toLong)
          val maxL = Option(local.schema.getMaxLength()).map(_.toLong)
          val length =
            if (minL.isDefined || maxL.isDefined) Some(Hint.Length(minL, maxL))
            else None

          val pattern = Option(local.schema.getPattern).map(Hint.Pattern(_))
          val sensitive = Option(local.schema.getFormat()).flatMap(f =>
            if (f == "password") Some(Hint.Sensitive) else None
          )
          val topLevel = local.context.hints.filter(_ == Hint.TopLevel)
          val (range, rangeError) = getRangeTrait(local, prim)

          val examples = getExamplesHint(local.schema)
          val externalDocs = getExternalDocsHint(local.schema)
          val hints =
            List(
              length.toList,
              range.toList,
              pattern.toList,
              sensitive.toList,
              topLevel,
              examples.toList,
              externalDocs.toList
            ).flatten
          val errors = List(rangeError).flatten
          (hints, errors)
        }
        F.pure(
          OpenApiPrimitive(
            local.context.copy(hints = hints).addErrors(errors),
            prim
          )
            .withDescription(local)
        )

      // $ref: <uri>
      case CaseRef(idOrError) =>
        idOrError match {
          case Left(error) => F.pure(OpenApiShortStop(local.context, error))
          case Right(parsedRef)   => F.pure(OpenApiRef(local.context, parsedRef.id))
        }

      // A:
      //   type: set
      //   items:
      //     <item type>
      case s: ArraySchema if s.getUniqueItems =>
        val lengthConstraint = getCollectionLengthConstraint(local.schema)
        val hints = getExternalDocsHint(s).toList ++
          getExamplesHint(s).toList ++ lengthConstraint
        F.pure(
          OpenApiSet(
            local.context.addHints(hints),
            Local(
              local.context.append(Segment.Arbitrary(ci"Item")),
              s.getItems()
            )
          ).withDescription(local)
        )

      // A:
      //   type: array
      //   items:
      //     <item type>
      case s: ArraySchema =>
        val lengthConstraint = getCollectionLengthConstraint(local.schema)
        val hints = getExternalDocsHint(s).toList ++
          getExamplesHint(s).toList ++ lengthConstraint
        F.pure(
          OpenApiArray(
            local.context.addHints(hints),
            Local(
              local.context.append(Segment.Arbitrary(ci"Item")),
              s.getItems()
            )
          ).withDescription(local)
        )

      // O:
      //   type: object
      //   additionalProperties: schema
      case CaseMap(items) =>
        val lengthConstraint = getCollectionLengthConstraint(local.schema)
        val hints = getExternalDocsHint(local.schema).toList ++
          getExamplesHint(local.schema).toList ++ lengthConstraint
        F.pure(
          OpenApiMap(
            local.context.addHints(hints),
            Local(
              local.context.append(Segment.Arbitrary(ci"Item")),
              items
            )
          ).withDescription(local)
        )

      // O:
      //   type: object
      //   additionalProperties: true
      //
      // O:
      //   type: object
      case IsFreeForm() =>
        F.pure(
          OpenApiPrimitive(local.context, Primitive.PFreeForm).withDescription(
            local
          )
        )

      // O:
      //   type: object
      //   properties:
      //     <at least one property>
      case CaseObject(s) =>
        val properties =
          Option(s.getProperties()).map(_.asScala.toVector).toVector.flatten
        val required =
          Option(s.getRequired()).map(_.asScala).getOrElse(List.empty).toSet
        val hints = getExternalDocsHint(s).toList ++ getExamplesHint(s).toList
        val fields =
          properties.map { case (name, schema) =>
            (name, required(name)) -> Local(
              local.context.append(Segment.Derived(CIString(name))),
              schema
            )
          }
        F.pure(
          OpenApiObject(
            local.context.addHints(hints, retainTopLevel = true),
            fields
          )
            .withDescription(local)
        )

      // O:
      //   allOf:
      case CaseAllOf(all) =>
        val newPath = local.context.append(Segment.Arbitrary(ci"allOf"))
        F.pure(
          OpenApiAllOf(
            local.context,
            all.zipWithIndex.map { case (s, i) =>
              Local(newPath.append(Segment.Arbitrary(ci"$i")), s)
            }
          ).withDescription(local)
        )

      // O:
      //   oneOf:
      case CaseOneOf(alternatives) =>
        val maybeDiscriminator =
          Option(local.schema.getDiscriminator())
            .map(_.getPropertyName)
        val unionKind = maybeDiscriminator match {
          case Some(value) => UnionKind.Discriminated(value)
          case None        => UnionKind.Untagged
        }

        val newPath = local.context.append(Segment.Arbitrary(ci"oneOf"))
        F.pure(
          OpenApiOneOf(
            local.context,
            alternatives.zipWithIndex.map { case (s, i) =>
              (Nil, Local(newPath.append(Segment.Arbitrary(ci"alt$i")), s))
            },
            unionKind
          ).withDescription(local)
        )

      // O:
      //   anyOf:
      case s: ComposedSchema if Option(s.getAnyOf).isDefined =>
        val error = Restriction("anyOf is not allowed")
        F.pure(OpenApiShortStop(local.context, error))

      case ContentTypeDiscriminatedSchema(alternatives) =>
        F.pure(
          OpenApiOneOf(
            local.context,
            alternatives.toVector.map { case (name, sch) =>
              List(Hint.ContentTypeLabel(name)) -> Local(
                local.context
                  .append(
                    Segment.Derived(CIString(StringUtil.toCamelCase(name)))
                  ),
                sch
              )
            },
            UnionKind.ContentTypeDiscriminated
          )
        )

      case s =>
        val error = Restriction(s"Schema not supported:\n$s")
        F.pure(OpenApiShortStop(local.context, error))
    }
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Utils
  // ///////////////////////////////////////////////////////////////////////////

  private def recordError(e: ToSmithyError): F[Unit] =
    Tell.tell(Chain.one(e))

  private def recordDef(definition: Definition): F[Unit] =
    Tell.tell(Chain.one(Right(definition)))

  private def recordSuppression(suppression: Suppression): F[Unit] =
    Tell.tell(Chain.one(Left(suppression)))

  def errorId(context: Context): DefId = {
    DefId(errorNamespace, context.path)
  }

  private def errorNamespace: Namespace = Namespace(List("error"))

}
