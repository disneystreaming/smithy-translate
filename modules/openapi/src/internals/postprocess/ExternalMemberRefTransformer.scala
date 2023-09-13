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
package postprocess

import org.typelevel.ci._
import cats.data.NonEmptyChain
import cats.data.Chain
import scala.annotation.tailrec

/** This transformer is for the specific case where an external reference
  * targets the member of a structure from another file. In this case, we need
  * to update the target type to be whatever the target type of the referenced
  * member is in the target structure. For example, if there is structure A$a
  * that references B$b in another file, we would update A$a to instead target
  * the same type that B$b targets, rather than targeting B$b directly.
  */
object ExternalMemberRefTransformer extends IModelPostProcessor {
  private abstract class MatchesOne(val segment: Segment) {
    def unapply(segments: List[Segment]): Option[List[Segment]] =
      segments match {
        case `segment` :: rest => Some(rest)
        case _                 => None
      }
  }

  private trait MatchesOneNamed { self: MatchesOne =>
    object named {
      def unapply(segments: List[Segment]): Option[(Segment, List[Segment])] =
        segments match {
          case `segment` :: name :: rest => Some((name, rest))
          case _                         => None
        }
    }
  }

  private object Components
      extends MatchesOne(Segment.Arbitrary(ci"components"))

  private object Schemas
      extends MatchesOne(Segment.Arbitrary(ci"schemas"))
      with MatchesOneNamed

  private object Properties
      extends MatchesOne(Segment.Arbitrary(ci"properties"))

  def apply(model: IModel): IModel = {
    val allDefs = model.definitions.map(d => d.id.toString -> d).toMap

    def process(d: Definition): Definition = d match {
      case s @ Structure(_, localFields, _, _) =>
        val newFields = localFields.map { f =>
          f.tpe.name.segments.toChain.toList match {
            case Components(Schemas(_)) =>
              f.copy(tpe = updatedDefId(f.tpe))
            case _ => f
          }
        }
        s.copy(localFields = newFields)
      case other => other
    }

    def removeProperties(dId: DefId): Option[DefId] = {
      dId.name.segments.toChain.toList match {
        case Components(Schemas.named((name, Properties(rest)))) =>
          Some(
            dId.copy(name =
              Name(
                NonEmptyChain
                  .of(Components.segment, Schemas.segment, name)
                  .appendChain(Chain.fromSeq(rest))
              )
            )
          )
        case _ => None
      }
    }

    @tailrec
    def updatedDefId(dId: DefId, isParentProperty: Boolean = false): DefId =
      allDefs.get(dId.toString) match {
        case Some(Newtype(_, target, _)) =>
          removeProperties(target) match {
            case None     => if (isParentProperty) target else dId
            case Some(id) => updatedDefId(id, true)
          }
        case _ => dId
      }
    IModel(model.definitions.map(process), model.suppressions)

  }

}
