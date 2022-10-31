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
import smithyproto.proto3.ProtoIR._
import smithyproto.proto3.ProtoIR.Statement._
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.StringShape

class CompilerSuite extends FunSuite {
  test("compile a simple smithy model") {
    val namespace = "com.example"
    val sut = new Compiler()
    val model = {
      val mb = Model.builder()
      mb.addShape(
        StringShape.builder().id(s"$namespace#SomeString").build()
      )
      mb.build()
    }
    val actual = sut.compile(model)
    val expected = List(
      OutputFile(
        List(
          "com",
          "example",
          "some_string.proto"
        ),
        CompilationUnit(
          Some(
            "com.example"
          ),
          List(
            TopLevelStatement(
              TopLevelDef.MessageDef(
                Message(
                  "SomeString",
                  List(
                    MessageElement.FieldElement(
                      Field(
                        false,
                        Type.String,
                        "value",
                        1
                      )
                    )
                  ),
                  Nil
                )
              )
            )
          )
        )
      )
    )

    Assertions.assertEquals(actual, expected)
  }

}
