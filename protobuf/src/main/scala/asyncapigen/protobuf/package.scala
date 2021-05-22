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

import asyncapigen.protobuf.schema.FieldDescriptorProto.{
  EnumFieldDescriptorProto,
  OneofDescriptorProto,
  PlainFieldDescriptorProto
}
import asyncapigen.protobuf.schema.FieldProtoType._
import asyncapigen.protobuf.schema._
import asyncapigen.schema.Schema._
import asyncapigen.schema.{AsyncApi, Message, Reference, RichString, Schema}
import cats.data.NonEmptyList
import cats.implicits._
import cats.kernel.Monoid

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

private case class MessageComponents(
    fields: List[FieldDescriptorProto],
    enums: List[EnumDescriptorProto],
    messages: List[MessageDescriptorProto]
) {
  def append(x: MessageComponents): MessageComponents =
    copy(fields = fields ++ x.fields, enums = enums ++ x.enums, messages = messages ++ x.messages)
  def appendFields(xs: List[FieldDescriptorProto]): MessageComponents =
    copy(fields = fields ++ xs)
  def appendField(x: FieldDescriptorProto): MessageComponents =
    copy(fields = fields :+ x)
  def appendEnums(xs: List[EnumDescriptorProto]): MessageComponents =
    copy(enums = enums ++ xs)
  def appendEnum(x: EnumDescriptorProto): MessageComponents =
    copy(enums = enums :+ x)
}
private object MessageComponents {
  implicit val messageComponentsMonoid: Monoid[MessageComponents] =
    Monoid.instance[MessageComponents](MessageComponents(Nil, Nil, Nil), (x, y) => x.append(y))
}

package object protobuf {
  def fromAsyncApi(asyncApi: AsyncApi, `package`: String): Try[List[FileDescriptorProto]] =
    // assuming 1 message schema per topic (see:https://docs.confluent.io/platform/current/schema-registry/serdes-develop/serdes-protobuf.html#multiple-event-types-in-the-same-topic)
    asyncApi.channels.toList
      .traverse { case (name, item) =>
        item.subscribe.toList
          .appendedAll(item.publish.toList)
          .flatTraverse(op => extractMessages(asyncApi, name, op.message, isRepeated = false).map(_.toList))
          .map(messages =>
            FileDescriptorProto(
              name = name.toJavaClassCompatible,
              `package` = Some(`package`),
              messageTypes = messages,
              enumTypes = Nil,
              syntax = "proto3"
            )
          )
      }

  private def extractMessages(
      asyncApi: AsyncApi,
      name: String,
      message: Option[Either[Message, Reference]],
      isRepeated: Boolean
  ): Try[Option[MessageDescriptorProto]] =
    message match {
      case Some(Left(message)) =>
        message.payload match {
          case Left(schema) =>
            val messageName = message.name.getOrElse(name)
            extractMessageComponents(asyncApi, schema, isRepeated).map(components =>
              Some(
                MessageDescriptorProto(
                  name = messageName.toJavaClassCompatible,
                  fields = components.fields,
                  nestedMessages = components.messages,
                  nestedEnums = components.enums,
                  options = Nil
                )
              )
            )
          case Right(ref) => resolveMessageDescriptorProtoFromRef(asyncApi, ref, isRepeated)
        }
      case Some(Right(ref)) => resolveMessageDescriptorProtoFromRef(asyncApi, ref, isRepeated)
      case _                => Success(None)
    }

  private def resolveMessageDescriptorProtoFromRef(
      asyncApi: AsyncApi,
      ref: Reference,
      isRepeated: Boolean
  ): Try[Option[MessageDescriptorProto]] = {
    val message = asyncApi.resolveMessageFromRef(ref)
    extractMessages(asyncApi, message._1, message._2, isRepeated)
  }

  private def extractMessageComponents(
      asyncApi: AsyncApi,
      schema: Schema,
      isRepeated: Boolean
  ): Try[MessageComponents] =
    schema match {
      case Schema.ObjectSchema(required, properties) =>
        extractFromObjectSchema(asyncApi, required, properties, isRepeated)
      case Schema.ArraySchema(items) =>
        recurseFieldComponents(
          asyncApi = asyncApi,
          required = None,
          acc = Monoid[MessageComponents].empty,
          fieldName = "items",
          schema = items,
          customFields = CustomFields.withDefaultProtobufIndex,
          isRepeated = true
        )
      case basicSchema: Schema.BasicSchema =>
        recurseFieldComponents(
          asyncApi = asyncApi,
          required = None,
          acc = Monoid[MessageComponents].empty,
          fieldName = "value",
          schema = basicSchema,
          customFields = CustomFields.withDefaultProtobufIndex,
          isRepeated = false
        )
      case sum: Schema.SumSchema =>
        recurseFieldComponents(
          asyncApi = asyncApi,
          required = None,
          acc = Monoid[MessageComponents].empty,
          fieldName = "kind",
          schema = sum,
          customFields = CustomFields.withDefaultProtobufIndex,
          isRepeated = false
        )
      case enum: Schema.EnumSchema =>
        recurseFieldComponents(
          asyncApi = asyncApi,
          required = None,
          acc = Monoid[MessageComponents].empty,
          fieldName = "values",
          schema = enum,
          customFields = CustomFields.withDefaultProtobufIndex,
          isRepeated = false
        )
      case x => Failure(new RuntimeException(s"Inconceivable: schema not supported: $x."))
    }

  @tailrec
  private def recurseFieldComponents(
      asyncApi: AsyncApi,
      required: Option[List[String]],
      acc: MessageComponents,
      fieldName: String,
      schema: Schema,
      customFields: CustomFields,
      isRepeated: Boolean
  ): Try[MessageComponents] = schema match {
    case Schema.RefSchema(ref) =>
      customFields.protoIndex.flatMap(i =>
        resolveMessageDescriptorProtoFromRef(asyncApi, ref, isRepeated)
          .map(
            _.toList.map(message =>
              PlainFieldDescriptorProto(
                name = fieldName,
                `type` = NamedTypeProto(message.name),
                label = toFieldDescriptorProtoLabel(required, fieldName, isRepeated),
                options = Nil,
                index = i,
                messageProto = Some(message)
              )
            )
          )
          .map(xs =>
            acc.combine(
              MessageComponents(
                fields = xs,
                enums = Nil,
                messages = xs.collect { case PlainFieldDescriptorProto(_, _, _, _, _, Some(msgProto)) => msgProto }
              )
            )
          )
      )
    case Schema.SumSchema(oneOfs) =>
      val oneofDescriptorProto = toOneofDescriptorProto(asyncApi, required, fieldName, oneOfs)
      oneofDescriptorProto.map(acc.combine)
    case Schema.ObjectSchema(required, properties) =>
      extractFromObjectSchema(asyncApi, required, properties, isRepeated).map(acc.combine)
    case Schema.ArraySchema(schema) =>
      recurseFieldComponents(asyncApi, required, acc, fieldName, schema, customFields, isRepeated = true)
    case Schema.EnumSchema(enum) =>
      val enumTypeName = fieldName.uppercaseFirstLetter
      customFields.protoIndex.flatMap(i =>
        Try(NonEmptyList.fromListUnsafe(enum.zipWithIndex))
          .map(enumValues =>
            acc
              .appendField(
                PlainFieldDescriptorProto(
                  name = fieldName,
                  `type` = NamedTypeProto(enumTypeName),
                  label = toFieldDescriptorProtoLabel(required, fieldName, isRepeated),
                  options = Nil,
                  index = i
                )
              )
              .appendEnum(EnumDescriptorProto(enumTypeName, enumValues))
          )
      )
    case bs: BasicSchema =>
      customFields.protoIndex.map(i =>
        acc.appendFields(toPlainFieldDescriptorProto(required, fieldName, i, bs, isRepeated))
      )
  }

  private def extractFromObjectSchema(
      asyncApi: AsyncApi,
      required: List[String],
      properties: Map[String, ObjectSchema.Elem],
      isRep: Boolean
  ): Try[MessageComponents] = {
    properties.toList
      .foldLeftM[Try, MessageComponents](Monoid[MessageComponents].empty) { case (acc, (fieldName, elem)) =>
        recurseFieldComponents(
          asyncApi = asyncApi,
          required = Some(required),
          acc = acc,
          fieldName = fieldName,
          schema = elem.schema,
          customFields = elem.customFields,
          isRepeated = isRep
        )
      }
  }

  private def toOneofDescriptorProto(
      asyncApi: AsyncApi,
      required: Option[List[String]],
      fieldName: String,
      oneOfs: List[SumSchema.Elem]
  ): Try[MessageComponents] =
    oneOfs
      .flatTraverse { elem =>
        val name = elem.name.getOrElse(fieldName).lowercaseFirstLetter
        elem.customFields.protoIndex.flatMap { i =>
          elem.schema match {
            case Schema.RefSchema(ref) =>
              resolveMessageDescriptorProtoFromRef(asyncApi, ref, isRepeated = false).map(
                _.toList.map(message =>
                  PlainFieldDescriptorProto(
                    name = message.name.lowercaseFirstLetter,
                    `type` = NamedTypeProto(message.name),
                    label = toFieldDescriptorProtoLabel(None, fieldName, isRepeated = false),
                    options = Nil,
                    index = i,
                    messageProto = Some(message)
                  ).asLeft
                )
              )
            case Schema.EnumSchema(enum) =>
              Try(NonEmptyList.fromListUnsafe(enum))
                .map(enumValues =>
                  List(
                    EnumFieldDescriptorProto(
                      name = name,
                      enum = EnumDescriptorProto(
                        name = fieldName,
                        symbols = enumValues.zipWithIndex
                      ),
                      label = toFieldDescriptorProtoLabel(required, fieldName, isRepeated = false),
                      index = i
                    ).asRight
                  )
                )
            case schema: BasicSchema =>
              Success(toPlainFieldDescriptorProto(None, name, i, schema, isRepeated = false).map(_.asLeft))
            case x => Failure(new RuntimeException(s"Schema not supported in protobuf oneof: $x"))
          }
        }
      }
      .map(fields =>
        MessageComponents(
          fields = List(
            OneofDescriptorProto(
              name = fieldName,
              label = toFieldDescriptorProtoLabel(required, fieldName, isRepeated = false),
              fields = fields
            )
          ),
          enums = fields.collect { case Right(e) => e.enum },
          messages = fields.collect { case Left(PlainFieldDescriptorProto(_, _, _, _, _, Some(msgProto))) => msgProto }
        )
      )

  private def toPlainFieldDescriptorProto(
      required: Option[List[String]],
      fieldName: String,
      i: Int,
      bs: BasicSchema,
      isRepeated: Boolean
  ): List[PlainFieldDescriptorProto] = {
    val pbType = bs match {
      case BasicSchema.IntegerSchema  => Int32Proto
      case BasicSchema.LongSchema     => Int64Proto
      case BasicSchema.FloatSchema    => FloatProto
      case BasicSchema.DoubleSchema   => DoubleProto
      case BasicSchema.ByteSchema     => BytesProto
      case BasicSchema.BinarySchema   => Int32Proto
      case BasicSchema.BooleanSchema  => BoolProto
      case BasicSchema.DateSchema     => StringProto // TODO google/protobuf/timestamp.proto instead? or what else?
      case BasicSchema.DateTimeSchema => StringProto // TODO google/protobuf/timestamp.proto instead? or what else?
      case BasicSchema.PasswordSchema => StringProto
      case BasicSchema.UUIDSchema     => StringProto
      case BasicSchema.StringSchema   => StringProto
    }
    List(
      PlainFieldDescriptorProto(
        name = fieldName,
        `type` = pbType,
        label = toFieldDescriptorProtoLabel(required, fieldName, isRepeated),
        options = Nil,
        index = i
      )
    )
  }

  private def toFieldDescriptorProtoLabel(
      required: Option[List[String]],
      fieldName: String,
      isRepeated: Boolean
  ): FieldDescriptorProtoLabel = {
    if (isRepeated) FieldDescriptorProtoLabel.Repeated
    else if (required.isEmpty || required.exists(_.contains(fieldName))) FieldDescriptorProtoLabel.Required
    else FieldDescriptorProtoLabel.Optional
  }

  implicit class CustomFieldsOps(val inner: CustomFields) {
    def protoIndex: Try[Int] = {
      inner.inner.get(CustomFields.`x-protobuf-index`) match {
        case Some(BasicSchemaValue.IntegerValue(i)) => Success(i)
        case x                                      => Failure(new RuntimeException(s"Missing or invalid ${CustomFields.`x-protobuf-index`}: $x"))
      }
    }
  }

  implicit class CustomFieldsTypeOps(val inner: CustomFields.type) {
    val `x-protobuf-index` = "x-protobuf-index"

    val withDefaultProtobufIndex: CustomFields = CustomFields(
      Map(`x-protobuf-index` -> BasicSchemaValue.IntegerValue(1))
    )
  }
}
