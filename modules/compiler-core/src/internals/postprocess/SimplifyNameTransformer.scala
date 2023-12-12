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

package smithytranslate.compiler.internals
package postprocess

import cats.data.NonEmptyChain
import cats.syntax.all._
import org.typelevel.ci._
import scala.collection.compat._

private[compiler] object SimplifyNameTransformer extends IModelPostProcessor {

  private type OriginalName = Name
  private type UpdatedName = Name
  private type NameMapping = Map[OriginalName, UpdatedName]

  private final class Ops(
      simpleNames: Set[CIString],
      namesToUpdate: NameMapping
  ) {
    implicit class DefIdOps(defId: DefId) {
      def simplify: DefId = {
        val d =
          defId.copy(name = namesToUpdate.getOrElse(defId.name, defId.name))
        if (
          simpleNames(
            d.name.segments.last.value
          ) && !d.name.segments.last.isArbitrary // don't simplify if last is arbitrary
        )
          d.copy(name =
            Name(
              d.name.segments.last.mapValue(s =>
                CIString(s.toString.capitalize)
              )
            )
          )
        else
          d.copy(name =
            d.name.copy(segments =
              d.name.segments.map(s =>
                s.mapValue(s => CIString(s.toString.capitalize))
              )
            )
          )
      }
    }

    implicit class MemberIdOps(d: MemberId) {
      def simplify: MemberId =
        d.copy(modelId = d.modelId.simplify)
    }
  }

  private def updateAllReferences(
      defs: Vector[Definition],
      simpleNames: Set[CIString],
      namesToUpdate: NameMapping
  ): Vector[Definition] = {
    val ops = new Ops(simpleNames, namesToUpdate)
    import ops._
    defs.map {
      case s: Structure =>
        val newFs = s.localFields.map { f =>
          f.copy(
            id = f.id.simplify,
            tpe = f.tpe.simplify
          )
        }
        val newHints = s.hints.map {
          case h @ Hint.HasMixin(id) => h.copy(id = id.simplify)
          case h                     => h
        }
        s.copy(id = s.id.simplify, localFields = newFs, hints = newHints)
      case s: SetDef =>
        s.copy(
          id = s.id.simplify,
          member = s.member.simplify
        )
      case l: ListDef =>
        l.copy(
          id = l.id.simplify,
          member = l.member.simplify
        )
      case m: MapDef =>
        m.copy(
          id = m.id.simplify,
          key = m.key.simplify,
          value = m.value.simplify
        )
      case u: Union =>
        val newA = u.alts.map(a =>
          a.copy(
            id = a.id.simplify,
            tpe = a.tpe.simplify
          )
        )
        u.copy(id = u.id.simplify, alts = newA)
      case n: Newtype =>
        n.copy(
          id = n.id.simplify,
          target = n.target.simplify
        )
      case e: Enumeration => e.copy(id = e.id.simplify)
      case o: OperationDef =>
        val newE = o.errors.map(_.simplify)
        val newI = o.input.map(_.simplify)
        val newO = o.output.map(_.simplify)
        o.copy(
          id = o.id.simplify,
          errors = newE,
          input = newI,
          output = newO
        )
      case s: ServiceDef =>
        val newO = s.operations.map(_.simplify)
        s.copy(id = s.id.simplify, operations = newO)
    }
  }

  private def namesWithoutRepeats(
      name: Name
  ): Name = {
    name.segments.tail.foldLeft(Name(name.segments.head)) {
      case (newN, currentSegment) =>
        if (newN.segments.last == currentSegment) newN
        else newN :+ currentSegment
    }
  }

  private def namesWithoutArbitraryPrefix(name: Name): Name = {
    NonEmptyChain
      .fromSeq(name.segments.toList.dropWhile(_.isArbitrary))
      .map(Name(_))
      .getOrElse(name)
  }

  private def transform(in: Vector[Definition]): Vector[Definition] = {
    val allNames = in.map(_.id.name)
    val simpleNames = allNames
      // if two shape have the same simple name
      // irrespective of the case, we exclude them from
      // simpleName, eg: z2 and Z2
      .groupBy(_.segments.last.value)
      .collect {
        case (last, allInstances) if allInstances.length == 1 =>
          last
      }
      .toSet

    val pairedNames = allNames.zip(allNames).toMap

    val noRepeatNames: NameMapping =
      pairedNames.view
        .mapValues(namesWithoutRepeats)
        .toMap

    val noPrefixNames: NameMapping =
      pairedNames.view
        .mapValues(namesWithoutArbitraryPrefix)
        .toMap

    val p1: NameMapping =
      noRepeatNames.filterNot { case (_, value) =>
        noRepeatNames.contains(value)
      }

    val p2 = noPrefixNames.filterNot { case (_, value) =>
      noPrefixNames.contains(value)
    }

    val namesToUpdate = p1 ++ p2
    updateAllReferences(in, simpleNames, namesToUpdate)
  }

  def apply(in: IModel): IModel = {
    IModel(transform(in.definitions), in.suppressions)
  }
}
