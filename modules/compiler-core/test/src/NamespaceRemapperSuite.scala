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
