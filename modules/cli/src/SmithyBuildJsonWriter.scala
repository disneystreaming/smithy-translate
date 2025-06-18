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

package smithytranslate.cli

import internal.BuildInfo

object SmithyBuildJsonWriter {
  def writeDefault(dest: os.Path, force: Boolean = false) = {
    val contents = ujson.Obj(
      "maven" -> ujson.Obj(
        "dependencies" -> ujson.Arr(
          ujson.Str(
            s"io.github.disneystreaming.alloy:alloy-core:${BuildInfo.alloyVersion}"
          ),
          ujson.Str(
            s"io.github.disneystreaming.smithy:smithytranslate-traits:${BuildInfo.cliVersion}"
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
