package smithytranslate
package formatter
package ast

import smithytranslate.formatter.ast.shapes.ShapeSection

case class Idl(
    ws: Whitespace,
    control: ControlSection,
    metadata: MetadataSection,
    shape: ShapeSection
)
