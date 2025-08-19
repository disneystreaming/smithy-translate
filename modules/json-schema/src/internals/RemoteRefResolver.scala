package smithytranslate.compiler
package internals
package json_schema

import scala.jdk.CollectionConverters._
import cats.data.NonEmptyChain
import cats.Parallel
import scala.io.Source
import org.json.JSONObject
import cats.Monad
import io.circe.jawn
import cats.syntax.all._
import Extractors._
import io.circe.Json
import org.everit.json.schema.Schema
import javax.xml.validation
import cats.mtl._
import cats.mtl.syntax._
import cats.data.Chain
import smithytranslate.compiler.json_schema.CompilationUnit
import java.net.URI
import scala.util.Try
import smithytranslate.compiler.ToSmithyError.OpenApiParseError
import cats.data.WriterT


private[compiler] object RemoteRefResolver {
  
  sealed trait RemoteRefResolutionFailure extends Throwable {
    def message: String
    override def getMessage(): String = message
  }

  private case class IntermediateCompilationUnit(ns: NonEmptyChain[String], schema: Schema)
  private object IntermediateCompilationUnit {
    def apply(compUnit: CompilationUnit): IntermediateCompilationUnit =
      new IntermediateCompilationUnit(compUnit.namespace, compUnit.schema)
  }

  type TellCompilationUnit[F[_]] = Tell[F, Chain[CompilationUnit]]
  
  /**
    * Resolves all remote references and returns them as CompilationUnits
    *
    * @param initialUnit A compilation unit representing one json schema file
    * @return The initialUnit as well as all CompilationUnits associated with any remote references
    */
  def resolveRemoteReferences[F[_]: Parallel](
      initialUnit: CompilationUnit
  ): F[(Chain[ToSmithyError], Chain[CompilationUnit])] = {
    implicit val F: Monad[F] = Parallel[F].monad
    type ErrorLayer[A] = WriterT[F, Chain[ToSmithyError], A]
    type WriterLayer[A] = WriterT[ErrorLayer, Chain[CompilationUnit], A]
    
    internalResolveRemoteReferences[WriterLayer](initialUnit)
      .run
      .run
      .map { case (errors, (units, _)) => (errors, units) }
  }

  private def internalResolveRemoteReferences[F[_]: Parallel: TellCompilationUnit: TellError](initialUnit: CompilationUnit): F[Unit] = {
    implicit val F: Monad[F] = Parallel[F].monad
  
    def recordError(e: ToSmithyError): F[Unit] =
      Tell.tell(Chain.one(e))

    def recordCompilationUnit(compUnit: CompilationUnit): F[Unit] = 
      Tell.tell(Chain.one(compUnit))
    
    def createIntemediateUnitsForDefs(compilationUnit: CompilationUnit): Vector[IntermediateCompilationUnit] = {
      JsonSchemaOps
        .extractDefs(compilationUnit.json)
        .map { case (_, schema, _) => 
          IntermediateCompilationUnit(compilationUnit.namespace, schema)
        }
    }

    def recordAndCreateIntermediates(compilationUnit: CompilationUnit): F[Vector[IntermediateCompilationUnit]] =
      recordCompilationUnit(compilationUnit)
        .as(IntermediateCompilationUnit(compilationUnit) +: createIntemediateUnitsForDefs(compilationUnit))

    /**
     * Matches recursive schema nodes and ref nodes. 
     * When a recursive node is encountered, the sub-schema is returned as another compilation unit with references that may need to be resolved.
     * When a remote ref is encountered, the remote schema is pulled, parsed, recorded for use in compilation, and then the process continues.
     */
    def unfold(compUnit: IntermediateCompilationUnit): F[Vector[IntermediateCompilationUnit]] = {
      val CaseRef = new JsonSchemaCaseRefBuilder(
        Option(compUnit.schema.getId()),
        compUnit.ns
      ) {}

      compUnit.schema match {
        case CaseRef(Right(ParsedRef.Remote(uri, id))) => 
          val ns = id.name.segments.map(_.value.toString)

          Try(
            // TODO: Use some library that leverages effects to fetch the content from the URL
            Source.fromURL(uri.toURL()) 
              .getLines()
              .mkString(System.lineSeparator())
          ).toEither.fold(
            err => recordError(ToSmithyError.HttpError(uri, err)).as(Vector.empty),
            jawn.parse(_) match {
              case Left(err) => recordError(OpenApiParseError(ns, List(err.getMessage()))).as(Vector.empty)
              case Right(json) =>
                val newCompUnit = CompilationUnit(ns, LoadSchema(new JSONObject(json.noSpaces)), json)
                recordAndCreateIntermediates(newCompUnit)
          }
        )
        case CaseArray(_, arrSchema) =>
          F.pure(Vector(IntermediateCompilationUnit(compUnit.ns, arrSchema)))
        case CaseMap(_, mapSchema) =>
          F.pure(Vector(IntermediateCompilationUnit(compUnit.ns, mapSchema)))
        case CaseObject(_, objSchema) =>
          F.pure(
            objSchema.getPropertySchemas()
              .asScala
              .values
              .toVector
              .map(IntermediateCompilationUnit(compUnit.ns, _))
          )
        case CaseOneOf(_, oneOfSchemas) =>
          F.pure(
            oneOfSchemas
              .toVector
              .map(IntermediateCompilationUnit(compUnit.ns, _))
          )
        case CaseAllOf(_, allOfSchemas) =>
          F.pure(
            allOfSchemas
              .toVector
              .map(IntermediateCompilationUnit(compUnit.ns, _))
          )
        case _ => 
          F.pure(Vector.empty)
      }
    }

    recordAndCreateIntermediates(initialUnit)
      .flatMap(_.flatTraverse(recursion.unfoldPar(unfold)(_)))
      .void
  }
}
