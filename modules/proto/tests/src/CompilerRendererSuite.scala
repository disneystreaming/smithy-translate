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
import software.amazon.smithy.model.validation.ValidatedResultException
import smithyproto.validation.ProtoValidator

class CompilerRendererSuite extends FunSuite {

  test("top level - union") {
    val source = """|namespace com.example
                    |
                    |use alloy.proto#protoWrapped
                    |
                    |union MyUnion {
                    |  name: String
                    |  id: Integer
                    |}
                    |""".stripMargin

    val expected = """|syntax = "proto3";
                      |
                      |package com.example;
                      |
                      |message MyUnion {
                      |  oneof definition {
                      |    string name = 1;
                      |    int32 id = 2;
                      |  }
                      |}
                      |""".stripMargin
    convertCheck(source, Map("com/example/example.proto" -> expected))
  }

  test("@protoInlinedOneOf union - used within only one data structure") {
    val source = """|namespace com.example
                    |
                    |use alloy.proto#protoInlinedOneOf
                    |
                    |structure WithUnion {
                    |  @required
                    |  age: Integer
                    |  @required
                    |  myUnion: MyUnion
                    |  @required
                    |  other: String
                    |}
                    |
                    |@protoInlinedOneOf
                    |union MyUnion {
                    |  name: String,
                    |  id: Integer
                    |}
                    |""".stripMargin

    val expected = """|syntax = "proto3";
                      |
                      |package com.example;
                      |
                      |message WithUnion {
                      |  int32 age = 1;
                      |  oneof myUnion {
                      |    string name = 2;
                      |    int32 id = 3;
                      |  }
                      |  string other = 4;
                      |}
                      |""".stripMargin
    convertCheck(source, Map("com/example/example.proto" -> expected))
  }

  test(
    "@protoInlinedOneOf union - cannot be used within multiple data structures"
  ) {
    val source = """|namespace com.example
                    |
                    |use alloy.proto#protoInlinedOneOf
                    |
                    |structure WithUnion {
                    |  @required
                    |  myUnion: MyUnion
                    |}
                    |
                    |list OfUnion {
                    |  member: MyUnion
                    |}
                    |
                    |@protoInlinedOneOf
                    |union MyUnion {
                    |  name: String,
                    |  id: Integer
                    |}
                    |""".stripMargin

    // The error is thrown by the validator associated with the `protoInlinedOneOf`
    // annotation.
    intercept[ValidatedResultException](
      convertCheck(source, Map.empty)
    )
  }

  test("@protoInlinedOneOf union - cannot be unused") {
    val source = """|namespace com.example
                    |
                    |use alloy.proto#protoInlinedOneOf
                    |
                    |@protoInlinedOneOf
                    |union MyUnion {
                    |  name: String,
                    |  id: Integer
                    |}
                    |""".stripMargin

    // The error is thrown by the validator associated with the `protoInlinedOneOf`
    // annotation.
    intercept[ValidatedResultException](
      convertCheck(source, Map.empty)
    )
  }

  test("document") {
    val source = """|namespace com.example
                    |
                    |structure SomeDoc {
                    |  value: Document
                    |}
                    |""".stripMargin

    val expected = """|syntax = "proto3";
                      |
                      |package com.example;
                      |
                      |import "google/protobuf/struct.proto";
                      |
                      |message SomeDoc {
                      |  google.protobuf.Value value = 1;
                      |}
                      |""".stripMargin

    convertCheck(source, Map("com/example/example.proto" -> expected))
  }

  test("Primitive fields") {
    val source = """|namespace com.example
                    |
                    |structure Struct {
                    |  boolean: Boolean
                    |  int: Integer
                    |  long: Long
                    |  byte: Byte
                    |  short: Short
                    |  float: Float
                    |  double: Double
                    |  bigInteger: BigInteger
                    |  bigDecimal: BigDecimal
                    |  blob: Blob
                    |  document: Document
                    |  string: String
                    |  timestamp: Timestamp
                    |}
                    |""".stripMargin

    val expected = """|syntax = "proto3";
                      |
                      |package com.example;
                      |
                      |import "google/protobuf/struct.proto";
                      |
                      |import "google/protobuf/timestamp.proto";
                      |
                      |message Struct {
                      |  bool boolean = 1;
                      |  int32 int = 2;
                      |  int64 long = 3;
                      |  int32 byte = 4;
                      |  int32 short = 5;
                      |  float float = 6;
                      |  double double = 7;
                      |  string bigInteger = 8;
                      |  string bigDecimal = 9;
                      |  bytes blob = 10;
                      |  google.protobuf.Value document = 11;
                      |  string string = 12;
                      |  google.protobuf.Timestamp timestamp = 13;
                      |}
                      |""".stripMargin
    convertCheck(source, Map("com/example/example.proto" -> expected))
  }

  test("Primitive fields (wrapped)") {
    val source = """|namespace com.example
                    |
                    |use alloy.proto#protoWrapped
                    |
                    |structure Struct {
                    |  @protoWrapped
                    |  boolean: Boolean
                    |  @protoWrapped
                    |  int: Integer
                    |  @protoWrapped
                    |  long: Long
                    |  @protoWrapped
                    |  byte: Byte
                    |  @protoWrapped
                    |  short: Short
                    |  @protoWrapped
                    |  float: Float
                    |  @protoWrapped
                    |  double: Double
                    |  @protoWrapped
                    |  bigInteger: BigInteger
                    |  @protoWrapped
                    |  bigDecimal: BigDecimal
                    |  @protoWrapped
                    |  blob: Blob
                    |  @protoWrapped
                    |  document: Document
                    |  @protoWrapped
                    |  string: String
                    |  @protoWrapped
                    |  timestamp: Timestamp
                    |}
                    |""".stripMargin

    val expected = """|syntax = "proto3";
                      |
                      |package com.example;
                      |
                      |import "google/protobuf/wrappers.proto";
                      |
                      |import "alloy/protobuf/wrappers.proto";
                      |
                      |message Struct {
                      |  google.protobuf.BoolValue boolean = 1;
                      |  google.protobuf.Int32Value int = 2;
                      |  google.protobuf.Int64Value long = 3;
                      |  alloy.protobuf.ByteValue byte = 4;
                      |  alloy.protobuf.ShortValue short = 5;
                      |  google.protobuf.FloatValue float = 6;
                      |  google.protobuf.DoubleValue double = 7;
                      |  alloy.protobuf.BigIntegerValue bigInteger = 8;
                      |  alloy.protobuf.BigDecimalValue bigDecimal = 9;
                      |  google.protobuf.BytesValue blob = 10;
                      |  alloy.protobuf.DocumentValue document = 11;
                      |  google.protobuf.StringValue string = 12;
                      |  alloy.protobuf.TimestampValue timestamp = 13;
                      |}
                      |""".stripMargin
    convertCheck(source, Map("com/example/example.proto" -> expected))
  }

  test("Primitive references") {
    val source = """|namespace com.example
                    |
                    |use alloy.proto#protoWrapped
                    |
                    |boolean MyBoolean
                    |integer MyInt
                    |long MyLong
                    |byte MyByte
                    |short MyShort
                    |float MyFloat
                    |double MyDouble
                    |bigInteger MyBigInt
                    |bigDecimal MyBigDecimal
                    |blob MyBlob
                    |document MyDocument
                    |string MyString
                    |timestamp MyTimestamp
                    |
                    |structure Struct {
                    |  boolean: Boolean
                    |  int: Integer
                    |  long: Long
                    |  byte: Byte
                    |  short: Short
                    |  float: Float
                    |  double: Double
                    |  bigInteger: BigInteger
                    |  bigDecimal: BigDecimal
                    |  blob: Blob
                    |  document: Document
                    |  string: String
                    |  timestamp: Timestamp
                    |}
                    |""".stripMargin

    val expected = """|syntax = "proto3";
                      |
                      |package com.example;
                      |
                      |import "google/protobuf/struct.proto";
                      |
                      |import "google/protobuf/timestamp.proto";
                      |
                      |message Struct {
                      |  bool boolean = 1;
                      |  int32 int = 2;
                      |  int64 long = 3;
                      |  int32 byte = 4;
                      |  int32 short = 5;
                      |  float float = 6;
                      |  double double = 7;
                      |  string bigInteger = 8;
                      |  string bigDecimal = 9;
                      |  bytes blob = 10;
                      |  google.protobuf.Value document = 11;
                      |  string string = 12;
                      |  google.protobuf.Timestamp timestamp = 13;
                      |}
                      |""".stripMargin
    convertCheck(source, Map("com/example/example.proto" -> expected))
  }

  test("Primitives reference (wrapped)") {
    val source = """|namespace com.example
                    |
                    |use alloy.proto#protoWrapped
                    |
                    |@protoWrapped
                    |boolean MyBoolean
                    |@protoWrapped
                    |integer MyInt
                    |@protoWrapped
                    |long MyLong
                    |@protoWrapped
                    |byte MyByte
                    |@protoWrapped
                    |short MyShort
                    |@protoWrapped
                    |float MyFloat
                    |@protoWrapped
                    |double MyDouble
                    |@protoWrapped
                    |bigInteger MyBigInt
                    |@protoWrapped
                    |bigDecimal MyBigDecimal
                    |@protoWrapped
                    |blob MyBlob
                    |@protoWrapped
                    |document MyDocument
                    |@protoWrapped
                    |string MyString
                    |@protoWrapped
                    |timestamp MyTimestamp
                    |
                    |structure Struct {
                    |  boolean: MyBoolean
                    |  int: MyInt
                    |  long: MyLong
                    |  byte: MyByte
                    |  short: MyShort
                    |  float: MyFloat
                    |  double: MyDouble
                    |  bigInteger: MyBigInt
                    |  bigDecimal: MyBigDecimal
                    |  blob: MyBlob
                    |  document: MyDocument
                    |  string: MyString
                    |  timestamp: MyTimestamp
                    |}
                    |""".stripMargin

    val expected = """|syntax = "proto3";
                      |
                      |package com.example;
                      |
                      |import "google/protobuf/struct.proto";
                      |
                      |import "google/protobuf/timestamp.proto";
                      |
                      |message MyBoolean {
                      |  bool value = 1;
                      |}
                      |
                      |message MyInt {
                      |  int32 value = 1;
                      |}
                      |
                      |message MyLong {
                      |  int64 value = 1;
                      |}
                      |
                      |message MyByte {
                      |  int32 value = 1;
                      |}
                      |
                      |message MyShort {
                      |  int32 value = 1;
                      |}
                      |
                      |message MyFloat {
                      |  float value = 1;
                      |}
                      |
                      |message MyDouble {
                      |  double value = 1;
                      |}
                      |
                      |message MyBigInt {
                      |  string value = 1;
                      |}
                      |
                      |message MyBigDecimal {
                      |  string value = 1;
                      |}
                      |
                      |message MyBlob {
                      |  bytes value = 1;
                      |}
                      |
                      |message MyDocument {
                      |  google.protobuf.Value value = 1;
                      |}
                      |
                      |message MyString {
                      |  string value = 1;
                      |}
                      |
                      |message MyTimestamp {
                      |  google.protobuf.Timestamp value = 1;
                      |}
                      |
                      |message Struct {
                      |  com.example.MyBoolean boolean = 1;
                      |  com.example.MyInt int = 2;
                      |  com.example.MyLong long = 3;
                      |  com.example.MyByte byte = 4;
                      |  com.example.MyShort short = 5;
                      |  com.example.MyFloat float = 6;
                      |  com.example.MyDouble double = 7;
                      |  com.example.MyBigInt bigInteger = 8;
                      |  com.example.MyBigDecimal bigDecimal = 9;
                      |  com.example.MyBlob blob = 10;
                      |  com.example.MyDocument document = 11;
                      |  com.example.MyString string = 12;
                      |  com.example.MyTimestamp timestamp = 13;
                      |}
                      |""".stripMargin
    convertCheck(source, Map("com/example/example.proto" -> expected))
  }

  test("deprecated field") {
    val source = """|$version: "2"
                    |
                    |namespace another.namespace
                    |
                    |structure Struct {
                    |  @deprecated
                    |  value: String
                    |}
                    |""".stripMargin

    val expected = """|syntax = "proto3";
                      |
                      |package another.namespace;
                      |
                      |message Struct {
                      |  string value = 1 [deprecated = true];
                      |}
                      |""".stripMargin
    convertCheck(source, Map("another/namespace/namespace.proto" -> expected))
  }

  test("protoNumType") {
    val source = """|namespace com.example
                    |
                    |use alloy.proto#protoNumType
                    |use alloy.proto#protoWrapped
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
                    |    fixed_signed: Long
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
                    |    fixed_signed: Integer
                    |}
                    |
                    |structure WrappedLongNumbers {
                    |    @protoNumType("SIGNED")
                    |    @protoWrapped
                    |    signed: Long,
                    |
                    |    @protoNumType("UNSIGNED")
                    |    @protoWrapped
                    |    unsigned: Long,
                    |
                    |    @protoNumType("FIXED")
                    |    @protoWrapped
                    |    fixed: Long,
                    |
                    |    @protoNumType("FIXED_SIGNED")
                    |    @protoWrapped
                    |    fixed_signed: Long
                    |}
                    |
                    |structure WrappedIntNumbers {
                    |    @protoNumType("SIGNED")
                    |    @protoWrapped
                    |    signed: Integer,
                    |
                    |    @protoNumType("UNSIGNED")
                    |    @protoWrapped
                    |    unsigned: Integer,
                    |
                    |    @protoNumType("FIXED")
                    |    @protoWrapped
                    |    fixed: Integer,
                    |
                    |    @protoNumType("FIXED_SIGNED")
                    |    @protoWrapped
                    |    fixed_signed: Integer
                    |}
                    |""".stripMargin

    val expected = """|syntax = "proto3";
                      |
                      |package com.example;
                      |
                      |import "alloy/protobuf/wrappers.proto";
                      |
                      |import "google/protobuf/wrappers.proto";
                      |
                      |message LongNumbers {
                      |  sint64 signed = 1;
                      |  uint64 unsigned = 2;
                      |  fixed64 fixed = 3;
                      |  sfixed64 fixed_signed = 4;
                      |}
                      |
                      |message IntNumbers {
                      |  sint32 signed = 1;
                      |  uint32 unsigned = 2;
                      |  fixed32 fixed = 3;
                      |  sfixed32 fixed_signed = 4;
                      |}
                      |
                      |message WrappedLongNumbers {
                      |  alloy.protobuf.SInt64Value signed = 1;
                      |  google.protobuf.UInt64Value unsigned = 2;
                      |  alloy.protobuf.Fixed64Value fixed = 3;
                      |  alloy.protobuf.SFixed64Value fixed_signed = 4;
                      |}
                      |
                      |message WrappedIntNumbers {
                      |  alloy.protobuf.SInt32Value signed = 1;
                      |  google.protobuf.UInt32Value unsigned = 2;
                      |  alloy.protobuf.Fixed32Value fixed = 3;
                      |  alloy.protobuf.SFixed32Value fixed_signed = 4;
                      |}
                      |""".stripMargin
    convertCheck(
      source,
      Map("com/example/example.proto" -> expected)
    )
  }

  test("maps") {
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
    convertCheck(source, Map("com/example/example.proto" -> expected))
  }

  test("maps (bis)") {
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
    convertCheck(source, Map("com/example/example.proto" -> expected))
  }

  test("maps (wrapped)") {
    val source = """|namespace com.example
                    |
                    |use alloy.proto#protoWrapped
                    |
                    |@protoWrapped
                    |map StringMap {
                    |   key: String,
                    |   @protoWrapped
                    |   value: Integer
                    |}
                    |
                    |structure Foo {
                    |   strings: StringMap
                    |}
                    |""".stripMargin
    val expected =
      """|syntax = "proto3";
         |
         |package com.example;
         |
         |import "google/protobuf/wrappers.proto";
         |
         |message StringMap {
         |  map<string, google.protobuf.Int32Value> value = 1;
         |}
         |
         |message Foo {
         |  com.example.StringMap strings = 1;
         |}
         |""".stripMargin
    convertCheck(source, Map("com/example/example.proto" -> expected))
  }

  test("lists") {
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
    convertCheck(source, Map("com/example/example.proto" -> expected))
  }

  test("lists (bis)") {
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
    convertCheck(source, Map("com/example/example.proto" -> expected))

  }

  test("lists (wrapped)") {
    val source = """|namespace com.example
                    |
                    |use alloy.proto#protoWrapped
                    |
                    |@protoWrapped
                    |list StringList {
                    |   @protoWrapped
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
                      |message StringList {
                      |  repeated google.protobuf.StringValue value = 1;
                      |}
                      |
                      |message Foo {
                      |  com.example.StringList strings = 1;
                      |}
                      |""".stripMargin
    convertCheck(source, Map("com/example/example.proto" -> expected))
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
                      |message Test {
                      |  test.Other o = 1;
                      |}
                      |
                      |message Other {
                      |  string s = 1;
                      |}
                      |""".stripMargin
    convertCheck(
      source,
      Map("test/definitions.proto" -> expected),
      allShapes = false
    )

  }

  test("uuid translates to string by default") {
    val source = """|namespace test
                    |
                    |use alloy#uuidFormat
                    |
                    |@uuidFormat
                    |string MyUUID
                    |
                    |structure Test {
                    |  id: MyUUID
                    |}
                    |""".stripMargin

    val expected = """|syntax = "proto3";
                      |
                      |package test;
                      |
                      |message Test {
                      |  string id = 1;
                      |}""".stripMargin

    convertCheck(
      source,
      Map("test/definitions.proto" -> expected)
    )
  }

  test(
    "uuid translates to message by when annotated with @protoCompactUUID"
  ) {
    val source = """|namespace test
                    |
                    |use alloy#uuidFormat
                    |use alloy.proto#protoCompactUUID
                    |
                    |@uuidFormat
                    |@protoCompactUUID
                    |string MyUUID
                    |
                    |structure Test {
                    |  id: MyUUID
                    |}
                    |""".stripMargin
    val expected = """|syntax = "proto3";
                      |
                      |package test;
                      |
                      |import "alloy/protobuf/types.proto";
                      |
                      |message Test {
                      |  alloy.protobuf.CompactUUID id = 1;
                      |}""".stripMargin

    convertCheck(
      source,
      Map("test/definitions.proto" -> expected)
    )
  }

  test("do not render shapes used in trait definition") {
    val source = """|namespace test
                    |
                    |@trait()
                    |structure compat {
                    |  @required
                    |  mode: String
                    |}
                    |
                    |@compat(mode: "ignored")
                    |structure Test {
                    |  @required
                    |  s: String
                    |}
                    |""".stripMargin
    val expected = """|syntax = "proto3";
                      |
                      |package test;
                      |
                      |message Test {
                      |  string s = 1;
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
                      |service Test {
                      |  rpc Op(test.Struct) returns (test.Struct);
                      |}
                      |
                      |message Struct {
                      |  string s = 1;
                      |}
                      |""".stripMargin

    convertCheck(
      source,
      Map("test/definitions.proto" -> expected),
      allShapes = false
    )
  }

  test("enum without protoIndex") {
    val source = """|$version: "2"
                    |namespace test
                    |
                    |use alloy.proto#protoIndex
                    |
                    |enum LoveProto {
                    |  YES
                    |  NO
                    |}
                    |""".stripMargin
    val expected = """|syntax = "proto3";
                      |
                      |package test;
                      |
                      |enum LoveProto {
                      |  YES = 0;
                      |  NO = 1;
                      |}""".stripMargin

    convertCheck(
      source,
      Map("test/definitions.proto" -> expected)
    )
  }

  test("enum with protoIndex") {
    val source = """|$version: "2"
                    |namespace test
                    |
                    |use alloy.proto#protoIndex
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
                      |enum LoveProto {
                      |  YES = 0;
                      |  NO = 2;
                      |}""".stripMargin

    convertCheck(
      source,
      Map("test/definitions.proto" -> expected)
    )
  }

  test("intEnum without protoIndex") {
    val source = """|$version: "2"
                    |namespace test
                    |
                    |use alloy.proto#protoIndex
                    |
                    |intEnum LoveProto {
                    |  YES = 0
                    |  NO = 2
                    |}
                    |""".stripMargin
    val expected = """|syntax = "proto3";
                      |
                      |package test;
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

  test("intEnum with protoIndex") {
    val source = """|$version: "2"
                    |namespace test
                    |
                    |use alloy.proto#protoIndex
                    |
                    |intEnum LoveProto {
                    |  @protoIndex(0)
                    |  YES = 1
                    |  @protoIndex(2)
                    |  NO = 3
                    |}
                    |""".stripMargin
    val expected = """|syntax = "proto3";
                      |
                      |package test;
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

  test("open string enum") {
    val source = """|$version: "2"
                    |namespace test
                    |
                    |use alloy#openEnum
                    |
                    |structure EnumWrapper {
                    |  value: LoveProto
                    |}
                    |
                    |@openEnum
                    |enum LoveProto {
                    |  YES
                    |  NO
                    |}
                    |""".stripMargin
    val expected = """|syntax = "proto3";
                      |
                      |package test;
                      |
                      |message EnumWrapper {
                      |  string value = 1;
                      |}""".stripMargin

    convertCheck(
      source,
      Map("test/definitions.proto" -> expected)
    )
  }

  test("open intEnum") {
    val source = """|$version: "2"
                    |namespace test
                    |
                    |use alloy#openEnum
                    |
                    |structure EnumWrapper {
                    |  value: LoveProto
                    |}
                    |
                    |@openEnum
                    |intEnum LoveProto {
                    |  YES = 1
                    |  NO = 3
                    |}
                    |""".stripMargin
    val expected = """|syntax = "proto3";
                      |
                      |package test;
                      |
                      |message EnumWrapper {
                      |  int32 value = 1;
                      |}""".stripMargin

    convertCheck(
      source,
      Map("test/definitions.proto" -> expected)
    )
  }

  test("union with protoIndex") {
    val source = """|$version: "2"
                    |namespace test
                    |
                    |use alloy.proto#protoIndex
                    |
                    |union SomeUnion {
                    |  @protoIndex(2)
                    |  name: String
                    |  @protoIndex(3)
                    |  age: Integer
                    |}
                    |""".stripMargin
    val expected = """|syntax = "proto3";
                      |
                      |package test;
                      |
                      |message SomeUnion {
                      |  oneof definition {
                      |    string name = 2;
                      |    int32 age = 3;
                      |  }
                      |}
                      |""".stripMargin

    convertCheck(
      source,
      Map("test/definitions.proto" -> expected)
    )
  }

  test("union with @protoInlinedOneOf and @protoIndex (invalid)") {
    val source = """|$version: "2"
                    |namespace test
                    |
                    |use alloy.proto#protoIndex
                    |use alloy.proto#protoInlinedOneOf
                    |
                    |@protoInlinedOneOf
                    |union SomeUnion {
                    |  @protoIndex(2)
                    |  name: String
                    |  @protoIndex(3)
                    |  age: Integer
                    |}
                    |
                    |// because union use protoInlinedOneOf and have
                    |// @protoIndex on its field, this structure's member
                    |// should have `protoIndex` as well
                    |structure SomeStructure {
                    |  info: SomeUnion,
                    |  @required
                    |  otherValue: Integer
                    |}
                    |""".stripMargin

    // The error is thrown by the validator associated with the `protoInlinedOneOf`
    // annotation.
    intercept[ValidatedResultException](
      convertCheck(source, Map.empty)
    )
  }

  test("union with @protoInlinedOneOf and @protoIndex") {
    val source = """|$version: "2"
                    |namespace test
                    |
                    |use alloy.proto#protoIndex
                    |use alloy.proto#protoInlinedOneOf
                    |
                    |@protoInlinedOneOf
                    |union SomeUnion {
                    |  @protoIndex(2)
                    |  name: String
                    |  @protoIndex(3)
                    |  age: Integer
                    |}
                    |
                    |structure SomeStructure {
                    |  info: SomeUnion,
                    |  @required
                    |  @protoIndex(4)
                    |  otherValue: Integer
                    |}
                    |""".stripMargin

    val expected = """|syntax = "proto3";
                      |
                      |package test;
                      |
                      |message SomeStructure {
                      |  oneof info {
                      |    string name = 2;
                      |    int32 age = 3;
                      |  }
                      |  int32 otherValue = 4;
                      |}
                      |""".stripMargin

    convertCheck(source, Map("test/definitions.proto" -> expected))
  }

  test("both type of union can be used from a structure") {
    val source = """|$version: "2"
                    |namespace test
                    |
                    |use alloy.proto#protoIndex
                    |use alloy.proto#protoInlinedOneOf
                    |
                    |@protoInlinedOneOf
                    |union InlinedUnion {
                    |  @protoIndex(2)
                    |  name: String
                    |  @protoIndex(3)
                    |  age: Integer
                    |}
                    |
                    |union RequestData {
                    |  value: String
                    |  size: Integer
                    |}
                    |
                    |structure SomeStructure {
                    |  info: InlinedUnion,
                    |  @required
                    |  @protoIndex(4)
                    |  otherValue: RequestData
                    |}
                    |""".stripMargin

    val expected = """|syntax = "proto3";
                      |
                      |package test;
                      |
                      |message RequestData {
                      |  oneof definition {
                      |    string value = 1;
                      |    int32 size = 2;
                      |  }
                      |}
                      |
                      |message SomeStructure {
                      |  oneof info {
                      |    string name = 2;
                      |    int32 age = 3;
                      |  }
                      |  test.RequestData otherValue = 4;
                      |}
                      |""".stripMargin

    convertCheck(source, Map("test/definitions.proto" -> expected))
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
    convertCheck(
      source,
      Map("avoid/cyclic/in/namespace/namespace.proto" -> expected)
    )
  }

  test("multiple namespaces") {
    def src(ns: String) = s"""|namespace com.$ns
                              |
                              |structure Struct {
                              |  value: String
                              |}
                              |""".stripMargin
    def expected(ns: String) = s"""|syntax = "proto3";
                                   |
                                   |package com.$ns;
                                   |
                                   |message Struct {
                                   |  string value = 1;
                                   |}
                                   |""".stripMargin
    convertChecks(
      Map("ns1.smithy" -> src("ns1"), "ns2.smithy" -> src("ns2")),
      Map(
        "com/ns1/ns1.proto" -> expected("ns1"),
        "com/ns2/ns2.proto" -> expected("ns2")
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
                    |structure Struct {
                    |  value: String
                    |}
                    |""".stripMargin
    val expected = """|syntax = "proto3";
                      |
                      |option java_multiple_files = true;
                      |option java_package = "demo.hello";
                      |
                      |package another.namespace;
                      |
                      |message Struct {
                      |  string value = 1;
                      |}
                      |""".stripMargin
    convertCheck(source, Map("another/namespace/namespace.proto" -> expected))
  }

  private def convertCheck(
      source: String,
      expected: Map[String, String],
      allShapes: Boolean = true
  )(implicit loc: Location): Unit = {
    convertChecks(
      Map("inlined-in-test.smithy" -> source),
      expected,
      allShapes
    )
  }

  private def convertChecks(
      sources: Map[String, String],
      expected: Map[String, String],
      allShapes: Boolean = true
  )(implicit loc: Location): Unit = {
    def render(srcs: Map[String, String]): List[(String, String)] = {
      val m = {
        val assembler = Model
          .assembler()
          .discoverModels()

        srcs.foreach { case (name, src) =>
          assembler.addUnparsedModel(name, src)
        }

        assembler
          .assemble()
          .unwrap()
      }
      val c = new Compiler(m, allShapes = allShapes)
      val res = c.compile()
      if (res.isEmpty) { fail("Compiler didn't produce any output") }
      res.map { of =>
        val fileName = of.path.mkString("/")
        fileName -> Renderer.render(of.unit)
      }
    }

    val renderedFiles = render(sources).toMap

    // Checking that we get the same keyset as expected
    assertEquals(renderedFiles.keySet, expected.keySet)
    // Checking that all contents match
    for {
      (file, content) <- expected
    } {
      assertEquals(renderedFiles(file).trim(), content.trim())
    }
    ProtoValidator.run(renderedFiles.toSeq: _*)
  }

}
