$version: "2"
metadata suppressions = [
    {
        id: "UnreferencedShape",
        namespace: "smithytranslate",
        reason: "This is a library namespace."
    }
]

namespace smithytranslate

@trait(selector: ":test(document, member > document)")
document const
