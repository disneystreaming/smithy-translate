package smithytranslate
package cli
package runners
import cats.data.NonEmptyList

object FileUtils {
  def readAll(
      paths: NonEmptyList[os.Path],
      includedExtensions: List[String]
  ): List[(NonEmptyList[String], String)] = {
    paths.toList.flatMap { path =>
      if (os.isDir(path)) {
        val files = os
          .walk(path)
          .filter(p => includedExtensions.contains(p.ext))
        files.map { in =>
          val subParts = in
            .relativeTo(path)
            .segments
            .toList
          val baseNs = path.segments.toList.lastOption.toList
          val nsPath =
            baseNs ++ subParts
          NonEmptyList.fromListUnsafe(nsPath) -> os
            .read(in)
        }.toList
      } else {
        List((NonEmptyList.of(path.last), os.read(path)))
      }
    }
  }

}
