package smithytranslate.compiler

import cats.data.NonEmptyList

final case class FileContents(path: NonEmptyList[String], content: String)
