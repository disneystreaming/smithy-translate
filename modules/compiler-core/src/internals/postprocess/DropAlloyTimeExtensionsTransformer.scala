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

package smithytranslate.compiler.internals
package postprocess
import smithytranslate.compiler.internals.Hint.OpenApiExtension

// import cats.syntax.all._
// import scala.annotation.tailrec
// import cats.kernel.Eq
// import org.typelevel.ci._
// import scala.collection.mutable

private[compiler] object DropAlloyTimeExtensionsTransformer extends IModelPostProcessor {

  def apply(model: IModel): IModel = {
    model.copy(definitions = model.definitions.map(process))
  }

  private val formatsToDrop = Set(
    "date-time",
    "local-date",
    "local-time",
    "local-date-time",
    "offset-date-time",
    "offset-time",
    "zone-id",
    "zone-offset",
    "zoned-date-time",
    "year",
    "year-month",
    "month-day",
  )

  private def process(d: Definition) = {
    d match {
      case x: Newtype => x.copy(hints = removeAlloyTimeExtension(x.hints))
      case x: Structure => x.copy(localFields = updateFields(x.localFields))
      case x: Union => x.copy(alts = updateAlts(x.alts))
      case x => x.mapHints(removeAlloyTimeExtension)
    }
  }

  private def updateFields(fields: Vector[Field]) =
    fields.map { field =>
      field.copy(hints = removeAlloyTimeExtension(field.hints))
    }

  private def updateAlts(alts: Vector[Alternative]) =
    alts.map { alt =>
      alt.copy(hints = removeAlloyTimeExtension(alt.hints))
    }

  private def removeAlloyTimeExtension(hints: List[Hint]) =
    hints.flatMap {
      case OpenApiExtension(values) => {
        val updatedValues = values.filterNot { case (ext, value) =>
          ext == "x-format" && formatsToDrop.contains(value.toString())
        }

        if (updatedValues.isEmpty) None else Some(OpenApiExtension(updatedValues))
      }
      case hint => Some(hint)
    }
}

