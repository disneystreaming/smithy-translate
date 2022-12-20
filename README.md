<!-- Using `yzhang.markdown-all-in-one` VS Code extension to create the table of contents -->
# Smithy Translate <!-- omit in toc -->

Tooling that enables converting to and from Smithy.

_Note: this library is published to work on Java 8 and above. However, you will need to use Java 11 or above to work on the library as a contributor. This is due to some of the build flags that we use._

## Table of Contents <!-- omit in toc -->

- [Alloy](#alloy)
- [CLI](#cli)
  - [Installation](#installation)
- [OpenAPI](#openapi)
  - [CLI Usage](#cli-usage)
  - [Capabilities and Design](#capabilities-and-design)
    - [Primitives](#primitives)
    - [Aggregate Shapes](#aggregate-shapes)
      - [Structure](#structure)
      - [Untagged Union](#untagged-union)
      - [Tagged Union](#tagged-union)
      - [Discriminated Union](#discriminated-union)
      - [List](#list)
      - [Set](#set)
      - [Map](#map)
    - [Constraints](#constraints)
      - [Enum](#enum)
      - [Pattern](#pattern)
    - [Service Shapes](#service-shapes)
      - [Basic Service](#basic-service)
      - [Service with Error Responses](#service-with-error-responses)
      - [Operation with headers](#operation-with-headers)
      - [Operation with multiple content types](#operation-with-multiple-content-types)
    - [Extensions](#extensions)
- [JSON Schema](#json-schema)
  - [CLI Usage](#cli-usage-1)
  - [Differences from OpenAPI](#differences-from-openapi)
    - [Default Values](#default-values)
    - [Null Values](#null-values)
    - [Maps](#maps)
- [Protobuf](#protobuf)
  - [CLI Usage](#cli-usage-2)
  - [Capabilities and Design](#capabilities-and-design-1)
    - [Primitives](#primitives-1)
    - [Aggregate Types](#aggregate-types)
      - [Structure](#structure-1)
      - [Union](#union)
      - [List](#list-1)
      - [Map](#map-1)
    - [Constraints](#constraints-1)
      - [Enum](#enum-1)
    - [Service Shapes](#service-shapes-1)
      - [Basic Service](#basic-service-1)
  - [Options](#options)
    - [Stringly typed options](#stringly-typed-options)
    - [Example](#example)
- [formatter](#formatter)
  - [CLI Usage](#cli-usage-3)
  - [Capabilities and Design](#capabilities-and-design-2)


## Alloy

Throughout smithy-translate you will see references to [alloy](https://github.com/disneystreaming/alloy). Alloy is a lightweight library that houses some common smithy shapes that are used across our open source projects such as [smithy4s](https://github.com/disneystreaming/smithy4s). This is to provide better interoperability between our tools at a lower cost to end users.

## CLI

### Installation

You will need to [install coursier](https://get-coursier.io/docs/cli-installation), an artifact fetching library, in order to install the CLI.

```
coursier install --channel https://disneystreaming.github.io/coursier.json smithytranslate
```

Run `smithytranslate --help` for usage information.

## OpenAPI

### CLI Usage

The `smithytranslate` CLI will recursively go through all child directories of the
input directory provided and convert any openapi files ending with an extension of `yaml`,
`yml`, or `json`.

```
> smithytranslate openapi-to-smithy --help

Usage: smithy-translate openapi-to-smithy --input <path> [--input <path>]... [--verboseNames] [--failOnValidationErrors] [--useEnumTraitSyntax] [--outputJson] <directory>

Take Open API specs as input and produce Smithy files as output.

Options and flags:
    --help
        Display this help text.
    --input <path>, -i <path>
        input source files
    --verboseNames
        If set, names of shapes not be simplified and will be as verbose as possible
    --failOnValidationErrors
        If set, abort the conversion if any specs contains a validation error
    --useEnumTraitSyntax
        output enum types with the smithy v1 enum trait (deprecated) syntax
    --outputJson
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

| OpenAPI Base Type | OpenAPI Format     | Smithy Shape               | Smithy Trait(s)               |
|-------------------|--------------------|----------------------------|-------------------------------|
| string            |                    | String                     |                               |
| string            | timestamp          | Timestamp                  |                               |
| string            | date-time          | Timestamp                  | @timestampFormat("date-time") |
| string            | date               | String                     | alloy#dateFormat              |
| string            | uuid               | alloy#UUID                 |                               |
| string            | binary             | Blob                       |                               |
| string            | byte               | Blob                       |                               |
| string            | password           | String                     | @sensitive                    |
| number            | float              | Float                      |                               |
| number            | double             | Double                     |                               |
| number            | double             | Double                     |                               |
| number            |                    | Double                     |                               |
| integer           |                    | Integer                    |                               |
| integer           | int32              | Integer                    |                               |
| integer           | int64              | Long                       |                               |
| boolean           |                    | Boolean                    |                               |
| object            | (empty properties) | Document                   |                               |

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
      x-null: null
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
 "x-null": null,
 "x-obj": {
   a: 1,
   b: 2
 }
)
string MyString
```

## JSON Schema

### CLI Usage

```
> smithy-translate json-schema-to-smithy --help

Usage: smithy-translate json-schema-to-smithy --input <path> [--input <path>]... [--verboseNames] [--failOnValidationErrors] [--useEnumTraitSyntax] [--outputJson] <directory>

Take Json Schema specs as input and produce Smithy files as output.

Options and flags:
    --help
        Display this help text.
    --input <path>, -i <path>
        input source files
    --verboseNames
        If set, names of shapes not be simplified and will be as verbose as possible
    --failOnValidationErrors
        If set, abort the conversion if any specs contains a validation error
    --useEnumTraitSyntax
        output enum types with the smithy v1 enum trait (deprecated) syntax
    --outputJson
        changes output format to be json representations of the smithy models
```

Run `smithy-translate json-schema-to-smithy --help` for all usage information.

### Differences from OpenAPI

Most of the functionality of the `OpenAPI => Smithy` conversion is the same for the `JSON Schema => Smithy` one. As such, here
we will outline any differences that exist. Everything else is the same.

#### Default Values

Default values from JSON Schema will be captured in the `smithy.api#default` trait.

JSON Schema:
```json
{
 "$id": "test.json",
 "$schema": "http://json-schema.org/draft-07/schema#",
 "title": "Person",
 "type": "object",
 "properties": {
   "firstName": {
     "type": "string",
     "default": "Sally"
   }
 }
}
```

Smithy:
```smithy
structure Person {
 @default("Sally")
 firstName: String
}
```

#### Null Values

JSON Schemas allows for declaring types such as `["string", "null"]`. This type declaration 
on a required field means that the value cannot be omitted from the `JSON` payload entirely,
but may be set to `null`. For example:

JSON Schema:
```json
{
  "$id": "test.json",
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "Foo",
  "type": "object",
  "properties": {
    "bar": {
      "type": ["string", "null"]
    }
  },
  "required": ["bar"]
}
```

Smithy:
```smithy
use alloy#nullable

structure Foo {
 @required
 @nullable
 bar: String
}
```

In most protocols, there is likely no difference between an optional field and a nullable optional field.
Similarly, some protocols may not allow for required fields to be nullable. These considerations are left
up to the protocol itself.

#### Maps

JSON Schema doesn't provide a first-class type for defining maps. As such, we translate a commonly-used
convention into map types when encountered. When `patternProperties` is set to have a single entry, `.*`,
we translate that to a smithy map type.

JSON Schema:
```json
{
  "$id": "test.json",
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "TestMap",
  "type": "object",
  "patternProperties": {
    ".*": {
      "type": "string"
    }
  }
}
```

Smithy:
```smithy
map TestMap {
 key: String,
 value: String
}
```

## Protobuf

### CLI Usage

```
> smithy-translate smithy-to-proto --help

Usage: smithy-translate smithy-to-proto --input <path> [--input <path>]... [--dependency <string>]... [--repository <string>]... <directory>

Take Smithy definitions as input and produce Proto files as output.

Options and flags:
    --help
        Display this help text.
    --input <path>, -i <path>
        input source files
    --dependency <string>
        Dependencies that contains Smithy definitions.
    --repository <string>
        Specify repositories to fetch dependencies from.
```

Run `smithytranslate smithy-to-proto --help` for more usage information.

### Capabilities and Design

#### Primitives

There are more precises number scalar types in protobuf that don't exist in Smithy. For reference, see [here](https://developers.google.com/protocol-buffers/docs/proto3#scalar). You can still model those using the `@protoNumType` trait. The `@required` trait also has an effect on the final protobuf type because we use Google's wrapper types. See the following table for an exhaustive list:

| Smithy type          | @protoNumType | @required | Proto                        |
| -------------------- | ------------- | --------- | ---------------------------- |
| bigDecimal           | N/A           | N/A       | message { string value = 1 } |
| bigInteger           | N/A           | N/A       | message { string value = 1 } |
| blob                 | N/A           | false     | google.protobuf.BytesValue   |
| blob                 | N/A           | true      | bytes                        |
| boolean              | N/A           | false     | google.protobuf.BoolValue    |
| boolean              | N/A           | true      | bool                         |
| double               | N/A           | false     | google.protobuf.DoubleValue  |
| double               | N/A           | true      | double                       |
| float                | N/A           | false     | google.protobuf.FloatValue   |
| float                | N/A           | true      | float                        |
| integer, byte, short | FIXED         | false     | google.protobuf.Int32Value   |
| integer, byte, short | FIXED         | true      | fixed32                      |
| integer, byte, short | FIXED_SIGNED  | false     | google.protobuf.Int32Value   |
| integer, byte, short | FIXED_SIGNED  | true      | sfixed32                     |
| integer, byte, short | N/A           | true      | google.protobuf.Int32Value   |
| integer, byte, short | N/A           | true      | int32                        |
| integer, byte, short | SIGNED        | false     | google.protobuf.Int32Value   |
| integer, byte, short | SIGNED        | true      | sint32                       |
| integer, byte, short | UNSIGNED      | false     | google.protobuf.UInt32Value  |
| integer, byte, short | UNSIGNED      | true      | uint32                       |
| long                 | FIXED         | false     | google.protobuf.Int64Value   |
| long                 | FIXED         | true      | fixed64                      |
| long                 | FIXED_SIGNED  | false     | google.protobuf.Int64Value   |
| long                 | FIXED_SIGNED  | true      | sfixed64                     |
| long                 | N/A           | true      | google.protobuf.Int64Value   |
| long                 | N/A           | true      | int64                        |
| long                 | SIGNED        | false     | google.protobuf.Int64Value   |
| long                 | SIGNED        | true      | sint64                       |
| long                 | UNSIGNED      | false     | google.protobuf.UInt64Value  |
| long                 | UNSIGNED      | true      | uint64                       |
| string               | N/A           | false     | google.protobuf.StringValue  |
| string               | N/A           | true      | string                       |
| timestamp            | N/A           | N/A       | message { long value = 1 }   |

_Note: we can see from the table that the `@protoNumType` has no effect on non-required integer/long (except `UNSIGNED`). This is because there are no FIXED, FIXED_SIGNED or SIGNED instances in the Google's protobuf wrappers_

Smithy Translate has special support for `alloy#UUID`. A custom `message` is used in place of `alloy#UUID`. This message is defined as such and it is optmized for compactness:

Smithy:
```smithy
structure UUID {
  @required
  upper_bits: Long
  @required
  lower_bits: Long
}
```

Proto:
```proto
message UUID {
  int64 upper_bits = 1;
  int64 lower_bits = 2;
}
```

#### Aggregate Types

##### Structure

Smithy:
```smithy
structure Testing {
  myString: String,
  myInt: Integer
}
```

Proto:
```proto
import "google/protobuf/wrappers.proto";

message Testing {
  google.protobuf.StringValue myString = 1;
  google.protobuf.Int32Value myInt = 2;
}
```

##### Union

Smithy:
```smithy
structure Union {
  @required
  value: TestUnion
}

union TestUnion {
    num: Integer,
    txt: String
}
```

Proto:
```proto
message Union {
  oneof value {
    int32 num = 1;
    string txt = 2;
  }
}
```

##### List

Smithy:
```smithy
list StringArrayType {
    member: String
}
structure StringArray {
    value: StringArrayType
}
```

Proto:
```proto
message StringArray {
  repeated string value = 1;
}
```

##### Map

Smithy:
```smithy
map StringStringMapType {
    key: String,
    value: String
}
structure StringStringMap {
  value: StringStringMapType
}
```

Proto:
```proto
message StringStringMap {
  map<string, string> value = 1;
}
```

#### Constraints

##### Enum

Smithy:
```smithy
enum Color {
    RED
    GREEN
    BLUE
}
```

Proto:
```proto
enum Color {
  RED = 0;
  GREEN = 1;
  BLUE = 2;
}
```

#### Service Shapes

##### Basic Service

Smithy:
```smithy
use alloy.proto#protoEnabled

@protoEnabled
service FooService {
    operations: [Test]
}

@http(method: "POST", uri: "/test", code: 200)
operation Test {
    input: TestInput,
    output: Test200
}

structure InputBody {
    @required
    s: String
}

structure OutputBody {
    sNum: Integer
}

structure Test200 {
    @httpPayload
    @required
    body: OutputBody
}

structure TestInput {
    @httpPayload
    @required
    body: InputBody
}
```

definitions.proto:
```proto
import "google/protobuf/wrappers.proto";

service FooService {
  rpc Test(foo.TestInput) returns (foo.Test200);
}

message InputBody {
  string s = 1;
}

message OutputBody {
  google.protobuf.Int32Value sNum = 1;
}

message Test200 {
  foo.OutputBody body = 1;
}

message TestInput {
  foo.InputBody body = 1;
}
```

### Options

Individual protobuf definitions file (`.proto`) can contain _options_. We support this feature using Smithy's metadata attribute. 

There are a few importing things to notice

1. All options are defined under the metadata key `proto_options`
2. The value is an array. This is because Smithy will concatenate the arrays if the model contains multiple entries
3. Each entry of the array is an object where the keys are the namespace and the values are objects that represent the options
4. Entries for other namespaces are ignored (for example, `demo` in the example below)
5. The object that represents an option can only use `String` as value (see the example below). More detail below.

#### Stringly typed options

We used a `String` to represent the option such as `"true"` for a boolean and `"\"demo\""` for a String because it's the simplest approach to cover all use cases supported by `protoc`. `protoc` supports simple types like you'd expect: `bool`, `int`, `float` and `string`. But it also supports `identifier` which are a reference to some value that `protoc` knows about. For example: `option optimize_for = CODE_SIZE;`. Using a `String` for the value allows us to model this, while keeping thing simple. It allows prevent the users from trying to use `Array`s or `Object` as value for options.

#### Example

The following is an example:

Smithy:
```smithy
$version: "2"

metadata "proto_options" = [{
  "foo": {
      "java_multiple_files": "true",
      "java_package": "\"foo.pkg\""
  },
  "demo": {
      "java_multiple_files": "true"
  }
}]

namespace foo

string MyString
```

Proto:
```proto
syntax = "proto3";

option java_multiple_files = true;
option java_package = "foo.pkg";

package foo;

message MyString {
  string value = 1;
}
```

## formatter


### CLI Usage

The `smithytranslate` CLI will recursively go through all child directories of the
input directory provided and format any Smithy files it finds. The output 

```
> smithytranslate format --help

Usage: smithy-translate format [--no-clobber] <path to Smithy file or directory containing Smithy files>...

validates and formats smithy files

Options and flags:
    --help
        Display this help text.
    --no-clobber
        dont overwrite existing file instead create a new file with the word 'formatted' appended so test.smithy -> test_formatted.smithy
```

### Capabilities and Design
 - The formatter is based off the ABNF defined at [Smithy-Idl-ABNF](https://smithy.io/2.0/spec/idl.html#smithy-idl-abnf) 
 - The formatter assumes the file is a valid Smithy file and must be able to pass the Model Assembler validation , otherwise it will return an error
 - use --no-clobber to create a new file to avoid overwriting the original file
 - actual formatting rules are still WIP and will be updated as the formatter is developed
