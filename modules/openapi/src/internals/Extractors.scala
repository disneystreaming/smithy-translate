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

import io.swagger.v3.oas.models.media._
import scala.jdk.CollectionConverters._
import cats.data.NonEmptyChain
import cats.syntax.all._
import Primitive._
import org.typelevel.ci.CIString

object CaseEnum {
  def unapply(sch: Schema[_]): Option[Vector[String]] = sch match {
    case s: StringSchema if s.getEnum() != null && !s.getEnum().isEmpty =>
      Some(s.getEnum().asScala.toVector)
    case _ => None
  }
}

object CaseMap {
  def unapply(sch: Schema[_]): Option[Schema[_]] = {
    sch match {
      case m: MapSchema =>
        Option(m.getAdditionalProperties()).collect { case s: Schema[_] =>
          s
        }
      case _ => None
    }
  }
}

object IsFreeForm {
  def unapply(sch: Schema[_]): Boolean = {
    val isObjectSchema = sch.isInstanceOf[ObjectSchema]
    val hasNoProperties = Option(sch.getProperties()).forall(_.isEmpty())
    (Option(sch.getAdditionalProperties())
      .map {
        case b: java.lang.Boolean => b.booleanValue()
        case _                    => CaseMap.unapply(sch).isEmpty
      })
      .getOrElse(isObjectSchema && hasNoProperties)
  }
}

object Format {
  def unapply(sch: Schema[_]): Option[String] = Option(sch.getFormat())
}

object NoFormat {
  def unapply(sch: Schema[_]): Boolean = Option(sch.getFormat()).isEmpty
}

object & {
  def unapply[A](a: A): Some[(A, A)] = Some((a, a))
}

object CaseAllOf {
  def unapply(sch: Schema[_]): Option[Vector[Schema[_]]] = sch match {
    case composed: ComposedSchema =>
      Option(composed.getAllOf()).map(_.asScala.toVector)
    case _ => None
  }
}

object CaseOneOf {
  def unapply(sch: Schema[_]): Option[Vector[Schema[_]]] = sch match {
    case composed: ComposedSchema =>
      Option(composed.getOneOf()).map(_.asScala.toVector)
    case _ => None
  }
}

object CaseObject {
  def unapply(sch: Schema[_]): Option[Schema[_]] = sch match {
    case o: ObjectSchema => Some(o)
    case s: Schema[_] if Option(s.getProperties()).exists(_.asScala.nonEmpty) =>
      Some(s)
    case _ => None
  }
}

object NonEmptySegments {
  def unapply(s: String): Option[(List[String], String)] = {
    val segments = s.split("/").toList.filterNot(_.isEmpty())
    segments.lastOption.map(last => segments.dropRight(1) -> last)
  }
}

object CasePrimitive {
  def unapply(sch: Schema[_]): Option[Primitive] = sch match {
    // S:
    //   type: string
    //   format: timestamp
    case (_: StringSchema) & Format("timestamp") => Some(PTimestamp)

    // S:
    //   type: string
    //   format: password
    case (_: PasswordSchema) => Some(PString)

    // S:
    //   type: string
    case _: StringSchema | _: EmailSchema => Some(PString)

    // S:
    //   type: string
    //   format: byte | format: binary
    case _: ByteArraySchema | _: BinarySchema => Some(PBytes)

    // S:
    //   type: string
    //   format: uuid
    case _: UUIDSchema => Some(PUUID)

    // S:
    //   type: string
    //   format: date
    case _: DateSchema => Some(PDate)

    // S:
    //   type: string
    //   format: date-time
    case _: DateTimeSchema => Some(PDateTime)

    // N:
    //   type: number
    //   format: float
    case (_: NumberSchema) & Format("float") => Some(PFloat)

    // N:
    //   type: number
    //   format: double
    case (_: NumberSchema) & Format("double") => Some(PDouble)

    // N:
    //   type: number
    case (_: NumberSchema) & NoFormat() => Some(PDouble)

    // I:
    //   type: integer
    //   format: int32 | noformat
    case (_: IntegerSchema) & (Format("int32") | NoFormat()) => Some(PInt)

    // I:
    //   type: integer
    //   format: int64
    case (_: IntegerSchema) & Format("int64") => Some(PLong)

    // B:
    //   type: boolean
    case _: BooleanSchema => Some(PBoolean)

    case _ => None
  }
}

/*
 * The most complicated thing
 */
abstract class CaseRefBuilder(ns: Path) {
  def unapply(sch: Schema[_]): Option[Either[ModelError, DefId]] =
    Option(sch.get$ref).map(this.apply)

  private def handleRef(
      uri: java.net.URI,
      pathSegsIn: List[String],
      fileName: String,
      segments: Option[List[String]],
      last: Option[String]
  ): Either[ModelError, DefId] = {
    val pathSegs = pathSegsIn.dropWhile(_ == ".")

    val ups = pathSegs.takeWhile(_ == "..").size
    if (ns.length < ups + 1) {
      // namespace contains the name of the file
      val error = ModelError.Restriction(s"Ref $uri goes too far up")
      Left(error)
    } else {
      // Removing as many ".." as needed to standardise the namespace
      val nsPrefix = ns.toChain.toList.dropRight(ups + 1)
      val nsPrefix2 = pathSegs.drop(ups)
      val splitName = fileName.split('.')
      val nsLastPart =
        if (splitName.size > 1) splitName.dropRight(1).mkString(".")
        else fileName
      val fullNs =
        Namespace((nsPrefix ++ nsPrefix2 :+ nsLastPart))
      val name = (segments, last) match {
        case (Some(seg), Some(l)) =>
          NonEmptyChain
            .fromSeq(seg.map(s => Segment.Arbitrary(CIString(s))))
            .map(Name(_))
            .map(_ ++ Name.derived(l))
            .getOrElse(Name.derived(l))
        case _ => Name.derived(nsLastPart)
      }
      Right(DefId(fullNs, name))
    }
  }

  def apply(ref: String): Either[ModelError, DefId] =
    scala.util
      .Try(java.net.URI.create(ref))
      .toEither
      .leftMap(_ => ModelError.BadRef(ref))
      .flatMap(uri =>
        Option(uri.getScheme()) match {
          case Some("file") => Right(uri)
          case None         => Right(uri)
          case Some(_)      => Left(ModelError.BadRef(ref))
        }
      )
      .flatMap { uri =>
        (uri.getPath(), uri.getFragment()) match {
          case ("", NonEmptySegments(segments, last)) =>
            val n = Namespace(ns.toChain.toList)
            val name =
              NonEmptyChain
                .fromSeq(
                  segments.map(s => Segment.Arbitrary(CIString(s)))
                )
                .map(Name(_))
                .map(_ ++ Name.derived(last))
                .getOrElse(Name.derived(last))
            Right(DefId(n, name))
          case (NonEmptySegments(pathSegsIn, fileName), null) =>
            handleRef(uri, pathSegsIn, fileName, None, None)
          case (
                NonEmptySegments(pathSegsIn, fileName),
                NonEmptySegments(segments, last)
              ) =>
            handleRef(uri, pathSegsIn, fileName, Some(segments), Some(last))
        }
      }
}
