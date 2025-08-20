package smithytranslate.compiler
package internals
package json_schema

import scala.jdk.CollectionConverters._
import cats.data.NonEmptyChain
import cats.Parallel
import scala.io.Source
import cats.Monad
import io.circe.jawn
import cats.syntax.all._
import Extractors._
import io.circe.Json
import org.everit.json.schema.Schema
import cats.mtl._
import cats.data.Chain
import smithytranslate.compiler.json_schema.CompilationUnit
import scala.util.Try
import smithytranslate.compiler.ToSmithyError.OpenApiParseError
import cats.data.WriterT
import scala.util.Success
import scala.util.Failure


private[compiler] object RemoteRefResolver {
  
  sealed trait RemoteRefResolutionFailure extends Throwable {
    def message: String
    override def getMessage(): String = message
  }

  type TellCompilationUnit[F[_]] = Tell[F, Chain[CompilationUnit]]
  
  /**
    * Resolves all remote references and returns them as CompilationUnits
    *
    * @param namespace The namespace of the json schema
    * @param schemaJson the json representation of the schema
    * @return All CompilationUnits associated with the input json schema, including remote schemas
    */
  def resolveRemoteReferences[F[_]: Parallel](
      namespace: NonEmptyChain[String],
      schemaJson: Json
  ): F[(Chain[ToSmithyError], Chain[CompilationUnit])] = {
    implicit val F: Monad[F] = Parallel[F].monad
    type ErrorLayer[A] = WriterT[F, Chain[ToSmithyError], A]
    type WriterLayer[A] = WriterT[ErrorLayer, Chain[CompilationUnit], A]
    
    internalResolveRemoteReferences[WriterLayer](namespace, schemaJson)
      .run
      .run
      .map { case (errors, (units, _)) => (errors, units) }
  }


  private def internalResolveRemoteReferences[F[_]: Parallel: TellCompilationUnit: TellError](namespace: NonEmptyChain[String], schemaJson: Json): F[Unit] = {
    implicit val F: Monad[F] = Parallel[F].monad
    
  
    def recordError(e: ToSmithyError): F[Unit] =
      Tell.tell(Chain.one(e))

    def recordCompilationUnit(compUnit: CompilationUnit): F[Unit] = {
      Tell.tell(Chain.one(compUnit))
    }
    
    def recordAndCreateUnits(namespace: Path, jsonString: String): F[Vector[(Path, Schema)]] =
      jawn.parse(jsonString) match {
        case Left(err) => recordError(
          OpenApiParseError(namespace, List(err.getMessage())))
            .as(Vector.empty)

        case Right(json) =>
          JsonSchemaOps.createCompilationUnits(namespace, json)
            .traverse(compUnit => recordCompilationUnit(compUnit).as(compUnit))
            .map(_.map(compUnit => ((compUnit.namespace, compUnit.schema))))
      }


    /**
     * Matches recursive schema nodes and ref nodes. 
     * When a recursive node is encountered, the sub-schema(s) are returned such that the search for remote refs continues recursively.
     * When a remote ref is encountered, the remote schema is pulled, parsed, recorded for use in compilation, and then the process continues.
     * When a terminal node is encountered, an empty node is returned signaling the end of that branch of the search
     *
     */
    def unfold(input: (Path, Schema)): F[Vector[(Path, Schema)]] = {
      val (namespace, schema) = input
        
      val CaseRef = new JsonSchemaCaseRefBuilder(
        Option(schema.getId()),
        namespace
      ) {}

      schema match {

        // Remote ref found. Pull in the associated schema
        case CaseRef(Right(ParsedRef.Remote(uri, id))) => 
          val ns = 
            NonEmptyChain.fromChainUnsafe(Chain.fromSeq(id.namespace.segments))

          Try(
            // TODO: Use some library that leverages effects to fetch the content from the URL
            Source.fromURL(uri.toURL()) 
              .getLines()
              .mkString(System.lineSeparator())
          ) match {
            case Failure(err) =>
                recordError(ToSmithyError.HttpError(uri, err))
                  .as(Vector.empty)

            case Success(content) => recordAndCreateUnits(ns, content)
          }

        // Recursive node, return the sub-schema and its namespace 
        // so we can continue searching for remote refs
        case CaseArray(_, arrSchema) =>
          F.pure(Vector((namespace, arrSchema)))
        case CaseMap(_, mapSchema) =>
          F.pure(Vector((namespace, mapSchema)))
        case CaseObject(_, objSchema) =>
          F.pure(
            objSchema.getPropertySchemas()
              .asScala
              .values
              .toVector
              .map((namespace, _))
          )
        case CaseOneOf(_, oneOfSchemas) =>
          F.pure(
            oneOfSchemas
              .toVector
              .map((namespace, _))
          )
        case CaseAllOf(_, allOfSchemas) =>
          F.pure(
            allOfSchemas
              .toVector
              .map((namespace, _))
          )

        // Nonrecursive node, terminal case. Return nothing
        case _ => 
          F.pure(Vector.empty)
      }
    }

    recordAndCreateUnits(namespace, schemaJson.noSpaces)
      .flatMap(_.flatTraverse(recursion.unfoldPar(unfold)(_)))
      .void
  }
}
