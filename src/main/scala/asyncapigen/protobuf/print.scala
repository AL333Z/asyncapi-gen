package asyncapigen.protobuf

import asyncapigen.Printer
import asyncapigen.protobuf.schema._
import asyncapigen.protobuf.schema.Protobuf._

object print {
  private def printOption(o: OptionValue): String = s"${o.name} = ${o.value}"

  def printSchema: Printer[Protobuf] = {

    def enumToString(e: TEnum): String = {
      val printOptions = e.options.map(o => s"\toption ${o.name} = ${o.value};").mkString("\n")
      val printSymbols = e.symbols.map({ case (s, i) => s"\t$s = $i;" }).mkString("\n")
      val printAliases = e.aliases.map({ case (s, i) => s"\t$s = $i;" }).mkString("\n")

      s"""
         |enum ${e.name} {
         |$printOptions
         |$printSymbols
         |$printAliases
         |}
      """.stripMargin
    }

    def messageToString(m: TMessage): String = {
      val printReserved: String = m.reserved
        .map(l => s"reserved " + l.mkString(start = "", sep = ", ", end = ";"))
        .mkString("\n  ")

      def printOptions(options: List[OptionValue]): String =
        if (options.isEmpty)
          ""
        else
          options.map(printOption).mkString(start = " [", sep = ", ", end = "]")

      val printFields =
        m.fields
          .map {
            case f: FieldType.Field =>
              s"${f.tpe} ${f.name} = ${f.position}${printOptions(f.options)};"
            case oneOf: FieldType.OneOfField =>
              s"${oneOf.tpe}"
          }
          .mkString("\n  ")

      val printNestedMessages = m.nestedMessages.mkString("\n")

      val printNestedEnums = m.nestedEnums.mkString("\n")

      s"""
         |message ${m.name} {
         |  $printReserved
         |  $printFields
         |  $printNestedMessages
         |  $printNestedEnums
         |}
      """.stripMargin
    }

    Printer.print[Protobuf] {
      case TNull                                   => "null"
      case TDouble                                 => "double"
      case TFloat                                  => "float"
      case TInt32                                  => "int32"
      case TInt64                                  => "int64"
      case TUint32                                 => "uint32"
      case TUint64                                 => "uint64"
      case TSint32                                 => "sint32"
      case TSint64                                 => "sint64"
      case TFixed32                                => "fixed32"
      case TFixed64                                => "fixed64"
      case TSfixed32                               => "sfixed32"
      case TSfixed64                               => "sfixed64"
      case TBool                                   => "bool"
      case TString                                 => "string"
      case TBytes                                  => "bytes"
      case TNamedType(_, name)                     => name
      case TOptionalNamedType(_, name)             => s"optional $name"
      case TRepeated(value)                        => s"repeated $value"
      case TMap(key, value)                        => s"map<$key, $value>"
      case TFileDescriptor(values, _, packageName) => s"package $packageName; \n ${values.mkString("\n")}"
      case e: TEnum                                => enumToString(e)
      case m: TMessage                             => messageToString(m)
      case TOneOf(name, fields) =>
        val printFields =
          fields
            .map(f => s"${f.tpe} ${f.name} = ${f.position};")
            .toList
            .mkString("\n  ")

        s"""
           |oneof $name {
           |  $printFields
           |}
        """.stripMargin
    }
  }
}
