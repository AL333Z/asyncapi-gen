package asyncapigen

import asyncapigen.protobuf.schema.FieldDescriptorProto.{
  EnumFieldDescriptorProto,
  OneofDescriptorProto,
  PlainFieldDescriptorProto
}
import asyncapigen.protobuf.schema.FieldProtoType._
import asyncapigen.protobuf.schema._
import asyncapigen.schema.Schema.{BasicSchema, BasicSchemaValue, CustomFields, ObjectSchema, SumSchema}
import asyncapigen.schema.{AsyncApi, Message, Reference, Schema}
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

  def fromAsyncApi(asyncApi: AsyncApi): Try[List[FileDescriptorProto]] =
    // assuming 1 message schema per topic (see:https://docs.confluent.io/platform/current/schema-registry/serdes-develop/serdes-protobuf.html#multiple-event-types-in-the-same-topic)
    asyncApi.channels.toList
      .traverse { case (name, item) =>
        item.subscribe.toList
          .appendedAll(item.publish.toList)
          .flatTraverse(op => extractMessages(asyncApi, name, op.message, isRepeated = false).map(_.toList))
          .map(messages =>
            FileDescriptorProto(
              name = name.split('/').last, // TODO what to use?
              `package` = None,            //Some(s"org.demo.${name.replace('/', '.')}"), // TODO what to use?
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
            val messageName = message.name.getOrElse(name.split('/').last.capitalize) // TODO what to put here?
            extractMessageComponents(asyncApi, schema, isRepeated).map(components =>
              Some(
                MessageDescriptorProto(
                  name = messageName,
                  fields = components.fields,
                  nestedMessages = components.messages,
                  nestedEnums = components.enums,
                  options = Nil // TODO
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
    // TODO this is a strong and wrong assumption. Refs should be fully supported!
    val messageName = ref.value.split("#/components/messages/")(1)
    val message     = asyncApi.components.flatMap(_.messages.get(messageName))
    extractMessages(asyncApi, messageName, message, isRepeated)
  }

  private def extractMessageComponents(
      asyncApi: AsyncApi,
      schema: Schema,
      isRepeated: Boolean
  ): Try[MessageComponents] =
    schema match {
      case Schema.RefSchema(_) => ???
      case Schema.SumSchema(_) => ???
      case Schema.ObjectSchema(required, properties) =>
        extractFromObjectSchema(asyncApi, required, properties, isRepeated)
      case Schema.ArraySchema(_) => ???
      case Schema.EnumSchema(_)  => ???
      case _: Schema.BasicSchema => ???
    }

  private def extractFromObjectSchema(
      asyncApi: AsyncApi,
      required: List[String],
      properties: Map[String, ObjectSchema.Elem],
      isRep: Boolean
  ): Try[MessageComponents] = {
    @tailrec
    def go(
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
                  label = toFieldDescriptorProtoLabel(Some(required), fieldName, isRepeated),
                  options = Nil,
                  index = i,
                  messageProto = Some(message)
                )
              )
            )
            .map(xs =>
              acc.combine(
                MessageComponents(xs, Nil, xs.collect { case x if x.messageProto.isDefined => x.messageProto.get })
              )
            )
        )
      case Schema.SumSchema(oneOfs) =>
        val oneofDescriptorProto = toOneofDescriptorProto(asyncApi, required, fieldName, oneOfs)
        Success(acc.combine(oneofDescriptorProto))
      case Schema.ObjectSchema(required, properties) =>
        extractFromObjectSchema(asyncApi, required, properties, isRepeated).map(acc.combine)
      case Schema.ArraySchema(schema) =>
        go(acc, fieldName, schema, customFields, isRepeated = true)
      case Schema.EnumSchema(enum) =>
        val enumTypeName = uppercaseFirstLetter(fieldName)
        customFields.protoIndex.flatMap(i =>
          Try(NonEmptyList.fromListUnsafe(enum.zipWithIndex))
            .map(enumValues =>
              acc
                .appendField(
                  PlainFieldDescriptorProto(
                    name = fieldName,
                    `type` = NamedTypeProto(enumTypeName),
                    label = toFieldDescriptorProtoLabel(Some(required), fieldName, isRepeated),
                    options = Nil,
                    index = i
                  )
                )
                .appendEnum(EnumDescriptorProto(enumTypeName, enumValues))
            )
        )
      case bs: BasicSchema =>
        customFields.protoIndex.map(i =>
          acc.appendFields(toPlainFieldDescriptorProto(Some(required), fieldName, i, bs, isRepeated))
        )
    }

    properties.toList
      .foldLeftM[Try, MessageComponents](Monoid[MessageComponents].empty) { case (acc, (fieldName, elem)) =>
        go(acc, fieldName, elem.schema, elem.customFields, isRepeated = isRep)
      }
  }

  private def toOneofDescriptorProto(
      asyncApi: AsyncApi,
      required: List[String],
      fieldName: String,
      oneOfs: List[SumSchema.Elem]
  ): MessageComponents = { // TODO this should be a Try?
    val fields: List[Either[PlainFieldDescriptorProto, EnumFieldDescriptorProto]] = oneOfs.flatMap {
      case SumSchema.Elem(s, maybeName, customFields) =>
        val name = lowercaseFirstLetter(maybeName.getOrElse(fieldName))
        val i    = customFields.protoIndex.get // TODO
        s match {
          case Schema.RefSchema(ref) =>
            resolveMessageDescriptorProtoFromRef(asyncApi, ref, isRepeated = false).toOption.flatten.toList.map(
              message =>
                PlainFieldDescriptorProto(
                  name = lowercaseFirstLetter(message.name),
                  `type` = NamedTypeProto(message.name),
                  label = toFieldDescriptorProtoLabel(None, fieldName, isRepeated = false),
                  options = Nil,
                  index = i, // TODO how to handle indexes?
                  messageProto = Some(message)
                ).asLeft
            )
          case Schema.SumSchema(_)       => ??? // TODO not supported in protobuf?
          case Schema.ObjectSchema(_, _) => ??? // TODO not supported in protobuf?
          case Schema.ArraySchema(_)     => ??? // TODO not supported in protobuf?
          case Schema.EnumSchema(enum) =>
            List(
              EnumFieldDescriptorProto(
                name = name,
                enum = EnumDescriptorProto(
                  name = fieldName,
                  symbols = NonEmptyList.fromListUnsafe(enum.zipWithIndex)
                ),
                label = toFieldDescriptorProtoLabel(Some(required), fieldName, isRepeated = false),
                index = i
              ).asRight
            )
          case schema: BasicSchema =>
            toPlainFieldDescriptorProto(None, name, i, schema, isRepeated = false).map(_.asLeft)
        }
    }

    MessageComponents(
      fields = List(
        OneofDescriptorProto(
          name = fieldName,
          label = toFieldDescriptorProtoLabel(Some(required), fieldName, isRepeated = false),
          fields = fields
        )
      ),
      enums = fields.collect { case Right(e) => e.enum },
      messages = fields.collect { case Left(x) if x.messageProto.isDefined => x.messageProto.get }
    )
  }

  private def lowercaseFirstLetter(str: String): String =
    if (str.isEmpty) "" else s"${Character.toLowerCase(str.charAt(0))}${str.substring(1)}"

  private def uppercaseFirstLetter(str: String): String =
    if (str.isEmpty) "" else s"${Character.toUpperCase(str.charAt(0))}${str.substring(1)}"

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
      case BasicSchema.DateSchema     => StringProto // google/protobuf/timestamp.proto instead?
      case BasicSchema.DateTimeSchema => StringProto // google/protobuf/timestamp.proto instead?
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
    else if (required.isEmpty || required.get.contains(fieldName)) FieldDescriptorProtoLabel.Required
    else FieldDescriptorProtoLabel.Optional
  }

  implicit class CustomFieldsOps(val inner: CustomFields) {
    def protoIndex: Try[Int] = inner.inner.get("x-proto-index") match {
      case Some(BasicSchemaValue.IntegerValue(i)) => Success(i)
      case x                                      => Failure(new RuntimeException(s"Missing or invalid x-proto-index: $x"))
    }
  }
}
