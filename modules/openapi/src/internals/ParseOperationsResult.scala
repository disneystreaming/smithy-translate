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

package smithytranslate.openapi.internals

import cats.kernel.Monoid
import smithytranslate.openapi.ModelError

case class SuppressionFor(id: String, reason: String)
    extends (Namespace => Suppression) {
  def apply(namespace: Namespace): Suppression =
    Suppression(id, namespace, reason)
}

final case class ParseOperationsResult(
    errors: List[ModelError],
    results: Vector[OperationInfo],
    suppressions: Vector[SuppressionFor]
) {
  def addErrors(err: Seq[ModelError]): ParseOperationsResult =
    this.copy(errors = this.errors ++ err)
}

object ParseOperationsResult {
  implicit val monoid: Monoid[ParseOperationsResult] =
    new Monoid[ParseOperationsResult] {
      def empty: ParseOperationsResult =
        ParseOperationsResult(List.empty, Vector.empty, Vector.empty)
      def combine(
          x: ParseOperationsResult,
          y: ParseOperationsResult
      ): ParseOperationsResult =
        x.copy(
          errors = x.errors ++ y.errors,
          results = x.results ++ y.results,
          suppressions = x.suppressions ++ y.suppressions
        )
    }
}
