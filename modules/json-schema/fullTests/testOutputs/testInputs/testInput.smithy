$version: "2.0"

namespace testInputs.testInput

/// A representation of a person, company, organization, or place
structure Test {
    fruits: Fruits
    vegetables: Vegetables
}

structure Veggie {
    /// The name of the vegetable.
    @required
    veggieName: String
    /// Do I like this vegetable?
    @required
    veggieLike: Boolean
}

list Fruits {
    member: String
}

list Vegetables {
    member: Veggie
}
