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

  sealed abstract class FieldDescriptorProto {
    def repeated: FieldDescriptorProto
  }
  object FieldDescriptorProto {
    final case class EnumFieldDescriptorProto(
        name: String,
        enumeration: EnumDescriptorProto,
        label: FieldDescriptorProtoLabel,
        index: Int
    ) extends FieldDescriptorProto {
      override def repeated: EnumFieldDescriptorProto =
        copy(label = FieldDescriptorProtoLabel.Repeated)
    }

    final case class OneofDescriptorProto(
        name: String,
        label: FieldDescriptorProtoLabel,
        fields: List[Either[PlainFieldDescriptorProto, EnumFieldDescriptorProto]]
    ) extends FieldDescriptorProto {
      override def repeated: OneofDescriptorProto =
        copy(label = FieldDescriptorProtoLabel.Repeated)
    }

    case class PlainFieldDescriptorProto(
        name: String,
        `type`: FieldProtoType,
        label: FieldDescriptorProtoLabel,
        options: List[OptionValue],
        index: Int,
        messageProto: Option[MessageDescriptorProto] = None
    ) extends FieldDescriptorProto {
      override def repeated: PlainFieldDescriptorProto =
        copy(label = FieldDescriptorProtoLabel.Repeated)
    }
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
