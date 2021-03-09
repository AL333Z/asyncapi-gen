package asyncapigen.protobuf

import asyncapigen.Printer
import asyncapigen.Printer.syntax._
import asyncapigen.protobuf.schema.FieldDescriptorProtoLabel.{Optional, _}
import asyncapigen.protobuf.schema.FieldDescriptorProtoType._
import asyncapigen.protobuf.schema._

object print {
  implicit val printOptionValue: Printer[OptionValue] = Printer.print[OptionValue] { ov =>
    s"${ov.name} = ${ov.value}"
  }

  implicit val printFieldDescriptorProtoType: Printer[FieldDescriptorProtoType] = {
    Printer[FieldDescriptorProtoType] {
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
      val printOptions = edp.options.map(o => s"\toption ${o.name} = ${o.value};").mkString("\n")
      val printSymbols = edp.symbols.map { case (s, i) => s"\t$s = $i;" }.toList.mkString("\n")
      s"""
        |enum ${edp.name} {
        |$printOptions
        |$printSymbols
        |}
      """.stripMargin
    }

  implicit val printOneofDescriptorProto: Printer[OneofDescriptorProto] =
    Printer.print[OneofDescriptorProto] { oodp =>
      val printFields =
        oodp.fields
          .map(f => s"${f.`type`.print} ${f.name} = ${f.index};")
          .toList
          .mkString("\n")

      s"""
        |oneof ${oodp.name} {
        |  $printFields
        |}
      """.stripMargin
    }

  implicit val printMessageDescriptorProto: Printer[MessageDescriptorProto] =
    Printer.print[MessageDescriptorProto] { mdp =>
      def printOptions(options: List[OptionValue]) =
        if (options.isEmpty)
          ""
        else
          options.map(_.print).mkString(start = " [", sep = ", ", end = "]")

      val printFields =
        mdp.fields
          .map { field =>
            s"${field.label.print} ${field.`type`.print} ${field.name} = ${field.index}${printOptions(field.options)};"
              .stripLeading()
          }
          .mkString("\n")
      val printNestedMessages: String = mdp.nestedMessages.map(_.print).mkString("\n")
      val printNestedEnums            = mdp.nestedEnums.map(_.print).mkString("\n")
      val printOneOfs                 = mdp.oneOfs.map(_.print)

      s"""
         |message ${mdp.name} {
         |  $printFields
         |  
         |  $printNestedMessages
         |  
         |  $printNestedEnums
         |  
         |  $printOneOfs
         |}
      """.stripMargin
    }

  implicit val printFileDescriptorProto: Printer[FileDescriptorProto] =
    Printer.print[FileDescriptorProto] { case FileDescriptorProto(_, pckage, messages, enums, syntax) =>
      s"""
           |syntax = "$syntax";
           |
           |package $pckage;
           |
           |${messages.map(_.print).mkString("\n")}
           |
           |${enums.map(_.print).mkString("\n")}
           |
           |""".stripMargin
    }

}
