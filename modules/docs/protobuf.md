# Protobuf

### CLI Usage

```
> smithytranslate smithy-to-proto --help

Usage: smithytranslate smithy-to-proto --input <path> [--input <path>]... [--dependency <string>]... [--repository <string>]... <directory>

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

The design of the smithy to protobuf translation follows the semantics defined in the [alloy specification][alloy-spec].

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

structure Foo {
  value: String
}
```

Proto:
```proto
syntax = "proto3";

option java_multiple_files = true;
option java_package = "foo.pkg";

package foo;

message Foo {
  string value = 1;
}
```


[alloy-spec]: https://github.com/disneystreaming/alloy/blob/main/modules/docs/serialisation/protobuf.md