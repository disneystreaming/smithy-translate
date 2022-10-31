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

/** This code is copy pasted from `com.monovore.decline.CommandApp`. This is
  * because the default implementation returns 0 when the parsing fails. We'd
  * like to have a non-zero exit code to catch errors.
  *
  * Will be removed if https://github.com/bkirwi/decline/issues/404 is
  * addressed.
  */
package smithytranslate.cli

import com.monovore.decline._ // added

/** This abstract class takes a `Command[Unit]` and turns it into a main method
  * for your application. Normally, you want to extend this class from a
  * top-level object:
  *
  * {{{
  * package myapp
  *
  * import com.monovore.decline._
  *
  * object MyApp extends CommandApp(
  *   name = "my-app",
  *   header = "This is a standalone application!",
  *   main =
  *     Opts.flag("fantastic", "Everything is working.")
  * )
  * }}}
  *
  * This should now behave like any other object with a main method -- for
  * example, on the JVM, this could be invoked as `java myapp.MyApp
  * --fantastic`.
  */
abstract class CommandApp(command: Command[Unit]) {

  def this(
      name: String,
      header: String,
      main: Opts[Unit],
      helpFlag: Boolean = true,
      version: String = ""
  ) = {

    this {
      val showVersion =
        if (version.isEmpty) Opts.never
        else
          Opts
            .flag(
              "version",
              "Print the version number and exit.",
              visibility = Visibility.Partial
            )
            .map(_ => System.err.println(version))

      Command(name, header, helpFlag)(showVersion orElse main)
    }
  }

  @deprecated(
    """
The CommandApp.main method is not intended to be called by user code.
For suggested usage, see: http://monovore.com/decline/usage.html#defining-an-application""",
    "0.3.0"
  )
  final def main(args: Array[String]): Unit =
    command.parse(PlatformApp.ambientArgs getOrElse args, sys.env) match {
      case Left(help) =>
        System.err.println(help)
        System.exit(1)
      case Right(_) => ()
    }
}
