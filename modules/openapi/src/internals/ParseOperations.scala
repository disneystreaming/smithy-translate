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

package smithytranslate.openapi
package internals

import cats.data.{Chain, NonEmptyChain}
import cats.syntax.all._
import io.swagger.v3.oas.models
import io.swagger.v3.oas.models.OpenAPI
import org.typelevel.ci._
import scala.jdk.CollectionConverters._
import smithytranslate.openapi.internals.Hint.Header

object ParseOperations
    extends (
        (
            SecuritySchemes,
            OpenAPI,
            CaseRefBuilder
        ) => ParseOperationsResult
    ) {

  def apply(
      securitySchemes: SecuritySchemes,
      openApi: OpenAPI,
      caseRef: CaseRefBuilder
  ): ParseOperationsResult = {
    new ParseOperationsImpl(securitySchemes, openApi, caseRef).run()
  }

}

private class ParseOperationsImpl(
    securitySchemes: SecuritySchemes,
    openApi: OpenAPI,
    caseRef: CaseRefBuilder
) {

  private case class IOpInfo(
      commonParams: Vector[Param],
      method: HttpMethod,
      path: String,
      op: models.Operation
  )

  private object IOpInfo {
    def maybe(
        commonParams: Vector[Param],
        method: HttpMethod,
        path: String,
        op: => models.Operation
    ): Option[IOpInfo] =
      Option(op).map(IOpInfo(commonParams, method, path, _))
  }

  def run(): ParseOperationsResult = {
    val paths = (for {
      p <- Option(openApi.getPaths)
    } yield p.asScala.toVector).getOrElse(Vector.empty)

    val opsAndErrors = paths
      .map { case (path, item) =>
        val allCommonParams: Vector[Either[ModelError, Param]] =
          Option(item.getParameters()).toVector
            .flatMap(_.asScala.toVector)
            .map(getParam)
        val (paramErrors, commonParams) = allCommonParams.partitionMap(identity)
        (
          paramErrors,
          Vector(
            // format: off
            IOpInfo.maybe(commonParams, HttpMethod.GET, path, item.getGet()),
            IOpInfo.maybe(commonParams, HttpMethod.POST, path, item.getPost()),
            IOpInfo.maybe(commonParams, HttpMethod.PUT, path, item.getPut()),
            IOpInfo.maybe(commonParams, HttpMethod.PATCH, path, item.getPatch()),
            IOpInfo.maybe(commonParams, HttpMethod.DELETE, path, item.getDelete()),
            IOpInfo.maybe(commonParams, HttpMethod.HEAD, path, item.getHead()),
            IOpInfo.maybe(commonParams, HttpMethod.OPTIONS, path, item.getOptions()),
            IOpInfo.maybe(commonParams, HttpMethod.TRACE, path, item.getTrace())
            //format: on
          ).flatten
        )
      }

    val allOperations = opsAndErrors.collect { _._2 }.flatten
    val allParamErrrs = opsAndErrors.collect { _._1 }.flatten

    val hasGlobalSecurity =
      Option(openApi.getSecurity().asScala).exists(_.nonEmpty)
    allOperations
      .map(gatherInformation(hasGlobalSecurity))
      .combineAll
      .addErrors(allParamErrrs)
  }

  private def getOpId(opInfo: IOpInfo): Name = {
    val segments = Option(opInfo.op.getOperationId())
      .map(s => Segment.Derived(CIString(s)))
      .map(NonEmptyChain.of(_))
      .getOrElse(
        NonEmptyChain
          .fromChainUnsafe(
            Chain.fromSeq(
              opInfo.path
                .split('/')
                .filter(_.nonEmpty)
                .map(s =>
                  Segment
                    .Derived(CIString(s.filterNot(c => c == '{' || c == '}')))
                )
                .toList :+ Segment.Arbitrary(CIString(opInfo.method.toString))
            )
          )
      )
    Name(segments)
  }

  // https://awslabs.github.io/smithy/1.0/spec/core/http-traits.html#restricted-http-headers
  private val restrictedHeaders = Set(
    ci"Authorization",
    ci"Connection",
    ci"Content-Length",
    ci"Expect",
    ci"Host",
    ci"Max-Forwards",
    ci"Proxy-Authenticate",
    ci"Server",
    ci"TE",
    ci"Trailer",
    ci"Transfer-Encoding",
    ci"Upgrade",
    ci"User-Agent",
    ci"WWW-Authenticate",
    ci"X-Forwarded-For"
  )
  private val httpHeaderSuppression = SuppressionFor(
    "HttpHeaderTrait",
    "Restricted headers are in use. See https://awslabs.github.io/smithy/1.0/spec/core/http-traits.html#restricted-http-headers."
  )
  private def getHeaderSuppressions(
      allParams: Vector[Param]
  ): Option[SuppressionFor] = {
    val usesRestrictedHeaders =
      allParams
        .flatMap(_.hints)
        .exists {
          case Header(name) =>
            restrictedHeaders(CIString(name))
          case _ => false
        }
    Option.when(usesRestrictedHeaders)(httpHeaderSuppression)
  }

  private def gatherInformation(
      hasGlobalSecurity: Boolean
  )(opInfo: IOpInfo): ParseOperationsResult = {
    import opInfo._
    val localParams: Vector[Either[ModelError, Param]] = getLocalInputParams(op)
    val body: Vector[Either[ModelError, Param]] =
      getRequestBodyParam(op).toVector.map(Right(_))
    val allInputParams: Vector[Either[ModelError, Param]] =
      opInfo.commonParams.map(Right(_)) ++ localParams ++ body
    val allValidInputParams = allInputParams.collect { case Right(p) => p }
    val allInputParamsErrors = allInputParams.collect { case Left(e) => e }
    val opName = getOpId(opInfo)
    val inputName = opName :+ Segment.Arbitrary(ci"Input")
    val inputInfo = HttpMessageInfo(inputName, allValidInputParams, List.empty)
    val outputs = getOutputs(opInfo)
    val (errors, securityHint) =
      getSecurityHint(securitySchemes, opInfo, hasGlobalSecurity)
    val descHint = Option(op.getDescription()).map(Hint.Description).toList
    val exDocs = Option(opInfo.op.getExternalDocs()).map(e =>
      Hint.ExternalDocs(Option(e.getDescription()), e.getUrl())
    )
    val hints =
      GetExtensions.from(opInfo.op) ++ securityHint ++ descHint ++ exDocs
    val suppressions = getHeaderSuppressions(allValidInputParams).toVector
    ParseOperationsResult(
      errors ++ allInputParamsErrors,
      Vector(
        OperationInfo(
          opName,
          method,
          path,
          inputInfo,
          outputs,
          hints
        )
      ),
      suppressions
    )
  }

  private def getSecurityHint(
      securitySchemes: SecuritySchemes,
      opInfo: IOpInfo,
      hasGlobalSecurity: Boolean
  ): (List[ModelError], Option[Hint.Auth]) = {
    import opInfo._
    val id = getOpId(opInfo)
    val security =
      Option(op.getSecurity())
        .map(_.asScala.toList)
        .getOrElse(List.empty)
    val (allSecurityKeys, usedSecurityKeys) = security
      .map { s =>
        val keys = s.keySet().asScala.toList
        (keys, keys.headOption)
      }
      .unzip
      .map(_.flatten)
    val schemes = usedSecurityKeys.flatMap(securitySchemes.get).toVector
    val needToIgnoreServiceLevelSchemes =
      securitySchemes.nonEmpty && !hasGlobalSecurity // signals the need to put empty auth trait in place
    val maybeHint =
      if (schemes.nonEmpty || needToIgnoreServiceLevelSchemes)
        Some(Hint.Auth(schemes))
      else None
    val errors = allSecurityKeys.collect {
      case s if s.size > 1 =>
        ModelError.Restriction(
          s"Operation ${id.segments.mkString_(".")} contains an unsupported security requirement: `$s`. " +
            s"Security schemes cannot be ANDed together. ${s.head} will be used and ${s.tail} will be ignored."
        )
    }
    (errors, maybeHint)
  }

  private def getRequestBodyParam(
      op: models.Operation
  ): Option[Param] = {
    Option(
      op.getRequestBody()
    ).flatMap { rb =>
      val exts = GetExtensions.from(rb)
      val bodies = ContentToSchemaOpt(rb.getContent())
      val (bodyHints, maybeSchema) =
        bodies.toList match {
          case Nil => (Nil, Option(rb.get$ref()).map(RefParser.toSchema))
          case (contentType, body) :: Nil =>
            (List(Hint.ContentTypeLabel(contentType)), Some(body))
          case list => (Nil, Some(ContentTypeDiscriminatedSchema(list.toMap)))
        }
      val required = rb.getRequired()
      val requiredHint = if (required) List(Hint.Required) else List.empty
      val descHint =
        Option(rb.getDescription())
          .map(d => List(Hint.Description(d)))
          .getOrElse(List.empty)
      val hints =
        List(Hint.Body) ++ requiredHint ++ descHint ++ exts ++ bodyHints
      maybeSchema.map { schema =>
        Param("body", Right(schema), hints)
      }
    }
  }

  private def getParam(
      parameter: models.parameters.Parameter
  ): Either[ModelError, Param] = {
    val maybeResolvedParam: Either[ModelError, models.parameters.Parameter] =
      Option(parameter.get$ref())
        .toLeft(parameter)
        .leftFlatMap(ref =>
          caseRef(ref)
            .map[models.parameters.Parameter] { defId =>
              Option(openApi.getComponents())
                .flatMap(c => Option(c.getParameters()))
                .flatMap(map =>
                  Option(map.get(defId.name.segments.last.value.toString))
                )
                .getOrElse(parameter)
            }
        )

    maybeResolvedParam.flatMap { resolvedParam =>
      val name = resolvedParam.getName()
      val exts = GetExtensions.from(resolvedParam)
      val httpBinding = resolvedParam.getIn() match {
        case "query"  => Some(Hint.QueryParam(name))
        case "path"   => Some(Hint.PathParam(name))
        case "header" => Some(Hint.Header(name))
        case _        => None
      }
      val description =
        Option(resolvedParam.getDescription())
          .map[Hint](Hint.Description)
          .toList
      httpBinding
        .map { b =>
          val required =
            if (resolvedParam.getRequired()) List(Hint.Required) else Nil
          val hints = b :: description ++ required ++ exts
          val refOrSchema = Option(resolvedParam.get$ref()) match {
            case Some(ref) => Left(ref)
            case None      => Right(resolvedParam.getSchema())
          }
          Param(name, refOrSchema, hints)
        }
        .toRight(
          ModelError.ProcessingError(
            s"Parameter ${name} should have in defined as `query`, `path` or `header."
          )
        )
    }
  }

  private def getLocalInputParams(
      op: models.Operation
  ): Vector[Either[ModelError, Param]] = {
    Option(op.getParameters()).toVector
      .flatMap(
        _.asScala
          .map(getParam)
          .toVector
      )
  }

  private def getOutputs(
      opInfo: IOpInfo
  ): Vector[Output] = {
    import opInfo._
    val opName = getOpId(opInfo)
    op
      .getResponses()
      .asScala
      .map { case (maybeCode, response) =>
        val code = maybeCode.toIntOption.getOrElse(400)
        val outputName = opName :+ Segment.Arbitrary(CIString(code.toString()))
        val codeHints = code match {
          case c if c < 200 || c >= 500 =>
            List(Hint.Error(code, isServerError = true))
          case c if c >= 400 && c < 500 =>
            List(Hint.Error(code, isServerError = false))
          case _ => List.empty
        }
        val refOrMessage = ApiResponseToParams(outputName, response)
          .map(_.mapHints(_ ++ codeHints))
        Output(code, refOrMessage, codeHints)
      }
      .toVector
  }

}
