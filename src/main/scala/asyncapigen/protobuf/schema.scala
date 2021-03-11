package asyncapigen.protobuf

import asyncapigen.protobuf.schema.FieldProtoType.{NamedTypeProto, ScalarValueProtoType}
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
        typeName: NamedTypeProto,
        index: Int
    ) extends FieldDescriptorProto

    final case class OneofDescriptorProto(
        name: String,
        fields: List[PlainFieldDescriptorProto] // TODO not sure this is correct
    ) extends FieldDescriptorProto

    case class PlainFieldDescriptorProto(
        name: String,
        `type`: ScalarValueProtoType,
        label: FieldDescriptorProtoLabel,
        options: List[OptionValue],
        index: Int
    ) extends FieldDescriptorProto
  }

  sealed abstract class FieldProtoType
  object FieldProtoType {
    sealed abstract class ScalarValueProtoType extends FieldProtoType

    final case object NullProto                   extends ScalarValueProtoType
    final case object DoubleProto                 extends ScalarValueProtoType
    final case object FloatProto                  extends ScalarValueProtoType
    final case object Int32Proto                  extends ScalarValueProtoType
    final case object Int64Proto                  extends ScalarValueProtoType
    final case object Uint32Proto                 extends ScalarValueProtoType
    final case object Uint64Proto                 extends ScalarValueProtoType
    final case object Sint32Proto                 extends ScalarValueProtoType
    final case object Sint64Proto                 extends ScalarValueProtoType
    final case object Fixed32Proto                extends ScalarValueProtoType
    final case object Fixed64Proto                extends ScalarValueProtoType
    final case object Sfixed32Proto               extends ScalarValueProtoType
    final case object Sfixed64Proto               extends ScalarValueProtoType
    final case object BoolProto                   extends ScalarValueProtoType
    final case object StringProto                 extends ScalarValueProtoType
    final case object BytesProto                  extends ScalarValueProtoType
    final case class NamedTypeProto(name: String) extends FieldProtoType
    final case object ObjectProto                 extends FieldProtoType
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
