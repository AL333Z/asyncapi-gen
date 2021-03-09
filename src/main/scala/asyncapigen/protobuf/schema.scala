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
      oneOfs: List[OneofDescriptorProto],
      label: FieldDescriptorProtoLabel,
      options: List[OptionValue],
      index: Int
  )

  final case class EnumDescriptorProto(
      name: String,
      symbols: NonEmptyList[(String, Int)],
      options: List[OptionValue]
  )

  final case class OneofDescriptorProto(
      name: String,
      fields: NonEmptyList[FieldDescriptorProto]
  )

  case class FieldDescriptorProto(
      name: String,
      `type`: FieldDescriptorProtoType,
      label: FieldDescriptorProtoLabel,
      options: List[OptionValue],
      index: Int
  )

  sealed abstract class FieldDescriptorProtoType
  object FieldDescriptorProtoType {
    final case object NullProto                   extends FieldDescriptorProtoType
    final case object DoubleProto                 extends FieldDescriptorProtoType
    final case object FloatProto                  extends FieldDescriptorProtoType
    final case object Int32Proto                  extends FieldDescriptorProtoType
    final case object Int64Proto                  extends FieldDescriptorProtoType
    final case object Uint32Proto                 extends FieldDescriptorProtoType
    final case object Uint64Proto                 extends FieldDescriptorProtoType
    final case object Sint32Proto                 extends FieldDescriptorProtoType
    final case object Sint64Proto                 extends FieldDescriptorProtoType
    final case object Fixed32Proto                extends FieldDescriptorProtoType
    final case object Fixed64Proto                extends FieldDescriptorProtoType
    final case object Sfixed32Proto               extends FieldDescriptorProtoType
    final case object Sfixed64Proto               extends FieldDescriptorProtoType
    final case object BoolProto                   extends FieldDescriptorProtoType
    final case object StringProto                 extends FieldDescriptorProtoType
    final case object BytesProto                  extends FieldDescriptorProtoType
    final case class NamedTypeProto(name: String) extends FieldDescriptorProtoType
  }

  sealed abstract class FieldDescriptorProtoLabel
  object FieldDescriptorProtoLabel {
    final case object Optional extends FieldDescriptorProtoLabel
    final case object Required extends FieldDescriptorProtoLabel
    final case object Repeated extends FieldDescriptorProtoLabel
  }

  final case class FileDescriptorProto(
      name: String,
      `package`: String,
      messageTypes: List[MessageDescriptorProto],
      enumTypes: List[EnumDescriptorProto],
      syntax: String
  )
}
