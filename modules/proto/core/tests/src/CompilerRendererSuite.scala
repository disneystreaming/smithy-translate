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

package smithyproto.proto3

import munit._
import software.amazon.smithy.model.Model
import smithyproto.validation.ProtoValidator

class CompilerRendererSuite extends FunSuite {

  test("top level - union") {
    val expected = """|syntax = "proto3";
                      |
                      |package com.example;
                      |
                      |message MyUnion {
                      |  oneof MyUnionOneof {
                      |    string name = 1;
                      |    int32 id = 2;
                      |  }
                      |}""".stripMargin
    val source = """|namespace com.example
                    |
                    |union MyUnion {
                    |  name: String,
                    |  id: Integer
                    |}
                    |""".stripMargin
    convertCheck(source, Map("com/example.proto" -> expected))
  }

  test("top level - document") {
    val source = """|namespace com.example
                    |
                    |document SomeDoc
                    |""".stripMargin
    val expected = """|syntax = "proto3";
                    |
                    |package com.example;
                    |
                    |import "google/protobuf/any.proto";
                    |
                    |message SomeDoc {
                    |  google.protobuf.Any value = 1;
                    |}
                    |""".stripMargin
    convertCheck(source, Map("com/example.proto" -> expected))
  }

  test("top level - string") {
    val source = """|namespace com.example
                  |
                  |string SomeString
                  |""".stripMargin
    val expected = """|syntax = "proto3";
                    |
                    |package com.example;
                    |
                    |message SomeString {
                    |  string value = 1;
                    |}
                    |""".stripMargin
    convertCheck(source, Map("com/example.proto" -> expected))
  }

  test("top level - int") {
    val source = """|namespace com.example
                  |
                  |integer SomeInt
                  |""".stripMargin
    val expected = """|syntax = "proto3";
                    |
                    |package com.example;
                    |
                    |message SomeInt {
                    |  int32 value = 1;
                    |}
                    |""".stripMargin
    convertCheck(source, Map("com/example.proto" -> expected))
  }

  test("top level - long") {
    val source = """|namespace com.example
                  |
                  |long SomeLong
                  |""".stripMargin
    val expected = """|syntax = "proto3";
                    |
                    |package com.example;
                    |
                    |message SomeLong {
                    |  int64 value = 1;
                    |}
                    |""".stripMargin
    convertCheck(source, Map("com/example.proto" -> expected))
  }

  test("top level - double") {
    val source = """|namespace com.example
                  |
                  |double SomeDouble
                  |""".stripMargin
    val expected = """|syntax = "proto3";
                    |
                    |package com.example;
                    |
                    |message SomeDouble {
                    |  double value = 1;
                    |}
                    |""".stripMargin

    convertCheck(source, Map("com/example.proto" -> expected))
  }

  test("top level - float") {
    val source = """|namespace com.example
                  |
                  |float SomeFloat
                  |""".stripMargin
    val expected = """|syntax = "proto3";
                    |
                    |package com.example;
                    |
                    |message SomeFloat {
                    |  float value = 1;
                    |}
                    |""".stripMargin
    convertCheck(source, Map("com/example.proto" -> expected))

  }

  test("top level - short") {
    val source = """|namespace com.example
                  |
                  |short SomeShort
                  |""".stripMargin
    val expected = """|syntax = "proto3";
                    |
                    |package com.example;
                    |
                    |message SomeShort {
                    |  int32 value = 1;
                    |}
                    |""".stripMargin
    convertCheck(source, Map("com/example.proto" -> expected))
  }

  test("top level - bool") {
    val source = """|namespace com.example
                  |
                  |boolean SomeBool
                  |""".stripMargin
    val expected = """|syntax = "proto3";
                    |
                    |package com.example;
                    |
                    |message SomeBool {
                    |  bool value = 1;
                    |}
                    |""".stripMargin
    convertCheck(source, Map("com/example.proto" -> expected))
  }

  test("top level - bytes") {
    val source = """|namespace com.example
                  |
                  |blob SomeBlob
                  |""".stripMargin
    val someBlob = """|syntax = "proto3";
                    |
                    |package com.example;
                    |
                    |message SomeBlob {
                    |  bytes value = 1;
                    |}
                    |""".stripMargin
    convertCheck(source, Map("com/example.proto" -> someBlob))
  }

  test("top level - big integer") {
    val source = """|namespace com.example
                  |
                  |bigInteger SomeBigInt
                  |""".stripMargin
    val expected = """|syntax = "proto3";
                    |
                    |package com.example;
                    |
                    |import "smithytranslate/definitions.proto";
                    |
                    |message SomeBigInt {
                    |  smithytranslate.BigInteger value = 1;
                    |}
                    |""".stripMargin
    convertCheck(source, Map("com/example.proto" -> expected))
  }

  test("top level - big decimal") {
    val source = """|namespace com.example
                  |
                  |bigDecimal SomeBigDec
                  |""".stripMargin
    val expected = """|syntax = "proto3";
                    |
                    |package com.example;
                    |
                    |import "smithytranslate/definitions.proto";
                    |
                    |message SomeBigDec {
                    |  smithytranslate.BigDecimal value = 1;
                    |}
                    |""".stripMargin
    convertWithApiCheck(source, Map("com/example.proto" -> expected))
  }

  test("top level - timestamp") {
    val source = """|namespace com.example
                  |
                  |timestamp SomeTs
                  |""".stripMargin
    val expected = """|syntax = "proto3";
                    |
                    |package com.example;
                    |
                    |import "smithytranslate/definitions.proto";
                    |
                    |message SomeTs {
                    |  smithytranslate.Timestamp value = 1;
                    |}
                    |""".stripMargin
    convertWithApiCheck(source, Map("com/example.proto" -> expected))
  }

  test("protoNumType") {
    val source = """|namespace com.example
                  |
                  |use alloy.proto#protoNumType
                  |
                  |structure LongNumbers {
                  |    @protoNumType("SIGNED")
                  |    signed: Long,
                  |
                  |    @protoNumType("UNSIGNED")
                  |    unsigned: Long,
                  |
                  |    @protoNumType("FIXED")
                  |    fixed: Long,
                  |
                  |    @protoNumType("FIXED_SIGNED")
                  |    FIXED_SIGNED: Long
                  |}
                  |
                  |structure IntNumbers {
                  |    @protoNumType("SIGNED")
                  |    signed: Integer,
                  |
                  |    @protoNumType("UNSIGNED")
                  |    unsigned: Integer,
                  |
                  |    @protoNumType("FIXED")
                  |    fixed: Integer,
                  |
                  |    @protoNumType("FIXED_SIGNED")
                  |    FIXED_SIGNED: Integer
                  |}
                  |
                  |structure RequiredLongNumbers {
                  |    @protoNumType("SIGNED")
                  |    @required
                  |    signed: Long,
                  |
                  |    @protoNumType("UNSIGNED")
                  |    @required
                  |    unsigned: Long,
                  |
                  |    @protoNumType("FIXED")
                  |    @required
                  |    fixed: Long,
                  |
                  |    @protoNumType("FIXED_SIGNED")
                  |    @required
                  |    FIXED_SIGNED: Long
                  |}
                  |
                  |structure RequiredIntNumbers {
                  |    @protoNumType("SIGNED")
                  |    @required
                  |    signed: Integer,
                  |
                  |    @protoNumType("UNSIGNED")
                  |    @required
                  |    unsigned: Integer,
                  |
                  |    @protoNumType("FIXED")
                  |    @required
                  |    fixed: Integer,
                  |
                  |    @protoNumType("FIXED_SIGNED")
                  |    @required
                  |    FIXED_SIGNED: Integer
                  |}
                  |""".stripMargin

    val expected = """|syntax = "proto3";
                      |
                      |package com.example;
                      |
                      |import "google/protobuf/wrappers.proto";
                      |
                      |message LongNumbers {
                      |  google.protobuf.Int64Value signed = 1;
                      |  google.protobuf.UInt64Value unsigned = 2;
                      |  google.protobuf.Int64Value fixed = 3;
                      |  google.protobuf.Int64Value FIXED_SIGNED = 4;
                      |}
                      |
                      |message IntNumbers {
                      |  google.protobuf.Int32Value signed = 1;
                      |  google.protobuf.UInt32Value unsigned = 2;
                      |  google.protobuf.Int32Value fixed = 3;
                      |  google.protobuf.Int32Value FIXED_SIGNED = 4;
                      |}
                      |
                      |message RequiredLongNumbers {
                      |  sint64 signed = 1;
                      |  uint64 unsigned = 2;
                      |  fixed64 fixed = 3;
                      |  sfixed64 FIXED_SIGNED = 4;
                      |}
                      |
                      |message RequiredIntNumbers {
                      |  sint32 signed = 1;
                      |  uint32 unsigned = 2;
                      |  fixed32 fixed = 3;
                      |  sfixed32 FIXED_SIGNED = 4;
                      |}""".stripMargin
    convertCheck(
      source,
      Map("com/example.proto" -> expected)
    )
  }

  test("inlined sparse maps") {
    val source = """|namespace com.example
                  |
                  |@sparse
                  |map StringMap {
                  |   key: String,
                  |   value: String
                  |}
                  |
                  |structure Foo {
                  |   object: StringMap
                  |}
                  |""".stripMargin
    val expected = """|syntax = "proto3";
                    |
                    |package com.example;
                    |
                    |import "google/protobuf/wrappers.proto";
                    |
                    |message Foo {
                    |  map<string, google.protobuf.StringValue> object = 1;
                    |}
                    |""".stripMargin
    convertCheck(source, Map("com/example.proto" -> expected))
  }

  test("inlined maps message") {
    val source = """|namespace com.example
                  |
                  |structure MapItem {
                  |  @required
                  |  name: String
                  |}
                  |
                  |map Map {
                  |   key: String,
                  |   value: MapItem
                  |}
                  |
                  |structure Foo {
                  |   values: Map
                  |}
                  |""".stripMargin
    val expected = """|syntax = "proto3";
                      |
                      |package com.example;
                      |
                      |message MapItem {
                      |  string name = 1;
                      |}
                      |
                      |message Foo {
                      |  map<string, com.example.MapItem> values = 1;
                      |}
                      |""".stripMargin
    convertCheck(source, Map("com/example.proto" -> expected))
  }

  test("inlined maps") {
    val source = """|namespace com.example
                  |
                  |map StringMap {
                  |   key: String,
                  |   value: Integer
                  |}
                  |
                  |structure Foo {
                  |   strings: StringMap
                  |}
                  |""".stripMargin
    val expected = """|syntax = "proto3";
                    |
                    |package com.example;
                    |
                    |message Foo {
                    |  map<string, int32> strings = 1;
                    |}
                    |""".stripMargin
    convertCheck(source, Map("com/example.proto" -> expected))
  }

  test("inlined lists") {
    val source = """|namespace com.example
                  |
                  |list StringList {
                  |   member: String
                  |}
                  |
                  |structure Foo {
                  |   strings: StringList
                  |}
                  |""".stripMargin
    val expected = """|syntax = "proto3";
                    |
                    |package com.example;
                    |
                    |message Foo {
                    |  repeated string strings = 1;
                    |}
                    |""".stripMargin
    convertCheck(source, Map("com/example.proto" -> expected))
  }

  test("inlined sparse lists") {
    val source = """|namespace com.example
                  |
                  |@sparse
                  |list StringList {
                  |   member: String
                  |}
                  |
                  |structure Foo {
                  |   strings: StringList
                  |}
                  |""".stripMargin
    val expected = """|syntax = "proto3";
                    |
                    |package com.example;
                    |
                    |import "google/protobuf/wrappers.proto";
                    |
                    |message Foo {
                    |  repeated google.protobuf.StringValue strings = 1;
                    |}
                    |""".stripMargin
    convertCheck(source, Map("com/example.proto" -> expected))
  }

  test("inlined lists message") {
    val source = """|namespace com.example
                  |
                  |structure ListItem {
                  |  @required
                  |  name: String
                  |}
                  |
                  |list List {
                  |   member: ListItem
                  |}
                  |
                  |structure Foo {
                  |   strings: List
                  |}
                  |""".stripMargin

    val expected = """|syntax = "proto3";
                   |
                   |package com.example;
                   |
                   |message ListItem {
                   |  string name = 1;
                   |}
                   |
                   |message Foo {
                   |  repeated com.example.ListItem strings = 1;
                   |}
                   |""".stripMargin
    convertCheck(source, Map("com/example.proto" -> expected))
  }

  test("transitive structure with protoEnabled") {
    val source = """|namespace test
                  |
                  |use alloy.proto#protoEnabled
                  |
                  |@protoEnabled
                  |structure Test {
                  |  o: Other
                  |}
                  |
                  |structure Other {
                  |  s: String
                  |}
                  |""".stripMargin
    val expected = """|syntax = "proto3";
                    |
                    |package test;
                    |
                    |import "google/protobuf/wrappers.proto";
                    |
                    |message Test {
                    |  test.Other o = 1;
                    |}
                    |
                    |message Other {
                    |  google.protobuf.StringValue s = 1;
                    |}
                    |""".stripMargin
    convertCheck(source, Map("test/definitions.proto" -> expected))
  }

  test("service with protoEnabled") {
    val source = """|namespace test
                  |
                  |use alloy.proto#protoEnabled
                  |
                  |@protoEnabled
                  |service Test {
                  |  operations: [Op]
                  |}
                  |
                  |@http(method: "POST", uri: "/test", code: 200)
                  |operation Op {
                  |  input: Struct,
                  |  output: Struct
                  |}
                  |
                  |structure Struct {
                  |  s: String
                  |}
                  |
                  |/// This one should not be converted
                  |service Other {
                  |  operations: [Op]
                  |}
                  |""".stripMargin
    val expected = """|syntax = "proto3";
                      |
                      |package test;
                      |
                      |import "google/protobuf/wrappers.proto";
                      |
                      |service Test {
                      |  rpc Op(test.Struct) returns (test.Struct);
                      |}
                      |
                      |message Struct {
                      |  google.protobuf.StringValue s = 1;
                      |}
                      |""".stripMargin

    convertCheck(source, Map("test/definitions.proto" -> expected))
  }

  test("enum with protoIndex") {
    val source = """|$version: "2"
                    |namespace test
                    |
                    |use alloy.proto#protoIndex
                    |use alloy.proto#protoEnabled
                    |
                    |@protoEnabled
                    |service Test {
                    |  operations: [Op]
                    |}
                    |
                    |@http(method: "POST", uri: "/test", code: 200)
                    |operation Op {
                    |  input: Struct,
                    |  output: Struct
                    |}
                    |
                    |structure Struct {
                    | s: LoveProto
                    | }
                    |
                    |enum LoveProto {
                    |  @protoIndex(0)
                    |  YES
                    |  @protoIndex(2)
                    |  NO
                    |}
                    |""".stripMargin
    val expected = """|syntax = "proto3";
                      |
                      |package test;
                      |
                      |service Test {
                      |  rpc Op(test.Struct) returns (test.Struct);
                      |}
                      |
                      |message Struct {
                      |  test.LoveProto s = 1;
                      |}
                      |
                      |enum LoveProto {
                      |  YES = 0;
                      |  NO = 2;
                      |}""".stripMargin

    convertCheck(
      source,
      Map("test/definitions.proto" -> expected)
    )
  }

  test("cycle") {
    val source = """|$version: "2"
                    |
                    |namespace avoid.cyclic.in.namespace
                    |
                    |structure One {
                    |    twos: TwoList
                    |}
                    |
                    |list TwoList {
                    |    member: Two
                    |}
                    |
                    |structure Two {
                    |    one: One
                    |}""".stripMargin
    val expected = """|syntax = "proto3";
                      |
                      |package avoid.cyclic.in.namespace;
                      |
                      |message One {
                      |  repeated avoid.cyclic.in.namespace.Two twos = 1;
                      |}
                      |
                      |message Two {
                      |  avoid.cyclic.in.namespace.One one = 1;
                      |}""".stripMargin
    convertCheck(source, Map("avoid/cyclic/in/namespace.proto" -> expected))
  }

  test("multiple namespaces") {
    def src(ns: String) = s"""|namespace com.$ns
                              |
                              |string SomeString
                              |""".stripMargin
    def expected(ns: String) = s"""|syntax = "proto3";
                                   |
                                   |package com.$ns;
                                   |
                                   |message SomeString {
                                   |  string value = 1;
                                   |}
                                   |""".stripMargin
    convertChecks(
      Map("ns1.smithy" -> src("ns1"), "ns2.smithy" -> src("ns2")),
      Map(
        "com/ns1.proto" -> expected("ns1"),
        "com/ns2.proto" -> expected("ns2")
      )
    )
  }

  test("proto options as metadata") {
    val source = """|$version: "2"
                    |
                    |metadata "proto_options" = [{
                    |  "another.namespace": {
                    |    "java_multiple_files": "true",
                    |    "java_package": "\"demo.hello\""
                    |  }
                    |}]
                    |
                    |namespace another.namespace
                    |
                    |string SomeString
                    |""".stripMargin
    val expected = """|syntax = "proto3";
                      |
                      |option java_multiple_files = true;
                      |option java_package = "demo.hello";
                      |
                      |package another.namespace;
                      |
                      |message SomeString {
                      |  string value = 1;
                      |}
                      |""".stripMargin
    convertCheck(source, Map("another/namespace.proto" -> expected))
  }

  /** Perform the same check as convertCheck but include the smithytranslate
    * namespace. To do so it prepends the proto api to your `expected` value.
    */
  private def convertWithApiCheck(
      source: String,
      expected: Map[String, String]
  )(implicit loc: Location): Unit = {
    val expectedApi = s"""|syntax = "proto3";
                          |
                          |package smithytranslate;
                          |
                          |message BigDecimal {
                          |  string value = 1;
                          |}
                          |
                          |message BigInteger {
                          |  string value = 1;
                          |}
                          |
                          |message Timestamp {
                          |  int64 value = 1;
                          |}
                          |""".stripMargin
    val newExpected = Map(
      "smithytranslate/definitions.proto" -> expectedApi
    ) ++ expected
    convertCheck(source, newExpected, excludeProtoApi = false)
  }

  private def convertCheck(
      source: String,
      expected: Map[String, String],
      excludeProtoApi: Boolean = true
  )(implicit loc: Location): Unit = {
    convertChecks(
      Map("inlined-in-test.smithy" -> source),
      expected,
      excludeProtoApi
    )
  }

  private def convertChecks(
      sources: Map[String, String],
      expected: Map[String, String],
      excludeProtoApi: Boolean = true
  )(implicit loc: Location): Unit = {
    def render(srcs: Map[String, String]): List[(String, String)] = {
      val m = {
        val assembler = Model
          .assembler()
          .discoverModels()
          .addShapes(
            smithytranslate.BigInteger.shape,
            smithytranslate.BigDecimal.shape,
            smithytranslate.Timestamp.shape
          )
        srcs.foreach { case (name, src) =>
          assembler.addUnparsedModel(name, src)
        }

        assembler
          .assemble()
          .unwrap()
      }
      val c = new Compiler()
      val res = c.compile(m)
      if (res.isEmpty) { fail("Expected compiler output") }
      res.map { of =>
        val fileName = of.path.mkString("/")
        fileName -> Renderer.render(of.unit)
      }
    }

    val actual = render(sources).sortWith { case ((name1, _), (_, _)) =>
      name1.startsWith("smithytranslate")
    }
    ProtoValidator.run(actual: _*)
    val exclude =
      if (excludeProtoApi)
        Set("smithytranslate/definitions.proto")
      else Set.empty[String]

    val finalFiles = actual.collect {
      case (name, contents) if !exclude(name) => (name, contents)
    }.toMap

    // Checking that we get the same keyset as expected
    assertEquals(finalFiles.keySet, expected.keySet)
    // Checking that all contents match
    for {
      (file, content) <- expected
    } {
      assertEquals(finalFiles(file).trim(), content.trim())
    }
  }

}
