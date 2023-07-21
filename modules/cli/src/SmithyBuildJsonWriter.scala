package smithytranslate.cli

import internal.BuildInfo

object SmithyBuildJsonWriter {
  def writeDefault(dest: os.Path, force: Boolean = false) = {
    val contents = ujson.Obj(
      "maven" -> ujson.Obj(
        "dependencies" -> ujson.Arr(
          ujson.Str(
            s"com.disneystreaming.alloy:alloy-core:${BuildInfo.alloyVersion}"
          ),
          ujson.Str(
            s"com.disneystreaming.smithy:smithytranslate-traits:${BuildInfo.cliVersion}"
          )
        )
      )
    )

    val destPath = dest / "smithy-build.json"

    if (force)
      os.write.over(destPath, contents)
    else if (destPath.toIO.exists() && !force)
      System.err.println(
        s"Destination [$destPath] already exist - to overwrite, please use --force flag"
      )
    else
      os.write(destPath, contents)

  }
}
