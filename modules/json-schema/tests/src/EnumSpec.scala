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

package smithytranslate.json_schema

import smithytranslate.openapi.OpenApiCompiler.SmithyVersion

final class EnumSpec extends munit.FunSuite {

  test("enums") {
    val jsonSchString = """|{
                           |  "$id": "test.json",
                           |  "$schema": "http://json-schema.org/draft-07/schema#",
                           |  "title": "TestIt",
                           |  "type": "object",
                           |  "properties": {
                           |    "someValue": {
                           |      "enum": ["ONE", "TWO", "THREE"]
                           |    }
                           |  }
                           |}
                           |""".stripMargin

    val expectedString = """|namespace foo
                            |
                            |@enum([
                            |    {
                            |        value: "ONE"
                            |    },
                            |    {
                            |        value: "TWO"
                            |    },
                            |    {
                            |        value: "THREE"
                            |    }
                            |])
                            |string SomeValue
                            |
                            |structure TestIt {
                            | someValue: SomeValue
                            |}
                            |""".stripMargin

    TestUtils.runConversionTest(
      jsonSchString,
      expectedString,
      smithyVersion = SmithyVersion.One
    )
  }

}
