package smithytranslate.cli.opts

import cats.data.NonEmptyList
import cats.implicits.catsSyntaxTuple2Semigroupal
import com.monovore.decline.{Command, Opts}
import smithytranslate.cli.opts.SmithyTranslateCommand.Format

import java.nio.file.Path

object FormatterOpts {
  case class FormatOpts(smithyFile: NonEmptyList[Path], noClobber: Boolean)
  val header = "validates and formats smithy files"

  val smithyFile: Opts[NonEmptyList[Path]] =
    Opts.arguments[Path]("path to smithy file")
  val noClobber: Opts[Boolean] = Opts
    .flag(
      "no-clobber",
      "dont overwrite existing file instead create a new file with the formatted appended"
    )
    .orFalse

  val formatOpts: Opts[FormatOpts] =
    (smithyFile, noClobber).mapN(FormatOpts.apply)

  val formatCommand: Command[FormatOpts] =
    Command(name = "format", header = header)(formatOpts)

  val format: Opts[Format] =
    Opts.subcommand(command = formatCommand).map(Format)

}
