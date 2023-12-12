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

sealed abstract class SmithyVersion extends Product with Serializable {
  override def toString(): String = this match {
    case SmithyVersion.One => "1.0"
    case SmithyVersion.Two => "2.0"
  }
}
object SmithyVersion {
  case object One extends SmithyVersion
  case object Two extends SmithyVersion

  def fromString(string: String): Either[String, SmithyVersion] = {
    val versionOne = Set("1", "1.0")
    val versionTwo = Set("2", "2.0")
    if (versionOne(string)) Right(SmithyVersion.One)
    else if (versionTwo(string)) Right(SmithyVersion.Two)
    else
      Left(
        s"expected one of ${versionOne ++ versionTwo}, but got '$string'"
      )
  }
}
