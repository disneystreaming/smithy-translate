package smithytranslate
package cli
package runners
package formatter

import software.amazon.smithy.model.Model
import java.nio.file.Path
import smithytranslate.formatter.parsers.SmithyParserLive
import smithytranslate.formatter.writers.Writer.WriterOps
import smithytranslate.formatter.writers.IdlWriter.idlWriter

object Formatter {

  def reformat(smithyFilePath: Path, noClobber: Boolean): Unit = {
    val basePath = os.pwd / os.RelPath(smithyFilePath)
    val contents: String = os.read(basePath)
    if (validator.validate(contents))
      SmithyParserLive
        .parse(contents)
        .fold(
          message =>
            println(
              s"unable to parse file at ${smithyFilePath.getFileName} because of ${message}"
            ),
          idl => {
            val newPath =
              if (noClobber) {
                val newFile = basePath
                  .getSegment(basePath.segmentCount - 1)
                  .split("\\.")
                  .mkString("_formatted.")
                os.Path(basePath.wrapped.getParent) / s"$newFile"
              } else basePath

            os.write.over(newPath, idl.write)
          }
        )
    else {
      println("Invalid Smithy file")
    }

  }
}

object validator {
  def validate(smithy: String): Boolean = {
    Model
      .assembler()
      .addUnparsedModel("formatter.smithy", smithy)
      .assemble()
      .validate()
      .isPresent
  }
}
