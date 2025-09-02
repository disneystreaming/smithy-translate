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

/* Copyright 2025 Disney Streaming
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

package smithytranslate.cli.opts

import com.monovore.decline._
import cats.data.NonEmptyList
import cats.data.ValidatedNel
import cats.data.Validated
import cats.implicits._
import cats.data.Chain
import cats.data.NonEmptyChain

object CommonArguments {
  case class NamespaceMapping(
      original: NonEmptyChain[String],
      remapped: Chain[String]
  )
  implicit val namespaceMappingArgument: Argument[NamespaceMapping] =
    new Argument[NamespaceMapping] {
      val defaultMetavar: String = "source.name.space:target.name.space"
      def read(string: String): ValidatedNel[String, NamespaceMapping] = {
        val result: Either[String, NamespaceMapping] =
          string.split(':') match {
            case Array(from, to) =>
              val sourceNs =
                NonEmptyChain.fromSeq(from.split('.').toList.filter(_.nonEmpty))
              val targetNs =
                Chain.fromSeq(to.split('.').toList.filter(_.nonEmpty))

              (sourceNs, targetNs) match {
                case (None, _) =>
                  Left("Source namespace must not be empty.")

                case (Some(f), t) =>
                  Right(NamespaceMapping(f, t))
              }
            case _ =>
              Left(
                s"""Invalid namespace remapping. 
                   |Expected input to be formatted as 'my.source.namespace:my.target.namespace'
                   |got: '$string'""".stripMargin
              )
          }

        result.toValidatedNel
      }
    }
}
