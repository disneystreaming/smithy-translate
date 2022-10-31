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
import cats.syntax.all._
import cats.data.NonEmptyChain
import org.typelevel.ci._
import cats.Show

/** Intermediate Model representation, that contains a scala representations of
  * what is eventually converted to smithy, with additional contextual
  * information.
  */
case class IModel(
    definitions: Vector[Definition],
    suppressions: Vector[Suppression]
)

object IModel {

  val empty = IModel(Vector.empty, Vector.empty)

  implicit val imodelMonoid: Monoid[IModel] =
    Monoid.instance(
      empty,
      (l, r) =>
        IModel(l.definitions ++ r.definitions, l.suppressions ++ r.suppressions)
    )

}

final case class Suppression(id: String, namespace: Namespace, reason: String)

/** Represents anything that can be directly reference, whether shape or member
  * :
  *
  * * Structure * Union * Newtype * Field * Alternative * Enumeration
  */
sealed abstract class Definition {
  def modelType: String
  def id: DefId
  def hints: List[Hint]
  def mapHints(f: List[Hint] => List[Hint]): Definition = this match {
    case d: Structure    => d.copy(hints = f(d.hints))
    case d: SetDef       => d.copy(hints = f(d.hints))
    case d: ListDef      => d.copy(hints = f(d.hints))
    case d: MapDef       => d.copy(hints = f(d.hints))
    case d: Union        => d.copy(hints = f(d.hints))
    case d: Newtype      => d.copy(hints = f(d.hints))
    case d: Enumeration  => d.copy(hints = f(d.hints))
    case d: OperationDef => d.copy(hints = f(d.hints))
    case d: ServiceDef   => d.copy(hints = f(d.hints))
  }
}

/** Self explanatory, represents a package, a folder in the filesystem, a path
  * in a more general URI ...
  */
case class Namespace(segments: List[String]) {
  def show: String = segments.mkString(".")
  override def toString: String = show
}

sealed trait Segment { self =>
  def value: CIString
  def mapValue(f: CIString => CIString): Segment = new Segment {
    def value: CIString = f(self.value)
  }
  def isArbitrary: Boolean = self match {
    case _: Segment.Arbitrary => true
    case _                    => false
  }
}
object Segment {
  final case class Arbitrary(value: CIString) extends Segment
  final case class Derived(value: CIString) extends Segment
  final case class StandardLib(value: CIString) extends Segment

  implicit val show: Show[Segment] = _.value.toString
}
final case class Name(segments: NonEmptyChain[Segment]) {
  def ++(that: Name): Name =
    this.copy(segments = this.segments ++ that.segments)
  def :+(s: Segment): Name = this.copy(segments = this.segments :+ s)

  def asRef: String = {
    val parts = segments.mkString_("/", "/", "")
    s"#$parts"
  }
}
object Name {
  def apply(head: Segment, tail: Segment*): Name = new Name(
    NonEmptyChain(head, tail: _*)
  )
  def arbitrary(value: String): Name = Name(Segment.Arbitrary(CIString(value)))
  def derived(value: String): Name = Name(Segment.Derived(CIString(value)))
  def stdLib(value: String): Name = Name(Segment.StandardLib(CIString(value)))
}

/** An identifier, composed of a namespace and a name. Can be called directly
  * from
  */
abstract class Id {
  def namespace: Namespace
  def name: Name
}

/** An identifier that specifically resolves to a shape (ie must not resolve to
  * a member)
  */
sealed case class DefId(
    namespace: Namespace,
    name: Name
) extends Id {

  override def toString() =
    namespace.segments.mkString(".") + "#" + name.segments.mkString_(".")

}

/** An identifier that specifically resolves to a member (ie must not resolve to
  * a shape)
  */
case class MemberId(
    modelId: DefId,
    memberName: Segment
) extends Id {
  def namespace: Namespace = modelId.namespace
  def name = modelId.name :+ memberName

  override def toString() =
    modelId.namespace.segments.mkString(".") + "#" +
      name.segments.mkString_(".")
}

/** Represents datatype that have an idea (as opposed to primitives and
  * collections)
  */
sealed abstract class Shape(val shapeType: String) extends Definition {
  def id: DefId
  def modelType: String = shapeType
}

/** Represents "members" of another type. A member of a Structure is a Field, a
  * member of a union is an Alternative.
  */
sealed abstract class Member(val memberType: String) {
  def modelType = memberType
}

/** A member of a structure.
  */
case class Field(
    id: MemberId,
    tpe: DefId,
    hints: List[Hint]
) extends Member("Field") {
  def mapHints(f: List[Hint] => List[Hint]) = this.copy(hints = f(hints))
}

/** A member of a union.
  */
case class Alternative(
    id: MemberId,
    tpe: DefId,
    hints: List[Hint]
) extends Member("Alternative")

/** Aka product, structure, object, case-class, etc.
  */
case class Structure(
    id: DefId,
    localFields: Vector[Field],
    parents: Vector[DefId],
    hints: List[Hint]
) extends Shape("Structure")

case class SetDef(
    id: DefId,
    member: DefId,
    hints: List[Hint] = Nil
) extends Shape("Set")

case class ListDef(
    id: DefId,
    member: DefId,
    hints: List[Hint] = Nil
) extends Shape("List")

case class MapDef(
    id: DefId,
    key: DefId,
    value: DefId,
    hints: List[Hint] = Nil
) extends Shape("Map")

/** Aka sums, coproducts, oneOfs, sealed-traits, etc.
  */
case class Union(
    id: DefId,
    alts: Vector[Alternative],
    kind: UnionKind,
    hints: List[Hint] = Nil
) extends Shape("Union")

/** Semantic type alias.
  */
case class Newtype(id: DefId, target: DefId, hints: List[Hint] = Nil)
    extends Shape("Newtype")

/** Stringly enumeration.
  */
case class Enumeration(
    id: DefId,
    values: Vector[String],
    hints: List[Hint] = Nil
) extends Shape("Enumeration")

case class OperationDef(
    id: DefId,
    input: Option[DefId],
    output: Option[DefId],
    errors: Vector[DefId],
    hints: List[Hint] = Nil
) extends Shape("Operation")

case class ServiceDef(
    id: DefId,
    operations: Vector[DefId],
    hints: List[Hint] = Nil
) extends Shape("Service")
