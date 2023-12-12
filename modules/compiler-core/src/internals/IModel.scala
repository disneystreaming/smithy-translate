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

import cats.kernel.Monoid
import cats.syntax.all._
import cats.data.NonEmptyChain
import org.typelevel.ci._
import cats.Show

/** Intermediate Model representation, that contains a scala representations of
  * what is eventually converted to smithy, with additional contextual
  * information.
  */
private[compiler] case class IModel(
    definitions: Vector[Definition],
    suppressions: Vector[Suppression]
)

private[compiler] object IModel {

  val empty = IModel(Vector.empty, Vector.empty)

  implicit val imodelMonoid: Monoid[IModel] =
    Monoid.instance(
      empty,
      (l, r) =>
        IModel(l.definitions ++ r.definitions, l.suppressions ++ r.suppressions)
    )

}

private[compiler] final case class Suppression(
    id: String,
    namespace: Namespace,
    reason: String
)

/** Represents anything that can be directly reference, whether shape or member
  * :
  *
  * * Structure * Union * Newtype * Field * Alternative * Enumeration
  */
private[compiler] sealed abstract class Definition {
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
private[compiler] case class Namespace(segments: List[String]) {
  def show: String = segments.mkString(".")
  override def toString: String = show
}

private[compiler] sealed trait Segment { self =>
  def value: CIString
  def mapValue(f: CIString => CIString): Segment = new Segment {
    def value: CIString = f(self.value)
  }
  def isArbitrary: Boolean = self match {
    case _: Segment.Arbitrary => true
    case _                    => false
  }
}
private[compiler] object Segment {
  final case class Arbitrary(value: CIString) extends Segment
  final case class Derived(value: CIString) extends Segment
  final case class StandardLib(value: CIString) extends Segment

  implicit val show: Show[Segment] = _.value.toString
}
private[compiler] final case class Name(
    segments: NonEmptyChain[Segment]
) {
  def ++(that: Name): Name =
    this.copy(segments = this.segments ++ that.segments)
  def :+(s: Segment): Name = this.copy(segments = this.segments :+ s)

  def asRef: String = {
    val parts = segments.mkString_("/", "/", "")
    s"#$parts"
  }
}
private[compiler] object Name {
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
private[compiler] abstract class Id {
  def namespace: Namespace
  def name: Name
}

/** An identifier that specifically resolves to a shape (ie must not resolve to
  * a member)
  */
private[compiler] sealed case class DefId(
    namespace: Namespace,
    name: Name
) extends Id {

  override def toString() =
    namespace.segments.mkString(".") + "#" + name.segments.mkString_(".")

}

/** An identifier that specifically resolves to a member (ie must not resolve to
  * a shape)
  */
private[compiler] case class MemberId(
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
private[compiler] sealed abstract class Shape(val shapeType: String)
    extends Definition {
  def id: DefId
  def modelType: String = shapeType
}

/** Represents "members" of another type. A member of a Structure is a Field, a
  * member of a union is an Alternative.
  */
private[compiler] sealed abstract class Member(val memberType: String) {
  def modelType = memberType
}

/** A member of a structure.
  */
private[compiler] case class Field(
    id: MemberId,
    tpe: DefId,
    hints: List[Hint]
) extends Member("Field") {
  def mapHints(f: List[Hint] => List[Hint]) = this.copy(hints = f(hints))
}

/** A member of a union.
  */
private[compiler] case class Alternative(
    id: MemberId,
    tpe: DefId,
    hints: List[Hint]
) extends Member("Alternative")

/** Aka product, structure, object, case-class, etc.
  */
private[compiler] case class Structure(
    id: DefId,
    localFields: Vector[Field],
    parents: Vector[DefId],
    hints: List[Hint]
) extends Shape("Structure")

private[compiler] case class SetDef(
    id: DefId,
    member: DefId,
    hints: List[Hint] = Nil
) extends Shape("Set")

private[compiler] case class ListDef(
    id: DefId,
    member: DefId,
    hints: List[Hint] = Nil
) extends Shape("List")

private[compiler] case class MapDef(
    id: DefId,
    key: DefId,
    value: DefId,
    hints: List[Hint] = Nil
) extends Shape("Map")

/** Aka sums, coproducts, oneOfs, sealed-traits, etc.
  */
private[compiler] case class Union(
    id: DefId,
    alts: Vector[Alternative],
    kind: UnionKind,
    hints: List[Hint] = Nil
) extends Shape("Union")

/** Semantic type alias.
  */
private[compiler] case class Newtype(
    id: DefId,
    target: DefId,
    hints: List[Hint] = Nil
) extends Shape("Newtype")

/** Stringly enumeration.
  */
private[compiler] case class Enumeration(
    id: DefId,
    values: Vector[String],
    hints: List[Hint] = Nil
) extends Shape("Enumeration")

private[compiler] case class OperationDef(
    id: DefId,
    input: Option[DefId],
    output: Option[DefId],
    errors: Vector[DefId],
    hints: List[Hint] = Nil
) extends Shape("Operation")

private[compiler] case class ServiceDef(
    id: DefId,
    operations: Vector[DefId],
    hints: List[Hint] = Nil
) extends Shape("Service")
