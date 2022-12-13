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

object sample_specs {
  val sampleSpec =
    """|$version: "2"
      |
      |metadata "some_key" = "some value"
      |
      |namespace example.weather
      |
      |/// Provides weather forecasts.
      |@paginated(inputToken: "nextToken", outputToken: "nextToken",
      |           pageSize: "pageSize")
      |service Weather {
      |    version: "2006-03-01",
      |    resources: [City],
      |    operations: [GetCurrentTime]
      |}
      |
      |resource City {
      |    identifiers: { cityId: CityId },
      |    read: GetCity,
      |    list: ListCities,
      |    resources: [Forecast],
      |}
      |
      |resource Forecast {
      |    identifiers: { cityId: CityId },
      |    read: GetForecast,
      |}
      |
      |// "pattern" is a trait.
      |@pattern("^[A-Za-z0-9 ]+$")
      |string CityId
      |
      |@readonly
      |operation GetCity {
      |    input: GetCityInput,
      |    output: GetCityOutput,
      |    errors: [NoSuchResource]
      |}
      |
      |@input
      |structure GetCityInput {
      |    // "cityId" provides the identifier for the resource and
      |    // has to be marked as required.
      |    @required
      |    cityId: CityId
      |}
      |
      |@output
      |structure GetCityOutput {
      |    // "required" is used on output to indicate if the service
      |    // will always provide a value for the member.
      |    @required
      |    name: String,
      |
      |    @required
      |    coordinates: CityCoordinates,
      |}
      |
      |// This structure is nested within GetCityOutput.
      |structure CityCoordinates {
      |    @required
      |    latitude: Float,
      |
      |    @required
      |    longitude: Float,
      |}
      |
      |// "error" is a trait that is used to specialize
      |// a structure as an error.
      |@error("client")
      |structure NoSuchResource {
      |    @required
      |    resourceType: String
      |}
      |
      |// The paginated trait indicates that the operation may
      |// return truncated results.
      |@readonly
      |@paginated(items: "items")
      |operation ListCities {
      |    input: ListCitiesInput,
      |    output: ListCitiesOutput
      |}
      |
      |@input
      |structure ListCitiesInput {
      |    nextToken: String,
      |    pageSize: Integer
      |}
      |
      |@output
      |structure ListCitiesOutput {
      |    nextToken: String,
      |
      |    @required
      |    items: CitySummaries,
      |}
      |
      |// CitySummaries is a list of CitySummary structures.
      |list CitySummaries {
      |    member: CitySummary
      |}
      |
      |// CitySummary contains a reference to a City.
      |@references([{resource: City}])
      |structure CitySummary {
      |    @required
      |    cityId: CityId,
      |
      |    @required
      |    name: String,
      |}
      |
      |@readonly
      |operation GetCurrentTime {
      |    input: GetCurrentTimeInput,
      |    output: GetCurrentTimeOutput
      |}
      |
      |@input
      |structure GetCurrentTimeInput {}
      |
      |@output
      |structure GetCurrentTimeOutput {
      |    @required
      |    time: Timestamp
      |}
      |
      |@readonly
      |operation GetForecast {
      |    input: GetForecastInput,
      |    output: GetForecastOutput
      |}
      |
      |// "cityId" provides the only identifier for the resource since
      |// a Forecast doesn't have its own.
      |@input
      |structure GetForecastInput {
      |    @required
      |    cityId: CityId,
      |}
      |
      |@output
      |structure GetForecastOutput {
      |    chanceOfRain: Float
      |}
                 """.stripMargin
  val foospec =
    """namespace foo
      |
      |structure Bar {
      |  i : Integer
      |}
      |
      |@trait
      |structure foo {
      |  bar: Bar
      |}
      |
      |@foo(bar: {i: 1})
      |string MyString1
      |
      |string MyString2 """.stripMargin

  val traitOnlyRes =
    """namespace foo
      |
      |structure Bar {
      |  i : Integer
      |}
      |
      |structure foo {
      |  bar: Bar
      |}
      |""".stripMargin

  val noTraitSpec =
    """namespace foo
      |string MyString1
      |""".stripMargin

  val complexTrait =
    """
      |namespace example.city
      |
      |structure Latitude {
      | @required
      |    value: Float
      |}
      |structure Longitude {
      | @required
      |   value: Float
      |   }
      |@trait
      |structure CityCoordinates {
      |    @required
      |    latitude: Latitude,
      |
      |    @required
      |    longitude: Longitude,
      |}
      |string Test
      |
      |@CityCoordinates(latitude:{value:0.0},longitude:{value:0.0})
      |string location
      |   """.stripMargin

  val protocolSpec = """|namespace example.test
                        |
                        |@protocolDefinition(traits: [
                        |    someTrait
                        |])
                        |@trait(selector: "service")
                        |structure testProtocol {}
                        |
                        |@trait()
                        |structure someTrait {}
                        |
                        |@testProtocol
                        |service MyService {}
                        |""".stripMargin

  val idRefSpec = """|namespace example.test
                     |
                     |@trait(selector: "string")
                     |structure theTestTrait {
                     |  a: SomeUnion,
                     |  @idRef
                     |  b: String
                     |}
                     |
                     |union SomeUnion {
                     |  @idRef
                     |  branchA: String,
                     |  branchB: Integer,
                     |  branchC: Other
                     |}
                     |
                     |@idRef
                     |string Other
                     |
                     |@deprecated(message: "test")
                     |@trait()
                     |structure anotherTrait {
                     |  @idRef
                     |  id: String,
                     |  other: SomeNewType
                     |}
                     |
                     |string SomeNewType
                     |
                     |@anotherTrait(
                     |  id: RandomInt3
                     |)
                     |@trait()
                     |string someTrait
                     |
                     |@theTestTrait(
                     |  a: {
                     |    branchA: RandomInt
                     |  },
                     |  b: someTrait
                     |)
                     |string MyString
                     |
                     |@theTestTrait(
                     |  a: {
                     |    branchC: RandomInt2
                     |  },
                     |  b: someTrait
                     |)
                     |string MyString2
                     |
                     |integer RandomInt
                     |integer RandomInt2
                     |integer RandomInt3
                     |""".stripMargin

    val cycleSpec = """|namespace example.test
                       |
                       |@trait(selector: "structure :not([trait|error])")
                       |@idRef(failWhenMissing: true, selector: "union")
                       |string adtMember
                       |
                       |union SomeUnion {
                       |  optA: SomeOptionA,
                       |  optB: SomeOptionB,
                       |}
                       |
                       |@adtMember(SomeUnion)
                       |structure SomeOptionA {
                       |  a: String
                       |}
                       |
                       |@adtMember(SomeUnion)
                       |structure SomeOptionB {
                       |  b: String
                       |}
                       |""".stripMargin
}
