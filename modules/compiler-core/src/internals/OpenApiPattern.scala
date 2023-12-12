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
package internals

import cats.{Applicative, Traverse}
import cats.syntax.all._
import cats.Eval
import OpenApiPattern.HintedAlternative

// format: off
private[compiler] sealed trait OpenApiPattern[+A] {
  def context: Context

  def mapContext(f: Context => Context) : OpenApiPattern[A] = this match {
    case OpenApiPrimitive(context, tpe) => OpenApiPrimitive(f(context), tpe)
    case OpenApiRef(context, ref) => OpenApiRef(f(context), ref)
    case OpenApiEnum(context, values) => OpenApiEnum(f(context), values)
    case OpenApiShortStop(context, error) => OpenApiShortStop(f(context), error)
    case OpenApiNull(context) => OpenApiNull(f(context))
    case OpenApiMap(context, items) => OpenApiMap(f(context), items)
    case OpenApiArray(context, items) => OpenApiArray(f(context), items)
    case OpenApiSet(context, items) => OpenApiSet(f(context), items)
    case OpenApiAllOf(context, allOfs) => OpenApiAllOf(f(context), allOfs)
    case OpenApiOneOf(context, alternatives, kind) => OpenApiOneOf(f(context), alternatives, kind)
    case OpenApiObject(context, items) => OpenApiObject(f(context), items)
  }

  def traverse[F[_], B](
      f: A => F[B]
    )(implicit F: Applicative[F]
    ): F[OpenApiPattern[B]] = this match {
    case OpenApiPrimitive(context, tpe)            => F.pure(OpenApiPrimitive(context, tpe))
    case OpenApiRef(context, id)                   => F.pure(OpenApiRef(context, id))
    case OpenApiEnum(context, values)              => F.pure(OpenApiEnum(context, values))
    case OpenApiShortStop(context, error)          => F.pure(OpenApiShortStop(context, error))
    case OpenApiNull(context)                      => F.pure(OpenApiNull(context))
    case OpenApiMap(context, items)                => f(items).map(OpenApiMap(context, _))
    case OpenApiArray(context, items)              => f(items).map(OpenApiArray(context, _))
    case OpenApiSet(context, items)                => f(items).map(OpenApiSet(context, _))
    case OpenApiAllOf(context, allOfs)             => allOfs.traverse(f).map(OpenApiAllOf(context, _))
    case OpenApiOneOf(context, alternatives, kind) => Traverse[Vector]
                                                        .compose[(List[Hint], *)]
                                                        .traverse(alternatives)(f)
                                                        .map(OpenApiOneOf(context, _, kind))
    case OpenApiObject(context, items)             => Traverse[Vector]
                                                       .compose[((String, Boolean), *)]
                                                       .traverse(items)(f)
                                                       .map(OpenApiObject(context, _))
  }
}

private[compiler] object OpenApiPattern {

  type HintedAlternative[A] = (List[Hint], A)

  implicit val openapiPatternTraverse: Traverse[OpenApiPattern] =
    new Traverse[OpenApiPattern] {
      def traverse[G[_]: Applicative, A, B](
          fa: OpenApiPattern[A]
        )(f: A => G[B]
        ): G[OpenApiPattern[B]] = fa.traverse(f)

      def foldLeft[A, B](fa: OpenApiPattern[A], b: B)(f: (B, A) => B): B =
        ??? // ...

      def foldRight[A, B](
          fa: OpenApiPattern[A],
          lb: Eval[B]
        )(f: (A, Eval[B]) => Eval[B]
        ): Eval[B] = ??? // ...
    }

}

// format: off
  case class OpenApiPrimitive(context: Context, tpe: Primitive)                                             extends OpenApiPattern[Nothing]
  case class OpenApiRef(context: Context, ref: DefId)                                                       extends OpenApiPattern[Nothing]
  case class OpenApiEnum(context: Context, values: Vector[String])                                          extends OpenApiPattern[Nothing]
  case class OpenApiShortStop(context: Context, error: ToSmithyError)                                          extends OpenApiPattern[Nothing]
  case class OpenApiNull(context: Context)                                                                  extends OpenApiPattern[Nothing]
  case class OpenApiMap[A](context: Context, items: A)                                                      extends OpenApiPattern[A]
  case class OpenApiArray[A](context: Context, items: A)                                                    extends OpenApiPattern[A]
  case class OpenApiSet[A](context: Context, items: A)                                                      extends OpenApiPattern[A]
  case class OpenApiAllOf[A](context: Context, allOfs: Vector[A])                                           extends OpenApiPattern[A]
  case class OpenApiOneOf[A](context: Context, alternatives: Vector[HintedAlternative[A]], kind: UnionKind) extends OpenApiPattern[A]
  case class OpenApiObject[A](context: Context, items: Vector[((String, Boolean), A)])                      extends OpenApiPattern[A]
  // format: on
