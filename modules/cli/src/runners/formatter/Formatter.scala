/* Copyright 2022 Disney Streaming
 *
 * Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://disneystreaming.github.io/TOST-1.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
