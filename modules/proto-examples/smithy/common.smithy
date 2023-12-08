$version: "2"

namespace demo.common

use alloy.proto#protoIndex

// protoindex is not necessary , this is just for demo purposes
enum Language{
    @protoIndex(0)
    FRENCH
    @protoIndex(1)
    ENGLISH
}
