package smithytranslate.compiler

import software.amazon.smithy.build.ProjectionTransformer

final case class ToSmithyCompilerOptions(
    useVerboseNames: Boolean,
    validateInput: Boolean,
    validateOutput: Boolean,
    transformers: List[ProjectionTransformer],
    useEnumTraitSyntax: Boolean,
    debug: Boolean
)
