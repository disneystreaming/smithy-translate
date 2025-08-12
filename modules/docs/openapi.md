# OpenAPI

### CLI Usage

The `smithytranslate` CLI will recursively go through all child directories of the
input directory provided and convert any openapi files ending with an extension of `yaml`,
`yml`, or `json`.

```
> smithytranslate openapi-to-smithy --help

Usage: smithytranslate openapi-to-smithy --input <path> [--input <path>]... [--verboseNames] [--failOnValidationErrors] [--useEnumTraitSyntax] [--outputJson] <directory>

Take Open API specs as input and produce Smithy files as output.

Options and flags:
    --help
        Display this help text.
    --input <path>, -i <path>
        input source files
    --verbose-names
        If set, names of shapes not be simplified and will be as verbose as possible
    --validate-input
        If set, abort the conversion if any input specs contains a validation error
    --validate-output
        If set, abort the conversion if any produced smithy spec contains a validation error
    --enum-trait-syntax
        output enum types with the smithy v1 enum trait (deprecated) syntax
    --json-output
        changes output format to be json representations of the smithy models
```

Run `smithytranslate openapi-to-smithy --help` for more usage information.

### Capabilities and Design

Because Smithy is a more constrained format than OpenAPI, this conversion is _partial_.
This means that a best effort is made to translate all possible aspects of OpenAPI into
Smithy and errors are outputted when something cannot be translated. When errors are
encountered, the conversion still makes a best effort at converting everything else.
This way, as much of the specification will be translated automatically and the user
can decide how to translate the rest.

OpenAPI 2.x and 3.x are supported as input formats to this converter.

Below are examples of how Smithy Translate converts various OpenAPI constructs into
Smithy.

#### Primitives

| OpenAPI Base Type | OpenAPI Format     | Smithy Shape         | Smithy Trait(s)               |
| ----------------- | ------------------ | -------------------- | ----------------------------- |
| string            |                    | String               |                               |
| string            | timestamp          | Timestamp            |                               |
| string            | date-time          | Timestamp            | @timestampFormat("date-time") |
| string            | date               | String               | alloy#dateFormat              |
| string            | local-date         | alloy#LocalDate      | alloy#localDateFormat         |
| string            | local-time         | alloy#LocalTime      | alloy#localTimeFormat         |
| string            | local-date-time    | alloy#LocalDateTime  | alloy#localDateTimeFormat     |
| string            | offset-date-time   | alloy#OffsetDateTime | alloy#offsetDateTimeFormat    |
| string            | offset-time        | alloy#OffsetTime     | alloy#offsetTimeFormat        |
| string            | zone-id            | alloy#ZoneId         | alloy#zoneIdFormat            |
| string            | zone-offset        | alloy#ZoneOffset     | alloy#zoneOffsetFormat        |
| string            | zoned-date-time    | alloy#ZonedDateTime  | alloy#zonedDateTimeFormat     |
| integer           | year               | alloy#Year           | alloy#yearFormat              |
| string            | year-month         | alloy#YearMonth      | alloy#yearMonthFormat         |
| string            | month-day          | alloy#MonthDay       | alloy#monthDayFormat          |
| string            | uuid               | alloy#UUID           |                               |
| string            | binary             | Blob                 |                               |
| string            | byte               | Blob                 |                               |
| string            | password           | String               | @sensitive                    |
| number            | duration           | Duration             | alloy#durationSecondsFormat   |
| number            | float              | Float                |                               |
| number            | double             | Double               |                               |
| number            | double             | Double               |                               |
| number            |                    | Double               |                               |
| integer           | int16              | Short                |                               |
| integer           |                    | Integer              |                               |
| integer           | int32              | Integer              |                               |
| integer           | int64              | Long                 |                               |
| boolean           |                    | Boolean              |                               |
| object            | (empty properties) | Document             |                               |

##### Time types

Time types are treated in two different ways. If the definition of the schema is just a primitive with a `format` or `x-format` defined
then the resulting smithy type will be a newtype. If the definition is within an aggregate shape then the smithy type will be the default
alloy time type.


OpenAPI:
```yaml
openapi: '3.0.'
info:
  title: test
  version: '1.0'
paths: {}
components:
  schemas:
    MyTimestamp:
      type: string
      format: date-time
    MyLocalDate:
      type: string
      format: local-date
    MyLocalTime:
      type: string
      format: local-time
    MyLocalDateTime:
      type: string
      format: local-date-time
    MyOffsetDateTime:
      type: string
      format: "offset-date-time"
    MyOffsetTime:
      type: string
      format: "offset-time"
    MyZoneId:
      type: string
      format: "zone-id"
    MyZoneOffset:
      type: string
      format: "zone-offset"
    MyZonedDateTime:
      type: string
      format: "zoned-date-time"
    MyYear:
      type: integer
      format: "year"
    MyYearMonth:
      type: string
      format: "year-month"
    MyMonthDay:
      type: string
      format: "month-day"
    MyDuration:
      type: number
      format: "duration"
    MyObj:
      type: object
      properties:
        myTimestamp:
          $ref: '#/components/schemas/MyTimestamp'
        myLocalDate:
          $ref: '#/components/schemas/MyLocalDate'
        localDate:
          type: string
          format: local-date
        localTime:
          type: string
          format: local-time
        localDateTime:
          type: string
          format: local-date-time
        offsetDateTime:
          type: string
          format: offset-date-time
        offsetTime:
          type: string
          format: offset-time
        zoneId:
          type: string
          format: zone-id
        zoneOffset:
          type: string
          format: zone-offset
        zonedDateTime:
          type: string
          format: zoned-date-time
        year:
          type: integer
          format: year
        yearMonth:
          type: string
          format: year-month
        monthDay:
          type: string
          format: month-day
        duration:
          type: number
          format: duration
```

Smithy:
```smithy
use alloy#dateFormat
use alloy#LocalDate
use alloy#LocalDateTime
use alloy#localDateTimeFormat
use alloy#LocalTime
use alloy#localTimeFormat
use alloy#MonthDay
use alloy#monthDayFormat
use alloy#OffsetDateTime
use alloy#offsetDateTimeFormat
use alloy#OffsetTime
use alloy#offsetTimeFormat
use alloy#Year
use alloy#yearFormat
use alloy#YearMonth
use alloy#yearMonthFormat
use alloy#ZonedDateTime
use alloy#zonedDateTimeFormat
use alloy#ZoneId
use alloy#zoneIdFormat
use alloy#ZoneOffset
use alloy#zoneOffsetFormat
use alloy#Duration
use alloy#durationSecondsFormat

structure MyObj {
    myTimestamp: MyTimestamp
    myLocalDate: MyLocalDate
    localDate: LocalDate
    localTime: LocalTime
    localDateTime: LocalDateTime
    offsetDateTime: OffsetDateTime
    offsetTime: OffsetTime
    zoneId: ZoneId
    zoneOffset: ZoneOffset
    zonedDateTime: ZonedDateTime
    year: Year
    yearMonth: YearMonth
    monthDay: MonthDay
    duration: Duration
}

@dateFormat
string MyLocalDate

@localDateTimeFormat
string MyLocalDateTime

@localTimeFormat
string MyLocalTime

@monthDayFormat
string MyMonthDay

@offsetDateTimeFormat
@timestampFormat("date-time")
timestamp MyOffsetDateTime

@offsetTimeFormat
string MyOffsetTime

@timestampFormat("date-time")
timestamp MyTimestamp

@yearFormat
integer MyYear

@yearMonthFormat
string MyYearMonth

@zonedDateTimeFormat
string MyZonedDateTime

@zoneIdFormat
string MyZoneId

@zoneOffsetFormat
string MyZoneOffset

@durationSecondsFormat
bigDecimal MyDuration
```

#### Aggregate Shapes

##### Structure

OpenAPI:
```yaml
openapi: '3.0.'
info:
  title: doc
  version: 1.0.0
paths: {}
components:
  schemas:
    Testing:
      type: object
      properties:
        myString:
          type: string
        my_int:
          type: integer
      required:
        - myString
```

Smithy:
```smithy
structure Testing {
 @required
 myString: String
 my_int: Integer
}
```

Required properties and nested structures are both supported.

Any properties in the input structure that begin with a number will be prefixed by the letter `n`. This is because smithy does not allow for member names to begin with a number. You can change this with post-processing if you want a different change to be made to names of this nature. Note that this extra `n` will not impact JSON encoding/decoding because we also attach the [JsonName Smithy trait](https://awslabs.github.io/smithy/2.0/spec/protocol-traits.html#jsonname-trait) to these properties. The same thing happens if the member name contains a hyphen. In this case, hyphens are replaced with underscores and a `jsonName` trait is once again added. Note that if the field is a header or query parameter, the `jsonName` annotation is not added since `httpHeader` or `httpQuery` is used instead.

OpenAPI:
```yaml
openapi: '3.0.'
info:
  title: doc
  version: 1.0.0
paths: {}
components:
  schemas:
    Testing:
      type: object
      properties:
        12_twelve:
          type: string
        X-something:
          type: string
```

Smithy:
```smithy
structure Testing {
 @jsonName("12_twelve")
 n12_twelve: String
 @jsonName("X-something")
 X_something: String
}
```

##### Structures with Mixins

Smithy Translate will convert allOfs from OpenAPI into structures with mixins in smithy where possible. AllOfs in OpenAPI have references to other types which compose the current type. We refer to these as "parents" or "parent types" below. There are three possibilities when converting allOfs to smithy shapes:

1. The parent structures are only ever used as mixins

OpenAPI:
```yaml
openapi: '3.0.'
info:
  title: doc
  version: 1.0.0
paths: {}
components:
  schemas:
    One:
      type: object
      properties:
        one:
          type: string
    Two:
      type: object
      properties:
        two:
          type: string
    Three:
      type: object
      allOf:
        - $ref: "#/components/schemas/One"
        - $ref: "#/components/schemas/Two"
```

Smithy:
```smithy
@mixin
structure One {
 one: String
}

@mixin
structure Two {
  two: String
}

structure Three with [One, Two] {}
```

Here we can see that both parents, `One` and `Two` are converted into mixins and used as such on `Three`.

2. The parents structures are used as mixins and referenced as member targets

OpenAPI:
```yaml
openapi: '3.0.'
info:
  title: doc
  version: 1.0.0
paths: {}
components:
  schemas:
    One:
      type: object
      properties:
        one:
          type: string
    Two:
      type: object
      allOf:
        - $ref: "#/components/schemas/One"
    Three:
      type: object
      properties:
        one:
          $ref: "#/components/schemas/One"
```

Smithy:
```smithy
@mixin
structure OneMixin {
  one: String
}

structure One with [OneMixin] {}

structure Two with [OneMixin] {}

structure Three {
  one: One
}
```

Here `One` is used as a target of the `Three$one` member and is used as a mixin in the `Two` structure. Since smithy does not allow mixins to be used as targets, we have to create a separate mixin shape, `OneMixin` which is used as a mixin for `One` which is ultimately what we use for the target in `Three`.

3. One of the parents is a document rather than a structure

OpenAPI:
```yaml
openapi: '3.0.'
info:
  title: doc
  version: 1.0.0
paths: {}
components:
  schemas:
    One:
      type: object
      properties: {}
    Two:
      type: object
      properties:
        two:
          type: string
    Three:
      type: object
      allOf:
        - $ref: "#/components/schemas/One"
        - $ref: "#/components/schemas/Two"
```

Smithy:
```smithy
document One

structure Two {
  two: String
}

document Three
```

In this case, no mixins are created since none are ultimately used. Since `One` is translated to a document, `Three` must also be a document since it has `One` as a parent shape. As such, `Two` is never used as a mixin.

##### Untagged Union

The majority of `oneOf` schemas in OpenAPI represent untagged unions.
As such, they will be tagged with the `alloy#untagged`
trait. There are two exceptions to this: tagged unions and discriminated
unions, shown in later examples.

OpenAPI:
```yaml
openapi: '3.0.'
info:
  title: doc
  version: 1.0.0
paths: {}
components:
  schemas:
    Cat:
      type: object
      properties:
        name:
          type: string
    Dog:
      type: object
      properties:
        breed:
          type: string
    TestUnion:
      oneOf:
        - $ref: '#/components/schemas/Cat'
        - $ref: '#/components/schemas/Dog'
```

Smithy:
```smithy
use alloy#untagged

structure Cat {
    name: String
}

structure Dog {
    breed: String
}

@untagged
union TestUnion {
    Cat: Cat,
    Dog: Dog
}
```

##### Tagged Union

Smithy Translate will convert a `oneOf` to a tagged union IF
each of the branches of the `oneOf` targets a structure where
each of those structures contains a single required property.
Note that unions in smithy are tagged by default, so there is
no trait annotation required here.

OpenAPI:
```yaml
openapi: '3.0.'
info:
  title: doc
  version: 1.0.0
paths: {}
components:
  schemas:
    Number:
      type: object
      properties:
        num:
          type: integer
      required:
        - num
    Text:
      type: object
      properties:
        txt:
          type: string
      required:
        - txt
    TestUnion:
      oneOf:
        - $ref: '#/components/schemas/Number'
        - $ref: '#/components/schemas/Text'
```

Smithy:
```smithy
structure Number {
    @required
    num: Integer,
}

structure Text {
    @required
    txt: String,
}

union TestUnion {
    num: Integer,
    txt: String
}
```

Although `TestUnion` is a tagged union that can be represented by directly
targeting the `Integer` and `String` types, `Text` and `Number` are still rendered.
This is because they are top-level schemas and could be used elsewhere.

##### Discriminated Union

A `oneOf` will be converted to a discriminated union IF it
contains the `discriminator` field. Discriminated unions in
Smithy will be denoted using the `alloy#discriminated`
trait. The discriminated trait contains the name of the property
that is used as the discriminator.

OpenAPI:
```yaml
openapi: '3.0.'
info:
  title: doc
  version: 1.0.0
paths: {}
components:
  schemas:
    Cat:
      type: object
      properties:
        name:
          type: string
        pet_type:
          type: string
    Dog:
      type: object
      properties:
        breed:
          type: string
        pet_type:
          type: string
    TestUnion:
      oneOf:
        - $ref: '#/components/schemas/Cat'
        - $ref: '#/components/schemas/Dog'
      discriminator:
        propertyName: pet_type
```

Smithy:
```smithy
use alloy#discriminated

structure Cat {
    name: String,
}

structure Dog {
    breed: String,
}

@discriminated("pet_type")
union TestUnion {
    Cat: Cat,
    Dog: Dog
}
```

##### List

OpenAPI:
```yaml
openapi: '3.0.'
info:
  title: doc
  version: 1.0.0
paths: {}
components:
  schemas:
    StringArray:
      type: array
      items:
        type: string
```

Smithy:
```smithy
list StringArray {
    member: String
}
```

##### Set

OpenAPI:
```yaml
openapi: '3.0.'
info:
  title: doc
  version: 1.0.0
paths: {}
components:
  schemas:
    StringSet:
      type: array
      items:
        type: string
      uniqueItems: true
```

Smithy:
```smithy
@uniqueItems
list StringSet {
    member: String
}
```

##### Map

OpenAPI:
```yaml
openapi: '3.0.'
info:
  title: doc
  version: 1.0.0
paths: {}
components:
  schemas:
    StringStringMap:
      type: object
      additionalProperties:
        type: string
```

Smithy:
```smithy
map StringStringMap {
    key: String,
    value: String
}
```

#### Constraints

##### Enum

Enums can be translated to either Smithy V1 or V2 syntax. You can control this using the `useEnumTraitSyntax` CLI flag.

OpenAPI:
```yaml
openapi: '3.0.'
info:
  title: doc
  version: 1.0.0
paths: {}
components:
  schemas:
    Color:
      type: string
      enum:
        - red
        - green
        - blue
```

Smithy:
```smithy
enum Color {
  red
  green
  blue
}
```

Or if using the `useEnumTraitSyntax` flag:

```smithy
@enum([
 {value: "red"},
 {value: "green"},
 {value: "blue"}
])
string Color
```

##### Pattern

OpenAPI:
```yaml
openapi: '3.0.'
info:
  title: doc
  version: 1.0.0
paths: {}
components:
  schemas:
    MyString:
      type: string
      pattern: '^\d{3}-\d{2}-\d{4}$'
```

Smithy:
```smithy
@pattern("^\\d{3}-\\d{2}-\\d{4}$")
string MyString
```

_Note that `length`, `range`, and `sensitive` traits are also supported,
as indicated in the primitives table above._

#### Service Shapes

##### Basic Service

OpenAPI:
```yaml
openapi: '3.0.'
info:
  title: doc
  version: 1.0.0
paths:
  /test:
    post:
      operationId: testOperationId
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ObjectIn'
      responses:
        '200':
          description: test
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ObjectOut'
components:
  schemas:
    ObjectIn:
      type: object
      properties:
        s:
          type: string
      required:
        - s
    ObjectOut:
      type: object
      properties:
        sNum:
          type: integer
```

If provided, such as above, the `operationId` will be used
to inform the naming of the operation and the various shapes it
contains.

Smithy:
```smithy
use smithytranslate#contentType

service FooService {
    operations: [TestOperationId]
}

@http(method: "POST", uri: "/test", code: 200)
operation TestOperationId {
    input: TestOperationIdInput,
    output: TestOperationId200
}

structure ObjectIn {
    @required
    s: String
}

structure ObjectOut {
    sNum: Integer
}

structure TestOperationId200 {
    @httpPayload
    @required
    @contentType("application/json")
    body: ObjectOut
}

structure TestOperationIdInput {
    @httpPayload
    @required
    @contentType("application/json")
    body: ObjectIn
}
```

##### Service with Error Responses

OpenAPI:
```yaml
openapi: '3.0.'
info:
  title: doc
  version: 1.0.0
paths:
  /test:
    get:
      operationId: testOperationId
      responses:
        '200':
          description: test
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Object'
        '404':
          description: test
          content:
            application/json:
              schema:
                type: object
                properties:
                  message:
                    type: string
components:
  schemas:
    Object:
      type: object
      properties:
        s:
          type: string
      required:
        - s
```

Smithy:
```smithy
use smithytranslate#contentType

service FooService {
    operations: [TestOperationId]
}

@http(method: "GET", uri: "/test", code: 200)
operation TestOperationId {
    input: Unit,
    output: TestOperationId200,
    errors: [TestOperationId404]
}

structure Object {
    @required
    s: String
}

@error("client")
@httpError(404)
structure TestOperationId404 {
    @httpPayload
    @required
    @contentType("application/json")
    body: Body
}

structure Body {
    message: String
}

structure TestOperationId200 {
    @httpPayload
    @required
    @contentType("application/json")
    body: Object
}
```

##### Operation with headers

OpenAPI:
```yaml
openapi: '3.0.'
info:
  title: doc
  version: 1.0.0
paths:
  /test:
    get:
      operationId: testOperationId
      parameters:
        - in: header
          name: X-username
          schema:
            type: string
      responses:
        '200':
          description: test
          headers:
            X-RateLimit-Limit:
              schema:
                type: integer
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Object'
components:
  schemas:
    Object:
      type: object
      properties:
        s:
          type: string
      required:
        - s
```

Smithy:
```smithy
use smithytranslate#contentType

service FooService {
    operations: [TestOperationId]
}

@http(method: "GET", uri: "/test", code: 200)
operation TestOperationId {
    input: TestOperationIdInput,
    output: TestOperationId200
}

structure TestOperationIdInput {
    @httpHeader("X-username")
    X_username: String
}

structure Object {
    @required
    s: String,
}

structure TestOperationId200 {
    @httpPayload
    @required
    @contentType("application/json")
    body: Object,
    @httpHeader("X-RateLimit-Limit")
    X_RateLimit_Limit: Integer
}
```

##### Operation with multiple content types

Operations in OpenAPI may contain more than one
content type. This is represented in smithy using a
`union` with a special `contentTypeDiscriminated` trait.
This trait indicates that the members of the union are
discriminated from one another using the `Content-Type`
header. Each member of the union is annotated with the `contentType`
trait. This trait indicates which content type refers to each
specific branch of the union.

OpenAPI:
```yaml
openapi: '3.0.'
info:
  title: doc
  version: 1.0.0
paths:
  /test:
    post:
      operationId: testOperationId
      requestBody:
        required: true
        content:
          application/octet-stream:
            schema:
              type: string
              format: binary
          application/json:
            schema:
              type: object
              properties:
                s:
                  type: string
      responses:
        '200':
          description: test
          content:
            application/octet-stream:
              schema:
                type: string
                format: binary
            application/json:
              schema:
                type: object
                properties:
                  s:
                    type: string
```

Smithy:
```smithy
use smithytranslate#contentTypeDiscriminated
use smithytranslate#contentType

service FooService {
    operations: [TestOperationId]
}

@http(method: "POST", uri: "/test", code: 200)
operation TestOperationId {
    input: TestOperationIdInput,
    output: TestOperationId200
}

structure TestOperationIdInput {
    @httpPayload
    @required
    body: TestOperationIdInputBody
}

structure TestOperationId200 {
    @httpPayload
    @required
    body: TestOperationId200Body
}

@contentTypeDiscriminated
union TestOperationId200Body {
    @contentType("application/octet-stream")
    applicationOctetStream: Blob,
    @contentType("application/json")
    applicationJson: TestOperationId200BodyApplicationJson
}

structure TestOperationId200BodyApplicationJson {
    s: String
}

@contentTypeDiscriminated
union TestOperationIdInputBody {
  @contentType("application/octet-stream")
  applicationOctetStream: Blob,
  @contentType("application/json")
  applicationJson: TestOperationIdInputBodyApplicationJson
}

structure TestOperationIdInputBodyApplicationJson {
  s: String
}
```

#### Extensions

[OpenAPI extensions](https://swagger.io/docs/specification/openapi-extensions/) are preserved in the output
Smithy model through the use of the `openapiExtensions` trait.

OpenAPI:
```yaml
openapi: '3.0.'
info:
  title: test
  version: '1.0'
paths: {}
components:
  schemas:
    MyString:
      type: string
      x-float: 1.0
      x-string: foo
      x-int: 1
      x-array: [1, 2, 3]
      x-obj:
        a: 1
        b: 2
```

Smithy:
```smithy
use alloy.openapi#openapiExtensions

@openapiExtensions(
 "x-float": 1.0,
 "x-array": [1, 2, 3],
 "x-string": "foo",
 "x-int": 1,
 "x-obj": {
   a: 1,
   b: 2
 }
)
string MyString
```
