/*
 * Copyright (c) 2021 al333z
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package asyncapigen.protobuf

import asyncapigen.Printer
import asyncapigen.Printer.syntax._
import asyncapigen.protobuf.schema.FieldDescriptorProto.{
  EnumFieldDescriptorProto,
  OneofDescriptorProto,
  PlainFieldDescriptorProto
}
import asyncapigen.protobuf.schema.FieldDescriptorProtoLabel.{Optional, _}
import asyncapigen.protobuf.schema.FieldProtoType._
import asyncapigen.protobuf.schema._

object print {
  implicit val printOptionValue: Printer[OptionValue] = Printer.print[OptionValue] { ov =>
    s"${ov.name} = ${ov.value}"
  }

  implicit val printFieldDescriptorProtoType: Printer[FieldProtoType] = {
    Printer[FieldProtoType] {
      case NullProto            => "null"
      case DoubleProto          => "double"
      case FloatProto           => "float"
      case Int32Proto           => "int32"
      case Int64Proto           => "int64"
      case Uint32Proto          => "uint32"
      case Uint64Proto          => "uint64"
      case Sint32Proto          => "sint32"
      case Sint64Proto          => "sint64"
      case Fixed32Proto         => "fixed32"
      case Fixed64Proto         => "fixed64"
      case Sfixed32Proto        => "sfixed32"
      case Sfixed64Proto        => "sfixed64"
      case BoolProto            => "bool"
      case StringProto          => "string"
      case BytesProto           => "bytes"
      case NamedTypeProto(name) => name
    }
  }

  implicit val printFieldDescriptorProtoLabel: Printer[FieldDescriptorProtoLabel] =
    Printer.print[FieldDescriptorProtoLabel] {
      case Optional => "optional"
      case Required => ""
      case Repeated => "repeated"
    }

  implicit val printEnumDescriptorProto: Printer[EnumDescriptorProto] =
    Printer.print[EnumDescriptorProto] { edp =>
      val printOptions = edp.options.map(o => s"option ${o.name} = ${o.value};").leftSpaced
      val printSymbols = edp.symbols.map { case (s, i) => s"$s = $i;" }.toList.leftSpaced
      s"""
        |enum ${edp.name} {
        |$printOptions
        |$printSymbols
        |}
      """.stripMargin
    }

  implicit val printEnumFieldDescriptorProto: Printer[EnumFieldDescriptorProto] =
    Printer.print[EnumFieldDescriptorProto] { edp =>
      s"${edp.label.print} ${edp.enumeration.name} ${edp.name} = ${edp.index};".stripLeadingSpaces
    }

  private val printPlainFieldDescriptorProto: Printer[PlainFieldDescriptorProto] =
    Printer.print[PlainFieldDescriptorProto] { pfdp =>
      def printOptions(options: List[OptionValue]): String =
        if (options.isEmpty)
          ""
        else
          options.map(_.print).mkString(start = "[", sep = ", ", end = "]")

      s"${pfdp.label.print} ${pfdp.`type`.print} ${pfdp.name} = ${pfdp.index}${printOptions(pfdp.options)};".stripLeadingSpaces
    }

  private val printOneofDescriptorProto: Printer[OneofDescriptorProto] =
    Printer.print[OneofDescriptorProto] { oodp =>
      val printFields =
        oodp.fields.map {
          case Right(EnumFieldDescriptorProto(name, enumeration, label, index)) =>
            s"${label.print} ${enumeration.name} $name = $index;".stripLeadingSpaces
          case Left(PlainFieldDescriptorProto(name, tpe, label, _, index, _)) =>
            s"${label.print} ${tpe.print} $name = $index;".stripLeadingSpaces
        }.leftSpaced
      s"""
         |${oodp.label.print} oneof ${oodp.name} {
         |$printFields
         |}
      """.stripMargin.stripLeadingSpaces
    }

  implicit val printFieldDescriptorProto: Printer[FieldDescriptorProto] = Printer.print[FieldDescriptorProto] {
    case e: EnumFieldDescriptorProto  => printEnumFieldDescriptorProto.print(e)
    case o: OneofDescriptorProto      => printOneofDescriptorProto.print(o)
    case f: PlainFieldDescriptorProto => printPlainFieldDescriptorProto.print(f)
  }

  implicit val printMessageDescriptorProto: Printer[MessageDescriptorProto] =
    Printer.print[MessageDescriptorProto] { mdp =>
      val printFields         = mdp.fields.map(_.print).leftSpaced
      val printNestedMessages = mdp.nestedMessages.map(_.print).leftSpaced
      val printNestedEnums    = mdp.nestedEnums.map(_.print).leftSpaced
      s"""
         |message ${mdp.name} {
         |$printFields
         |$printNestedMessages
         |$printNestedEnums
         |}
      """.stripMargin
    }

  implicit val printFileDescriptorProto: Printer[FileDescriptorProto] =
    Printer.print[FileDescriptorProto] { case FileDescriptorProto(_, pckage, messages, enums, syntax) =>
      s"""
      |syntax = "$syntax";
      |${pckage.fold("")(x => s"package $x;")}
      |${messages.map(_.print).mkString("\n")}
      |${enums.map(_.print).mkString("\n")}
      |""".stripMargin
    }

  implicit class RichListString(val inner: List[String]) extends AnyVal {
    def leftSpaced: String = inner.map(_.leftSpacedAllLines).mkString("\n")
  }
  implicit class RichString(val inner: String) extends AnyVal {
    def stripLeadingSpaces: String = inner.dropWhile(c => c == ' ' || c == '\t' || Character.isWhitespace(c))
    def leftSpaced: String         = "  " + inner
    def leftSpacedAllLines: String = inner.split('\n').map(_.leftSpaced).mkString("\n")
    def normalized: String =
      inner
        .replaceAll("\\s*[(\\r\\n|\\r|\\n)]+", "\n")
        .replaceAll("\\{\\s*\\}", "{}")
  }
}
