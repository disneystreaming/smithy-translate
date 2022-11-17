package smithytranslate.cli.opts

import smithytranslate.cli.opts.{OpenAPIJsonSchemaOpts, ProtoOpts}
import smithytranslate.cli.opts.FormatterOpts.FormatOpts

sealed trait SmithyTranslateCommand
object SmithyTranslateCommand {
  case class Format(formatOpts: FormatOpts) extends SmithyTranslateCommand
  case class ProtoTranslate(protoOpts: ProtoOpts) extends SmithyTranslateCommand
  case class OpenApiTranslate(openAPIJsonSchemaOpts: OpenAPIJsonSchemaOpts)
      extends SmithyTranslateCommand
}
