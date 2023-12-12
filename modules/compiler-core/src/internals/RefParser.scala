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

import cats.data.NonEmptyChain
import org.typelevel.ci.CIString
import cats.syntax.all._

/*
 * The most complicated thing
 */
private[compiler] abstract class RefParser(ns: Path) {

  private def handleRef(
      uri: java.net.URI,
      pathSegsIn: List[String],
      fileName: String,
      segments: Option[List[String]],
      last: Option[String]
  ): Either[ToSmithyError, DefId] = {
    val pathSegs = pathSegsIn.dropWhile(_ == ".")

    val ups = pathSegs.takeWhile(_ == "..").size
    if (ns.length < ups + 1) {
      // namespace contains the name of the file
      val error = ToSmithyError.Restriction(s"Ref $uri goes too far up")
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

  def apply(ref: String): Either[ToSmithyError, DefId] =
    scala.util
      .Try(java.net.URI.create(ref))
      .toEither
      .leftMap(_ => ToSmithyError.BadRef(ref))
      .flatMap(uri =>
        Option(uri.getScheme()) match {
          case Some("file") => Right(uri)
          case None         => Right(uri)
          case Some(_)      => Left(ToSmithyError.BadRef(ref))
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
