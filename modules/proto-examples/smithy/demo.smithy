$version: "2"

namespace demo

use alloy.proto#protoIndex
use alloy.proto#protoInlinedOneOf
use alloy.proto#protoEnabled
use alloy.proto#protoReservedFields
use alloy.proto#protoNumType
use alloy#UUID
use demo.common#Language

@protoEnabled
service Hello {
  version: "2017-12-11",
  operations: [SayHello, Greet]
}

operation SayHello {
  input: HelloRequest,
  output: HelloResponse
}

operation Greet {
  output: HelloResponse
}

@protoReservedFields([{number: 3}])
structure HelloRequest {
    @protoIndex(1)
    @required
    name: String,

    @protoIndex(2)
    lang: Language,

    @protoIndex(4)
    @required
    requiredInt: Integer,

    @protoIndex(5)
    int: Integer

    @protoIndex(6)
    id: UUID
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
  bigInt: BigInteger,
  bigDec: BigDecimal,
  ts: Timestamp
}

@protoInlinedOneOf
union ApiUnion {
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
