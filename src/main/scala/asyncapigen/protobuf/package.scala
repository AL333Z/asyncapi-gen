package asyncapigen

import asyncapigen.protobuf.schema.FieldDescriptorProto.PlainFieldDescriptorProto
import asyncapigen.protobuf.schema.FieldProtoType._
import asyncapigen.protobuf.schema.{
  FieldDescriptorProto,
  FieldDescriptorProtoLabel,
  FileDescriptorProto,
  MessageDescriptorProto
}
import asyncapigen.schema.Schema.BasicSchema
import asyncapigen.schema.{AsyncApi, Message, Reference, Schema}
import cats.implicits._

import scala.util.{Success, Try}

package object protobuf {

  def fromAsyncApi(asyncApi: AsyncApi): Try[List[FileDescriptorProto]] =
    // assuming 1 message schema per topic (see:https://docs.confluent.io/platform/current/schema-registry/serdes-develop/serdes-protobuf.html#multiple-event-types-in-the-same-topic)
    asyncApi.channels
      .map { case (name, item) =>
        for {
          messages <- item.subscribe.toList
            .appendedAll(item.publish.toList)
            .traverse(op =>
              extractMessages(
                asyncApi,
                name,
                op.message
              )
            )
        } yield FileDescriptorProto(
          name = name.split('/').last, // TODO what to use?
          `package` = None,            //Some(s"org.demo.${name.replace('/', '.')}"), // TODO what to use?
          messageTypes = messages.flatten,
          enumTypes = Nil,
          syntax = "proto3"
        )
      }
      .toList
      .sequence

  private def extractMessages(
      asyncApi: AsyncApi,
      name: String,
      message: Option[Either[Message, Reference]]
  ): Try[List[MessageDescriptorProto]] =
    message match {
      case Some(Left(message)) =>
        message.payload match {
          case Left(schema) =>
            val messageName = message.name.getOrElse(name.split('/').last.capitalize) // TODO what to put here?
            Success(
              List(
                MessageDescriptorProto(
                  name = messageName,
                  fields = toFieldDescriptorProtos(schema),
                  nestedMessages = Nil, // TODO
                  nestedEnums = Nil,    // TODO
                  options = Nil         // TODO
                )
              )
            )
          case Right(ref) => resolveMessageDescriptorProtoFromRef(asyncApi, ref)
        }
      case Some(Right(ref)) => resolveMessageDescriptorProtoFromRef(asyncApi, ref)
      case None             => Success(Nil)
    }

  private def resolveMessageDescriptorProtoFromRef(
      asyncApi: AsyncApi,
      ref: Reference
  ): Try[List[MessageDescriptorProto]] = {
    // TODO this is a strong and wrong assumption. Refs should be fully supported!
    val messageName = ref.value.split("#/components/messages/")(1)
    val message     = asyncApi.components.flatMap(_.messages.get(messageName))
    extractMessages(asyncApi, messageName, message)
  }

  private def toFieldDescriptorProtos(schema: Schema): List[FieldDescriptorProto] =
    schema match {
      case Schema.RefSchema(_) => ???
      case Schema.SumSchema(_) => ???
      case Schema.ObjectSchema(required, properties) =>
        properties.zipWithIndex.toList // TODO understand how to keep track of field indexes
          .map { case ((fieldName, v), i) =>
            v match {
              case Schema.RefSchema(_)       => ???
              case Schema.SumSchema(_)       => ???
              case Schema.ObjectSchema(_, _) => ???
              case Schema.ArraySchema(_)     => ???
              case Schema.EnumSchema(_)      => ???
              case bs: Schema.BasicSchema =>
                val pbType = bs match {
                  case BasicSchema.IntegerSchema  => Int32Proto
                  case BasicSchema.LongSchema     => Int64Proto
                  case BasicSchema.FloatSchema    => FloatProto
                  case BasicSchema.DoubleSchema   => DoubleProto
                  case BasicSchema.ByteSchema     => BytesProto
                  case BasicSchema.BinarySchema   => Int32Proto
                  case BasicSchema.BooleanSchema  => BoolProto
                  case BasicSchema.DateSchema     => ??? // TODO what to use?
                  case BasicSchema.DateTimeSchema => ??? // TODO what to use?
                  case BasicSchema.PasswordSchema => StringProto
                  case BasicSchema.StringSchema   => StringProto
                }
                PlainFieldDescriptorProto(
                  fieldName,
                  pbType,
                  toFieldDescriptorProtoLabel(required, fieldName),
                  Nil,
                  i + 1
                )
            }
          }
      case Schema.ArraySchema(_) => ???
      case Schema.EnumSchema(_)  => ???
      case _: Schema.BasicSchema => ???
    }

  private def toFieldDescriptorProtoLabel(required: List[String], fieldName: String): FieldDescriptorProtoLabel =
    if (required.contains(fieldName)) FieldDescriptorProtoLabel.Required
    else FieldDescriptorProtoLabel.Optional
}
