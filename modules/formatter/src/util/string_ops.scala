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
package util

import cats.Show
import cats.implicits.toShow

object string_ops {

  implicit class StringMutations(val s: String) {

    private def insertAt(index: Int, str: String): String = {
      s.substring(0, index) + str + s.substring(index)
    }
    def insertAfter(char: Char, toInsert: String): String = {
      val index = s.indexOf(char.toInt)
      if (index < 0) s
      else insertAt(index, toInsert)
    }

    def insertBefore(char: Char, toInsert: String): String = {
      val index = s.indexOf(char.toInt) - 1
      if (index < 0) s
      else insertAt(index, toInsert)
    }

    def insertAfterLast(char: Char, toInsert: String): String = {
      val index = s.lastIndexOf(char.toInt)
      if (index < 0) s
      else insertAt(index + 1, toInsert)
    }

    def insertBeforeLast(char: Char, toInsert: String): String = {
      val index = s.lastIndexOf(char.toInt)
      if (index < 0) s
      else insertAt(index, toInsert)
    }

    def slice(open: Char, close: Char): String = {
      val openIndex = s.indexOf(open.toInt)
      val closeIndex = s.indexOf(close.toInt)
      if (openIndex < 0 || closeIndex < 0) s
      else s.substring(openIndex + 1, closeIndex)

    }

    def insertAfterAll(char: Char, toInsert: String): String = {
      ???
    }
  }

  // todo insertion utils
  // todo insert before character/after , single instance, all instance, first instance ,last instance, all except first , all except last
  // index based
  // structure detection
  // todo isArray,isObject,isEnum

  def purgeIfNonText(string: String): String = {
    if (containsNonWhitespace(string)) {
      string
    } else {
      ""
    }
  }
  def containsNonWhitespace(string: String): Boolean = {
    string.exists(!_.isWhitespace)
  }

  def doubleSpace(string: String): String = {
    if (!string.endsWith("\n"))
      string + "\n"
    else
      string
  }

  def indentList[A: Show](
      value: List[A],
      delimiter: String,
      indentLevel: Int
  ): String = {
    val indentation = " " * indentLevel
    val intermediate = value
      .map(_.show)
      .filterNot(_.isEmpty)
      .foldLeft(("", true)) { case ((acc, isFirst), line) =>
        if (isFirst) (acc + indentation + line, false)
        else (acc + delimiter + indentation + line, isFirst)
      }
      ._1
    if (
      (intermediate.trim.startsWith("[") || intermediate.trim.startsWith(
        "{"
      )) && intermediate.length < 80
    )
      intermediate.replaceAll("\n", "").replaceAll(" ", "")
    else
      intermediate
  }

  def indent(value: String, delimiter: String, indentLevel: Int): String = {
    val indentation = " " * indentLevel
    val intermediate = value
      .split(delimiter, -1)
      .toList
      .filterNot(_.isEmpty)
      .foldLeft(("", true)) { case ((acc, isFirst), line) =>
        if (isFirst) (acc + indentation + line, false)
        else (acc + delimiter + indentation + line, isFirst)
      }
      ._1
    if (
      (intermediate.trim.startsWith("[") || intermediate.trim.startsWith(
        "{"
      )) && intermediate.length < 80
    )
      intermediate.replaceAll(" ", "").replaceAll("\n", " ")
    else
      intermediate
  }

  def simpleIndent(spaces: Int, skipfirst: Boolean, str: String): String = {
    val indent = " " * spaces
    str.split("\n").mkString(if (skipfirst) "" else indent, s"\n$indent", "")
  }
  def formatEnum(string: String): String = {
    simpleIndent(
      4,
      skipfirst = true,
      string
        .replaceAll(" +", " ")
        .replaceAll("\n", "")
        .split("},", -1)
        .mkString("},\n")
        .split("\\[")
        .mkString("[\n")
    )
      .insertBeforeLast(']', "\n")
  }

  def formatStructure(string: String): String = {
    string.split(",").mkString(",\n")
  }

  def addBrackets(
      s: String,
      newLineAfterOpen: Boolean = true,
      newLineBeforeClose: Boolean = true
  ): String = {
    if (s.startsWith("{")) s
    else {
      val open = if (newLineAfterOpen) "\n{" else "{"
      val close = if (newLineBeforeClose) "\n}" else "}"
      open + s + close
    }
  }

  def indentIfNotEmpty(value: String): String = {
    value.split(",", -1).toList.foldLeft("") { case (acc, line) =>
      s"${acc}    ${line.replaceAll("\n", "")},\n"
    }
  }
  def formatArrayHorizontally(value: String): String = {
    val array = value.split(",").toList.foldLeft("") { case (acc, line) =>
      s"$acc   ${line.replaceAll("\n", "")},\n"
    }
    s"[\n${array}\n]"
  }

  def suffix(string: String, s: String): String = {
    if (string.isEmpty) string
    else string + s
  }

}
