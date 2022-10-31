$version: "2"
metadata suppressions = [
    {
        id: "UnreferencedShape",
        namespace: "smithytranslate",
        reason: "This is a library namespace."
    }
]

namespace smithytranslate

@trait(selector: "union")
structure contentTypeDiscriminated {
}

@trait(selector: ":test(union [trait|smithytranslate#contentTypeDiscriminated], structure) > :test(member < union, member [trait|httpPayload])")
string contentType

@trait(selector: "*")
string errorMessage

@trait
map openapiExtensions {
  key: String,
  value: Document
}

@trait(selector: ":test(timestamp, member > timestamp)")
structure dateOnly {
}

@trait(selector: "structure")
structure nullFormat {
}

@nullFormat
structure Null {
}

@trait(
  selector: "structure > member :test(> :is(simpleType, list, map))",
  conflicts: [required]
)
document defaultValue

@trait()
structure nullable {}
