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

import cats.data.NonEmptyList
import java.nio.file.Path
import scala.jdk.CollectionConverters._
import scala.util.control.NoStackTrace
import smithyproto.proto3.{Compiler => ProtoCompiler, ModelPreProcessor}
import smithytranslate.openapi.OpenApiCompiler
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.SmithyIdlModelSerializer
import smithytranslate.json_schema.JsonSchemaCompiler

object Validator {

  sealed trait ValidationError extends NoStackTrace
  object ValidationError {
    final case class ProtoConversionError(
        expected: List[String],
        found: List[String]
    ) extends ValidationError {
      override def getMessage(): String = {
        s"""|
            |EXPECTED:
            |${expected.mkString("\n")}
            |
            |FOUND:
            |${found.mkString("\n")}
            |""".stripMargin
      }
    }
    final case class OpenapiConversionError(
        expected: Model,
        found: Model,
        errors: List[Throwable]
    ) extends ValidationError {
      override def getMessage(): String = {
        s"""|
            |EXPECTED:
            |${serialize(expected)}
            |
            |FOUND:
            |${serialize(found)}
            |""".stripMargin
      }
      private def serialize(model: Model): String =
        SmithyIdlModelSerializer
          .builder()
          .build()
          .serialize(model)
          .asScala(Path.of("foo.smithy"))
          .mkString
    }
    case class UnableToProduceOutput(raw: String) extends ValidationError {
      override def getMessage(): String = {
        s"""|
            |FAILED TO CONVERT:
            |$raw
            |""".stripMargin
      }
    }
  }

  private def validateOpenapi(
      openapi: String,
      smithy: String
  ): List[ValidationError] = {
    val namespace = "foo"
    val actualSmithy = s"""|$$version: "2"
                           |
                           |namespace $namespace
                           |
                           |$smithy""".stripMargin
    val options =
      OpenApiCompiler.Options(
        useVerboseNames = false,
        failOnValidationErrors = true,
        List.empty,
        useEnumTraitSyntax = false
      )
    val result =
      OpenApiCompiler.parseAndCompile(
        options,
        (NonEmptyList.of(namespace), openapi)
      )
    result match {
      case OpenApiCompiler.Failure(err, _) =>
        List(
          ValidationError.UnableToProduceOutput(err.getMessage)
        )
      case OpenApiCompiler.Success(errors, expectedModel) =>
        val actualModel = Model
          .assembler()
          .discoverModels()
          .addUnparsedModel(s"$namespace.smithy", actualSmithy)
          .assemble()
          .unwrap()
        if (expectedModel != actualModel)
          List(
            ValidationError.OpenapiConversionError(
              expectedModel,
              actualModel,
              errors
            )
          )
        else Nil
    }
  }

  private def validateJsonSchema(
      json: String,
      smithy: String
  ): List[ValidationError] = {
    val namespace = "foo"
    val actualSmithy = s"namespace $namespace\n" + smithy
    val options =
      OpenApiCompiler.Options(
        useVerboseNames = false,
        failOnValidationErrors = true,
        List.empty,
        useEnumTraitSyntax = false
      )
    val result =
      JsonSchemaCompiler.parseAndCompile(
        options,
        (NonEmptyList.of(namespace), json)
      )
    result match {
      case OpenApiCompiler.Failure(err, _) =>
        List(
          ValidationError.UnableToProduceOutput(err.getMessage)
        )
      case OpenApiCompiler.Success(errors, expectedModel) =>
        val actualModel = Model
          .assembler()
          .discoverModels()
          .addUnparsedModel(s"$namespace.smithy", actualSmithy)
          .assemble()
          .unwrap()
        if (expectedModel != actualModel)
          List(
            ValidationError.OpenapiConversionError(
              expectedModel,
              actualModel,
              errors
            )
          )
        else Nil
    }
  }

  private def validateProto(
      smithy: String,
      protoInputs: NonEmptyList[String]
  ): List[ValidationError] = {
    val namespace = "foo"
    val actualSmithy = s"""|$$version: "2"
                           |
                           |namespace $namespace
                           |
                           |$smithy""".stripMargin
    val getActualProto: String => String =
      proto => s"""|syntax = "proto3";
                   |
                   |package $namespace;
                   |$proto""".stripMargin
    val ActualProto = protoInputs.map(getActualProto).toList.sorted
    val compiler = new ProtoCompiler()
    val inputModel = Model
      .assembler()
      .discoverModels()
      .addUnparsedModel(s"$namespace.smithy", actualSmithy)
      .assemble()
      .unwrap()

    val result = compiler.compile(
      ModelPreProcessor(
        inputModel,
        List(ModelPreProcessor.transformers.PreventEnumConflicts)
      )
    )
    val rendered = result
      .filter(_.path.contains(namespace))
      .map(r => smithyproto.proto3.Renderer.render(r.unit))
      .sorted
    rendered match {
      case Nil => List(ValidationError.UnableToProduceOutput(actualSmithy))
      case ActualProto => Nil
      case other =>
        List(ValidationError.ProtoConversionError(other, ActualProto))
    }
  }

  def validate(
      result: ReadmeParser.ParserResult
  ): List[ValidationError] = {
    result.examples.flatMap {
      case ReadmeParser.Example.OpenApi(openapi, smithy) =>
        validateOpenapi(openapi, smithy)
      case ReadmeParser.Example.Proto(smithy, proto) =>
        validateProto(smithy, proto)
      case ReadmeParser.Example.JsonSchema(json, smithy) =>
        validateJsonSchema(json, smithy)
    }
  }
}
