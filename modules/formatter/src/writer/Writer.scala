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
package smithytranslate
package formatter
package writers

trait Writer[A] {
  def write(a: A): String
}

object Writer {
  def apply[A: Writer]: Writer[A] = implicitly[Writer[A]]
  def fromToString[A]: Writer[A] = (a: A) => a.toString
  implicit val stringWriter: Writer[String] = fromToString[String]
  implicit val charWriter: Writer[Char] = fromToString[Char]
  implicit val intWriter: Writer[Int] = fromToString[Int]
  implicit val shortWriter: Writer[Short] = fromToString[Short]
  implicit val longWriter: Writer[Long] = fromToString[Long]
  implicit val floatWriter: Writer[Float] = fromToString[Float]
  implicit val doubleWriter: Writer[Double] = fromToString[Double]
  implicit val booleanWriter: Writer[Boolean] = (a: Boolean) =>
    if (a) "true" else "false"
  implicit val unitWriter: Writer[Unit] = (_: Unit) => ""

  implicit def tuple2[A: Writer, B: Writer]: Writer[(A, B)] = (a: (A, B)) =>
    s"${Writer[A].write(a._1)}${Writer[B].write(a._2)}"
  implicit def tuple3[A: Writer, B: Writer, C: Writer]: Writer[(A, B, C)] =
    (a: (A, B, C)) =>
      s"${Writer[A].write(a._1)}${Writer[B].write(a._2)}${Writer[C].write(a._3)}"
  implicit def tuple4[A: Writer, B: Writer, C: Writer, D: Writer]: Writer[
    (A, B, C, D)
  ] = (a: (A, B, C, D)) =>
    s"${Writer[A].write(a._1)}${Writer[B]
        .write(a._2)}${Writer[C].write(a._3)}${Writer[D].write(a._4)}"
  implicit def tuple5[A: Writer, B: Writer, C: Writer, D: Writer, E: Writer]
      : Writer[
        (A, B, C, D, E)
      ] = (a: (A, B, C, D, E)) =>
    s"${Writer[A].write(a._1)}${Writer[B].write(a._2)}${Writer[C].write(a._3)}${Writer[D]
        .write(a._4)}${Writer[E].write(a._5)}"

  implicit def option[A: Writer]: Writer[Option[A]] = {
    case Some(a) => Writer[A].write(a)
    case None    => ""
  }
  implicit def either[A, B](implicit
      writerA: Writer[A],
      writerB: Writer[B]
  ): Writer[Either[A, B]] = {
    case Left(a)  => writerA.write(a)
    case Right(b) => writerB.write(b)
  }

  def write[A](f: A => String): Writer[A] = (a: A) => f(a)

  implicit class WriterOps[A: Writer](val a: A) {
    val writer: Writer[A] = Writer[A]
    def write: String = writer.write(a)
  }

  implicit class WriterOpsIterable[A: Writer](val iterable: Iterable[A]) {
    self =>
    val writer: Writer[A] = Writer[A]
    def writeN(prefix: String, delimiter: String, suffix: String): String =
      iterable.iterator
        .map(writer.write)
        .filter(_.nonEmpty)
        .mkString_(prefix, delimiter, suffix)

    def writeN(delimiter: String): String = writeN("", delimiter, "")
    def writeN: String = writeN("", "", "")
  }

}
