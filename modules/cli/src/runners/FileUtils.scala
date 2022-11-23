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
package smithytranslate
package cli
package runners
import cats.data
import cats.data.NonEmptyList

object FileUtils {
  def readAll(
      paths: NonEmptyList[os.Path],
      includedExtensions: List[String]
  ): List[(data.NonEmptyList[String], String)] = {
    paths.toList.flatMap { path =>
      if (os.isDir(path)) {
        val files = os
          .walk(path)
          .filter(p => includedExtensions.contains(p.ext))
        files.map { in =>
          pathToNel(in, path) -> os
            .read(in)
        }.toList
      } else {
        List((NonEmptyList.of(path.last), os.read(path)))
      }
    }
  }

  def pathToNel(in: os.Path, path: os.Path): NonEmptyList[String] = {
    val subParts = in
      .relativeTo(path)
      .segments
      .toList
    val baseNs = path.segments.toList.lastOption.toList
    NonEmptyList.fromListUnsafe(baseNs ++ subParts)
  }

}
