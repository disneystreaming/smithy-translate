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

package smithytranslate.proto3.internals

import munit.FunSuite

class RendererSuite extends FunSuite {

  import ProtoIR._

  test("render message") {
    val unit = CompilationUnit(
      Some("com.example"),
      List(
        Statement.TopLevelStatement(
          TopLevelDef.MessageDef(
            Message(
              "Foo",
              List(
                MessageElement.FieldElement(
                  Field(
                    deprecated = false,
                    Type.Int32,
                    "a",
                    1
                  )
                ),
                MessageElement.FieldElement(
                  Field(
                    deprecated = false,
                    Type.ListType(Type.String),
                    "b",
                    2
                  )
                )
              ),
              Nil
            )
          )
        )
      ),
      List.empty
    )

    val result = Renderer.render(unit)
    val expected =
      s"""|syntax = "proto3";
          |
          |package com.example;
          |
          |message Foo {
          |  int32 a = 1;
          |  repeated string b = 2;
          |}
          |""".stripMargin

    assertEquals(result, expected)
  }

  test("render reserved") {
    val node = Message(
      "Foo",
      List(
        MessageElement.FieldElement(
          Field(deprecated = false, Type.Int32, "a", 1)
        ),
        MessageElement.FieldElement(
          Field(deprecated = false, Type.ListType(Type.String), "b", 2)
        )
      ),
      List(
        Reserved.Number(3),
        Reserved.Range(5, 8),
        Reserved.Name("c"),
        Reserved.Name("d")
      )
    )

    val result = Text.renderText(Renderer.renderMessage(node))
    val expected =
      s"""|message Foo {
          |  reserved 3, 5 to 8;
          |  reserved "c", "d";
          |  int32 a = 1;
          |  repeated string b = 2;
          |}""".stripMargin

    assertEquals(result, expected)
  }

  test("render reserved in enum") {
    val node = Enum(
      "SomeEnum",
      List(
        EnumValue("V1", 1)
      ),
      List(
        Reserved.Number(3),
        Reserved.Range(5, 8),
        Reserved.Name("c"),
        Reserved.Name("d")
      )
    )

    val result = Text.renderText(Renderer.renderEnum(node))
    val expected =
      s"""|enum SomeEnum {
          |  reserved 3, 5 to 8;
          |  reserved "c", "d";
          |  V1 = 1;
          |}""".stripMargin

    assertEquals(result, expected)
  }

  test("render oneof") {
    val unit = CompilationUnit(
      Some("com.example"),
      List(
        Statement.TopLevelStatement(
          TopLevelDef.MessageDef(
            Message(
              "Foo",
              List(
                MessageElement.OneofElement(
                  Oneof(
                    "foo_oneof",
                    List(
                      Field(
                        deprecated = false,
                        Type.Int32,
                        "a",
                        1
                      ),
                      Field(
                        deprecated = true,
                        Type.ListType(Type.String),
                        "b",
                        2
                      )
                    )
                  )
                )
              ),
              Nil
            )
          )
        )
      ),
      List.empty
    )

    val result = Renderer.render(unit)
    val expected =
      s"""|syntax = "proto3";
          |
          |package com.example;
          |
          |message Foo {
          |  oneof foo_oneof {
          |    int32 a = 1;
          |    repeated string b = 2 [deprecated = true];
          |  }
          |}
          |""".stripMargin

    assertEquals(result, expected)
  }

  test("render enum") {
    val unit = CompilationUnit(
      Some("com.example"),
      List(
        Statement.TopLevelStatement(
          TopLevelDef.EnumDef(
            Enum(
              "TopLevelEnum",
              List(
                EnumValue("FALSE", 0),
                EnumValue("TRUE", 1)
              ),
              List(
                Reserved.Name("c"),
                Reserved.Name("d")
              )
            )
          )
        ),
        Statement.TopLevelStatement(
          TopLevelDef.MessageDef(
            Message(
              "Foo",
              List(
                MessageElement.EnumDefElement(
                  Enum(
                    "Corpus",
                    List(
                      EnumValue("UNIVERSAL", 0),
                      EnumValue("WEB", 1),
                      EnumValue("VIDEO", 2)
                    ),
                    List(
                      Reserved.Number(3),
                      Reserved.Range(5, 8)
                    )
                  )
                )
              ),
              Nil
            )
          )
        )
      ),
      List.empty
    )

    val result = Renderer.render(unit)
    val expected =
      s"""|syntax = "proto3";
          |
          |package com.example;
          |
          |enum TopLevelEnum {
          |  reserved "c", "d";
          |  FALSE = 0;
          |  TRUE = 1;
          |}
          |
          |message Foo {
          |  enum Corpus {
          |    reserved 3, 5 to 8;
          |    UNIVERSAL = 0;
          |    WEB = 1;
          |    VIDEO = 2;
          |  }
          |}
          |""".stripMargin

    assertEquals(result, expected)
  }

}
