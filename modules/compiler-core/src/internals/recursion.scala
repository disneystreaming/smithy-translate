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

import cats._
import cats.syntax.all._

// format: off
/**
  * A couple recursion-schemes, named in a less arcanic fashion than usual.
  * These functions allow to decouple the recursive aspect of model construction
  * and validation, from the domain-specific concerns.
  */
private[compiler] object recursion {

  type Fold[Pattern[_], A] = Pattern[A] => A
  type FoldF[F[_], Pattern[_], A] = Pattern[A] => F[A]
  type LabelledFoldF[F[_], Pattern[_], Label, A] = (Label, Pattern[A]) => F[A]

  type Unfold[Pattern[_], A] = A => Pattern[A]
  type UnfoldF[F[_], Pattern[_], A] = A => F[Pattern[A]]
  type LabelledUnfoldF[F[_], Pattern[_], Label, A] = A => F[(Label, Pattern[A])]

  /**
   * Traditionally called "hylo" for hylomorphism
   * (hylo == matter, morphism == change)
   *
   * The only recursive function in here. All other recursions/corecursions
   * originate from this function.
   *
   * Essentially, this takes a value A, expands it into a Pattern tree, and collapses
   * that tree into a value B, in a way that never requires the full tree to be in
   * memory.
   *
   * @param a the seed value
   * @param f the [[Fold]] function that collapses only one layer of pattern tree at a time
   * @param u the [[Unfold]] function that expands only one layer of a pattern tree at a time
   * @tparam Pattern the pattern functor (typically mirroring single layers of tree-like structure)
   * @tparam A the type of the original seed value
   * @tparam B the target type of the refold
   */
  def refold[Pattern[_]: Functor, A, B](
      unfold: Unfold[Pattern, A],
      fold: Fold[Pattern, B]
    )(a: A): B =
    fold(unfold(a).map(refold(unfold, fold)))

  /**
   * A monadic version of [[refold]] that can process separate branches in a
   * non-sequential way, typically used when the unfold has to be validated
   * and the accumulation of errors is desirable.
   */
  def refoldPar[F[_]: Parallel, Pattern[_]: Traverse, A, B](
      unfold: UnfoldF[F, Pattern, A],
      fold: FoldF[F, Pattern, B]
    )(a: A ): F[B] = {

    type FP[T] = F[Pattern[T]] // composition of F and F
    implicit val F: Monad[F] = Parallel[F].monad
    implicit val FP: Functor[FP] = Functor[F].compose[Pattern]

    def fold2(fpfb: F[Pattern[F[B]]]): F[B] = for {
      fmb <- fpfb
      fb <- fmb.parSequence
      f <- fold(fb)
    } yield f

    refold[FP, A, F[B]](unfold, fold2)(a)
  }

  /**
   * An method to unfold a recursive structure from a starting value
   */
  def unfoldPar[F[_]: Parallel: Monad, Pattern[_]: Traverse: FlatMap, A](
    unfold: UnfoldF[F, Pattern, A]
  )(a: A): F[Pattern[A]] = 
    refoldPar[F, Pattern, A, Pattern[A]](unfold, pa => pa.flatTraverse(Parallel[F].monad.pure))(a)

  /**
   * A version of [[refoldPar]] that accepts additional labels at each layer
   */
  def labelledRefoldPar[F[_]: Parallel, Pattern[_]: Traverse, Label, A, B](
      unfold: LabelledUnfoldF[F, Pattern, Label, A],
      fold: LabelledFoldF[F, Pattern, Label, B]
    )(a: A ): F[B] = {

    type WithLabel[T] = (Label, T)
    type PatternWithLabel[T] = WithLabel[Pattern[T]]
    implicit val patternWithLabelTraverse : Traverse[PatternWithLabel] = Traverse[WithLabel].compose[Pattern]

    refoldPar[F, PatternWithLabel, A, B](unfold, (fold.apply _).tupled)(a)
  }

}
