# JSON Schema

### CLI Usage

```
> smithytranslate json-schema-to-smithy --help

Usage: smithytranslate json-schema-to-smithy --input <path> [--input <path>]... [--verboseNames] [--failOnValidationErrors] [--useEnumTraitSyntax] [--outputJson] <directory>

Take Json Schema specs as input and produce Smithy files as output.

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

Run `smithytranslate json-schema-to-smithy --help` for all usage information.

### Differences from OpenAPI

Most of the functionality of the `OpenAPI => Smithy` conversion is the same for the `JSON Schema => Smithy` one. As such, here
we will outline any differences that exist. Everything else is the same. See [OpenAPI docs](openapi.md) for more information.

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
