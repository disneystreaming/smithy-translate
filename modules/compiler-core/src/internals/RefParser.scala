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
import java.net.URI
import cats.data.Chain


private[compiler] sealed trait ParsedRef {
  def id: DefId
}
private[compiler] object ParsedRef {
  final case class Local(id: DefId) extends ParsedRef
  final case class Remote(uri: URI, id: DefId) extends ParsedRef
}

/*
 * The most complicated thing
 */
private[compiler] abstract class RefParser(ns: Path, namespaceRemapper: NamespaceRemapper) {
  
  def apply(ref: String): Either[ToSmithyError, ParsedRef] = {
    scala.util
      .Try(java.net.URI.create(ref))
      .toEither
      .leftMap(_ => ToSmithyError.BadRef(ref))
      .flatMap { uri =>
        Option(uri.getScheme()) match {
          case None | Some("file") => 
            parseURI(ns.toChain, uri)
              .map(remapNamespace)
              .map(ParsedRef.Local(_))

          case Some("http") | Some("https") => 
            parseRemoteURI(uri)
              .map(remapNamespace)
              .map(ParsedRef.Remote(uri, _))

          case _ => Left(ToSmithyError.BadRef(s"Unsupported URI scheme: ${uri.getScheme()}"))
        }
      }
  }

  private def parseURI(baseNamespace: Chain[String], uri: URI): Either[ToSmithyError, DefId] = {
    def initBaseNamespace: List[String] = 
      baseNamespace.initLast.map(_._1.toList).getOrElse(Nil)

    (uri.getPath(), uri.getFragment()) match {
      case ("", NonEmptySegments(segments, last)) =>
        // use the full namespace, because this ref is to a local definition
        val namespace = Namespace(baseNamespace.toList) 
        val name = deriveNameFromFragment(segments, last)
        Right(DefId(namespace, name))

      case (NonEmptySegments(pathSegsIn, fileNameWithExtension), null) =>
        val fileName = removeFileExtension(fileNameWithExtension)
        
        // we remove the last portion of the namespace, because we are 
        // referencing a separate file from the relative path of the current 
        // namespace
        val namespaceParts = initBaseNamespace ++ pathSegsIn :+ fileName


        normalizeRelativePath(uri, namespaceParts)
          .map(ns => {
            DefId(Namespace(ns.toList), Name.derived(fileName))
          })

      case (
            NonEmptySegments(pathSegsIn, fileNameWithExtension),
            NonEmptySegments(segments, last)
          ) =>
        
        val fileName = removeFileExtension(fileNameWithExtension)

        // we remove the last portion of the namespace, because we are 
        // referencing a separate file from the relative path of the current 
        // namespace
        val namespaceParts = initBaseNamespace ++ pathSegsIn :+ fileName

        normalizeRelativePath(uri, namespaceParts)
          .map { ns =>
            val name = deriveNameFromFragment(segments, last)
            DefId(Namespace(ns.toList), name)
          }

      case ("", null) => Left(ToSmithyError.BadRef(uri.toString))
    }
  }
  
  
  def parseRemoteURI(uri: URI): Either[ToSmithyError, DefId] = {
    // Use the reverse of the host segments as the base namespace
    // Similar to how java namespaces are derived from domain names
    val baseNamespace = 
      Chain.fromSeq(uri.getHost().split('.').reverse.toList)

    parseURI(Chain.empty, uri).map(defId => 
        defId.copy(namespace = Namespace(baseNamespace.toList ++ defId.namespace.segments))
    )
  }

  private def removeFileExtension(fileName: String): String = {
    val parts = fileName.split('.')

    if (parts.length > 1) parts.dropRight(1).mkString(".") 
    else fileName
  }
  
  private def deriveNameFromFragment(segments: List[String], lastSegment: String): Name  = 
    NonEmptyChain
      .fromSeq(segments.map(s => Segment.Arbitrary(CIString(s))))
      .map(Name(_))
      .map(_ ++ Name.derived(lastSegment))
      .getOrElse(Name.derived(lastSegment))

  private def normalizeRelativePath(uri: URI, parts: List[String]): Either[ToSmithyError, Chain[String]] = {
    parts.foldLeft(Chain.empty[String].asRight[ToSmithyError]) { 
      case (l @ Left(_), _) => l
      case (Right(acc), "..") =>
        acc.initLast match {
          case Some((init, _)) => Right(init)
          case None => Left(ToSmithyError.Restriction(s"Ref $uri goes too far up"))
        }
      case (Right(acc), "." | "") => Right(acc)
      case (Right(acc), seg) => Right(acc :+ seg)
    }
  }

  // If the beginning of the input namespace matches the keys in any of the namespaceRemapping entries, remap that
  // prefix to the cooresponding value
  def remapNamespace(defId: DefId): DefId = {
    defId.copy(namespace = 
      Namespace(namespaceRemapper.remap(defId.namespace.segments))
    )
  }

}
