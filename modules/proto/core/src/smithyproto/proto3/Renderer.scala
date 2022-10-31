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

package smithyproto
package proto3

object Renderer {

  import ProtoIR._
  import Text._

  def render(compilationUnit: CompilationUnit): String = {
    val text = many(
      statement(s"""syntax = "proto3""""),
      emptyLine,
      maybe(
        compilationUnit.packageName.map(packageName =>
          many(renderPackageName(packageName), emptyLine)
        )
      ),
      intersperse(
        many(compilationUnit.statements.map(renderStatement)),
        emptyLine
      ),
      emptyLine
    )

    renderText(text)
  }

  def renderPackageName(packageName: String): Text =
    statement(s"package ${packageName}")

  def renderStatement(st: Statement): Text =
    st match {
      case Statement.ImportStatement(path)  => statement(s"""import "$path"""")
      case Statement.OptionStatement        => Text.emptyLine
      case Statement.TopLevelStatement(tld) => renderTopLevelDef(tld)
    }

  def renderTopLevelDef(tld: TopLevelDef): Text =
    tld match {
      case TopLevelDef.MessageDef(message) => renderMessage(message)
      case TopLevelDef.EnumDef(enumDef)    => renderEnum(enumDef)
      case TopLevelDef.ServiceDef(service) => renderService(service)
    }

  def renderEnumElement(enumValue: EnumValue): Text = {
    enumValue match {
      case EnumValue(identifier, intvalue) =>
        line(s"$identifier = $intvalue;")
    }
  }

  def renderEnum(enumeration: Enum): Text =
    many(
      line(s"enum ${enumeration.name} {"),
      indent(renderReserved(enumeration.reserved)),
      indent(enumeration.values.map(renderEnumElement)),
      line("}")
    )

  def renderMessage(message: Message): Text =
    many(
      line(s"message ${message.name} {"),
      indent(renderReserved(message.reserved)),
      indent(message.elements.map(renderMessageElement)),
      line("}")
    )

  def renderOneof(oneof: Oneof): Text =
    many(
      line(s"oneof ${oneof.name} {"),
      indent(oneof.fields.map(renderField)),
      line("}")
    )

  def renderMessageElement(element: MessageElement): Text =
    element match {
      case MessageElement.FieldElement(field)        => renderField(field)
      case MessageElement.EnumDefElement(enumDef)    => renderEnum(enumDef)
      case MessageElement.MessageDefElement(message) => renderMessage(message)
      case MessageElement.OneofElement(oneof)        => renderOneof(oneof)
    }

  // collapses into a few reserved statements
  def renderReserved(reserved: List[Reserved]): Text = {
    val numeric = reserved.collect {
      case Reserved.Number(number)    => s"$number"
      case Reserved.Range(start, end) => s"$start to $end"
    }
    val names = reserved.collect { case Reserved.Name(name) =>
      s""""$name""""
    }
    // TODO: cats.data.NonEmptyList would be useful
    many(
      maybe(
        if (numeric.nonEmpty)
          Some(line(numeric.mkString("reserved ", ", ", ";")))
        else None
      ),
      maybe(
        if (names.nonEmpty) Some(line(names.mkString("reserved ", ", ", ";")))
        else None
      )
    )
  }

  def renderField(field: Field): Text = {
    val repeated = if (field.repeated) "repeated " else ""
    val ty = renderType(field.ty)
    statement(s"$repeated$ty ${field.name} = ${field.number}")
  }

  def renderService(service: Service): Text =
    many(
      line(s"service ${service.name} {"),
      indent(service.rpcs.map(renderRpc)),
      line("}"),
      emptyLine
    )

  def renderRpc(rpc: Rpc): Text =
    statement(
      s"rpc ${rpc.name}(${rpc.requestFqn.render}) returns (${rpc.responseFqn.render})"
    )

  def renderType(ty: Type): String = {
    import Type._
    ty match {
      case Double   => "double"
      case Float    => "float"
      case Int32    => "int32"
      case Int64    => "int64"
      case Uint32   => "uint32"
      case Uint64   => "uint64"
      case Sint32   => "sint32"
      case Sint64   => "sint64"
      case Fixed32  => "fixed32"
      case Fixed64  => "fixed64"
      case Sfixed32 => "sfixed32"
      case Sfixed64 => "sfixed64"
      case Bool     => "bool"
      case String   => "string"
      case Bytes    => "bytes"
      case ty @ MapType(_, valueType) =>
        s"map<${renderType(ty.foldedKeyType)}, ${renderType(valueType)}>"
      case ListType(valueType) => s"repeated ${renderType(valueType)}"
      case MessageType(fqn)    => fqn.render
      case EnumType(fqn)       => fqn.render
      case Any                 => Any.fqn.render
      case Empty               => Empty.fqn.render
      case w: Wrappers         => w.fqn.render
    }
  }

  def statement(string: String): Text =
    line(s"$string;")

}
