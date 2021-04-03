package asyncapigen.protobuf

import cats.data.NonEmptyList

/**
 * @see https://developers.google.com/protocol-buffers/docs/reference/proto3-spec
 */
object schema {

  final case class OptionValue(name: String, value: String)

  final case class MessageDescriptorProto(
      name: String,
      fields: List[FieldDescriptorProto],
      nestedMessages: List[MessageDescriptorProto],
      nestedEnums: List[EnumDescriptorProto],
      options: List[OptionValue]
  )

  final case class EnumDescriptorProto(
      name: String,
      symbols: NonEmptyList[(String, Int)],
      options: List[OptionValue] = Nil
  )

  sealed abstract class FieldDescriptorProto
  object FieldDescriptorProto {

    final case class EnumFieldDescriptorProto(
        name: String,
        enum: EnumDescriptorProto,
        label: FieldDescriptorProtoLabel,
        index: Int
    ) extends FieldDescriptorProto

    final case class OneofDescriptorProto(
        name: String,
        label: FieldDescriptorProtoLabel,
        fields: List[Either[PlainFieldDescriptorProto, EnumFieldDescriptorProto]]
    ) extends FieldDescriptorProto

    case class PlainFieldDescriptorProto(
        name: String,
        `type`: FieldProtoType,
        label: FieldDescriptorProtoLabel,
        options: List[OptionValue],
        index: Int,
        messageProto: Option[MessageDescriptorProto] = None
    ) extends FieldDescriptorProto
  }

  sealed abstract class FieldProtoType
  object FieldProtoType {
    final case object NullProto                   extends FieldProtoType
    final case object DoubleProto                 extends FieldProtoType
    final case object FloatProto                  extends FieldProtoType
    final case object Int32Proto                  extends FieldProtoType
    final case object Int64Proto                  extends FieldProtoType
    final case object Uint32Proto                 extends FieldProtoType
    final case object Uint64Proto                 extends FieldProtoType
    final case object Sint32Proto                 extends FieldProtoType
    final case object Sint64Proto                 extends FieldProtoType
    final case object Fixed32Proto                extends FieldProtoType
    final case object Fixed64Proto                extends FieldProtoType
    final case object Sfixed32Proto               extends FieldProtoType
    final case object Sfixed64Proto               extends FieldProtoType
    final case object BoolProto                   extends FieldProtoType
    final case object StringProto                 extends FieldProtoType
    final case object BytesProto                  extends FieldProtoType
    final case class NamedTypeProto(name: String) extends FieldProtoType
  }

  sealed abstract class FieldDescriptorProtoLabel
  object FieldDescriptorProtoLabel {
    final case object Optional extends FieldDescriptorProtoLabel
    final case object Required extends FieldDescriptorProtoLabel
    final case object Repeated extends FieldDescriptorProtoLabel
  }

  final case class FileDescriptorProto(
      name: String,
      `package`: Option[String],
      messageTypes: List[MessageDescriptorProto],
      enumTypes: List[EnumDescriptorProto],
      syntax: String
  )
}
