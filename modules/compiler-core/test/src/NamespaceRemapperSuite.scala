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

import cats.data.{ Chain, NonEmptyChain }

final class NamespaceRemapperSuite extends munit.FunSuite {

  test("remap namespace list should remap the namespace starting from the left") {
    val remapper = new NamespaceRemapper(
      Map(
        NonEmptyChain.of("a", "b") -> Chain("c"),
      )
    )

    assertEquals(remapper.remap(List("a", "b", "d")), List("c", "d"))
  }
  
  test("remap namespace list should strip the prefix when there is an empty target namespace") {
    val remapper = new NamespaceRemapper(
      Map(
        NonEmptyChain.of("a", "b") -> Chain(),
      )
    )

    assertEquals(remapper.remap(List("a", "b", "c")), List("c"))
  }

  test("remap namespace list should not remap if the prefix does not match") {
    val remapper = new NamespaceRemapper(
      Map(
        NonEmptyChain.of("a", "b") -> Chain("c"),
      )
    )

    assertEquals(remapper.remap(List("x", "y", "z")), List("x", "y", "z"))
  }

  test("remap DefId should remap the namespace of the DefId") {
    val remapper = new NamespaceRemapper(
      Map(
        NonEmptyChain.of("a", "b") -> Chain("c"),
      )
    )

    val defId = DefId(Namespace(List("a", "b", "d")), Name.arbitrary("MyShape"))
    assertEquals(remapper.remap(defId), DefId(Namespace(List("c", "d")), Name.arbitrary("MyShape")))
  }

}
