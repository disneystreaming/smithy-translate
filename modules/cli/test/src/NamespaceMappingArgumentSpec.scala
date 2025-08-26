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

package smithytranslate.cli

import smithytranslate.cli.opts.CommonArguments
import cats.data.Validated
import cats.data.Chain
import cats.data.NonEmptyChain

class NamespaceMappingArgumentSpec extends munit.FunSuite {
  test("NamespaceMapping Argument should - Parse a valid source:target") {
    CommonArguments.namespaceMappingArgument.read("a.b.c:d.e.f") match {
      case Validated.Valid(mapping) =>
        assertEquals(
          mapping,
          CommonArguments.NamespaceMapping(
            original = NonEmptyChain("a", "b", "c"),
            remapped = Chain("d", "e", "f")
          )
        )
      case Validated.Invalid(errors) =>
        fail(s"Failed to parse valid input: ${errors.toList.mkString(", ")}")
    }
  }

  test("NamespaceMapping Argument should - Parse a valid source:target with single part namespaces") {
    CommonArguments.namespaceMappingArgument.read("a:b") match {
      case Validated.Valid(mapping) =>
        assertEquals(
          mapping,
          CommonArguments.NamespaceMapping(
            original = NonEmptyChain("a"),
            remapped = Chain("b")
          )
        )
      case Validated.Invalid(errors) =>
        fail(s"Failed to parse valid input: ${errors.toList.mkString(", ")}")
    }
  }

  test("NamespaceMapping Argument should - successfully parse with an empty target") {
    CommonArguments.namespaceMappingArgument.read("a.b.c:") match {
      case Validated.Valid(mapping) =>
        assertEquals(
          mapping,
          CommonArguments.NamespaceMapping(
            original = NonEmptyChain("a", "b", "c"),
            remapped = Chain()
          )
        )
      case Validated.Invalid(_) =>
    }
  }

  test("NamespaceMapping Argument should - fail to parse an invalid input missing colon") {
    CommonArguments.namespaceMappingArgument.read("a.b.c") match {
      case Validated.Valid(mapping) => fail(s"Parsed invalid input successfully: $mapping")
      case Validated.Invalid(_) =>
    }
  }

  test("NamespaceMapping Argument should - fail to parse an invalid input with empty source") {
    CommonArguments.namespaceMappingArgument.read(":a.b.c") match {
      case Validated.Valid(mapping) => fail(s"Parsed invalid input successfully: $mapping")
      case Validated.Invalid(_) => 
    }
  }
}
