package smithytranslate
package formatter
package ast

import smithytranslate.formatter.ast.NodeValue.NodeObjectKey

case class MetadataSection(metadata: List[MetadataStatement])

// parse rules   "metadata" ws node_object_key ws "=" ws node_value br
case class MetadataStatement(
    nodeObjectKey: NodeObjectKey,
    nodeValue: NodeValue,
    break: Break
)
