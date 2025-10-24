$version: "2"

namespace demo

use alloy.proto#protoIndex
use alloy.proto#protoInlinedOneOf
use alloy.proto#protoEnabled
use alloy.proto#protoReservedFields
use alloy.proto#protoNumType
use demo.common#Language

@documentation("The Hello service\nfor testing")
@protoEnabled
service Hello {
  version: "2017-12-11",
  operations: [SayHello, Greet]
}

@documentation("Say hello to the world\nif you want to")
operation SayHello {
  input: HelloRequest,
  output: HelloResponse
}

operation Greet {
  output: HelloResponse
}

@documentation("Structure to hold information used to say hello\nto someone")
@protoReservedFields([{number: 3}])
structure HelloRequest {
    @documentation("The name to say\nhello to")
    @protoIndex(1)
    @required
    name: String,

    @protoIndex(2)
    @documentation("The language to say hello in")
    lang: Language,

    @protoIndex(4)
    @required
    @documentation("A required integer")
    requiredInt: Integer,

    @protoIndex(5)
    @documentation("An optional integer")
    int: Integer
}

structure HelloResponse {
    @protoIndex(1)
    @required
    message: String,

    @protoNumType("UNSIGNED")
    @protoIndex(2)
    size: Long,

    @protoIndex(3)
    apiStruct: UseApiStruct

    @required
    apiUnion: ApiUnion

    @protoIndex(6)
    @required
    anotherLong: Long,
}

structure UseApiStruct {
  @documentation("A big integer")
  bigInt: BigInteger,

  @documentation("A big decimal")
  bigDec: BigDecimal,

  @documentation("A timestamp")
  ts: Timestamp
}

@protoInlinedOneOf
@documentation("API Union\nwhich version")
union ApiUnion {
  @documentation("The version of the API\nin use")
  @protoIndex(4)
  version: String,
  @protoIndex(5)
  id: Integer
}

@protoEnabled
structure DocumentWrapper {
  @required
  doc: Document
}
