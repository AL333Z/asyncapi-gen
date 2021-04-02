package asyncapigen

import asyncapigen.protobuf.schema.FieldDescriptorProto.{
  EnumFieldDescriptorProto,
  OneofDescriptorProto,
  PlainFieldDescriptorProto
}
import asyncapigen.protobuf.schema.FieldProtoType._
import asyncapigen.protobuf.schema._
import asyncapigen.schema.Schema.BasicSchema
import asyncapigen.schema.{AsyncApi, Message, Reference, Schema}
import cats.data.NonEmptyList
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
            .flatTraverse(op =>
              extractMessages(
                asyncApi,
                name,
                op.message
              ).map(_.toList)
            )
          messagesFromRefs <- asyncApi.components.toList
            .flatMap(_.messages.map { case (name, item) =>
              extractMessages(asyncApi, name, Some(item)).map(_.toList)
            })
            .flatSequence[Try, MessageDescriptorProto]
        } yield FileDescriptorProto(
          name = name.split('/').last, // TODO what to use?
          `package` = None,            //Some(s"org.demo.${name.replace('/', '.')}"), // TODO what to use?
          messageTypes = messages ++ messagesFromRefs,
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
  ): Try[Option[MessageDescriptorProto]] =
    message match {
      case Some(Left(message)) =>
        message.payload match {
          case Left(schema) =>
            val messageName = message.name.getOrElse(name.split('/').last.capitalize) // TODO what to put here?
            toFieldDescriptorProtos(asyncApi, schema).map(x =>
              Some(
                MessageDescriptorProto(
                  name = messageName,
                  fields = x._1,
                  nestedMessages = Nil, // TODO
                  nestedEnums = x._2,
                  options = Nil // TODO
                )
              )
            )
          case Right(ref) => resolveMessageDescriptorProtoFromRef(asyncApi, ref)
        }
      case _ => Success(None)
    }

  private def resolveMessageDescriptorProtoFromRef(
      asyncApi: AsyncApi,
      ref: Reference
  ): Try[Option[MessageDescriptorProto]] = {
    // TODO this is a strong and wrong assumption. Refs should be fully supported!
    val messageName = ref.value.split("#/components/messages/")(1)
    val message     = asyncApi.components.flatMap(_.messages.get(messageName))
    extractMessages(asyncApi, messageName, message)
  }

  private def toFieldDescriptorProtos(
      asyncApi: AsyncApi,
      schema: Schema
  ): Try[(List[FieldDescriptorProto], List[EnumDescriptorProto])] =
    schema match {
      case Schema.RefSchema(_)                       => ???
      case Schema.SumSchema(_)                       => ???
      case Schema.ObjectSchema(required, properties) => extractFromObjectSchema(asyncApi, required, properties)
      case Schema.ArraySchema(_)                     => ???
      case Schema.EnumSchema(_)                      => ???
      case _: Schema.BasicSchema                     => ???
    }

  private def extractFromObjectSchema(
      asyncApi: AsyncApi,
      required: List[String],
      properties: Map[String, Schema]
  ): Try[(List[FieldDescriptorProto], List[EnumDescriptorProto])] = {
    properties.zipWithIndex.toList // TODO understand how to keep track of field indexes
      .foldLeftM[Try, (List[FieldDescriptorProto], List[EnumDescriptorProto])]((Nil, Nil)) {
        case ((fieldsAcc, enumsAcc), ((fieldName, v), i)) =>
          v match {
            case Schema.RefSchema(ref) =>
              val value: Try[List[FieldDescriptorProto]] =
                resolveMessageDescriptorProtoFromRef(asyncApi, ref).map(_.toList.flatMap(_.fields))
              value.map(x => (fieldsAcc ++ x, enumsAcc))
            case Schema.SumSchema(oneOfs) =>
              Success(
                (fieldsAcc :+ toOneofDescriptorProto(asyncApi, required, fieldName, oneOfs.zipWithIndex), enumsAcc)
              ) // TODO indexes!
            case Schema.ObjectSchema(required, properties) =>
              extractFromObjectSchema(asyncApi, required, properties).map { case (fields, enums) =>
                (fieldsAcc ++ fields, enumsAcc ++ enums)
              }
            case Schema.ArraySchema(_) => ???
            case Schema.EnumSchema(enum) =>
              val enumTypeName = uppercaseFirstLetter(fieldName)
              Success(
                (
                  fieldsAcc :+ PlainFieldDescriptorProto(
                    name = fieldName,
                    `type` = NamedTypeProto(enumTypeName),
                    label = toFieldDescriptorProtoLabel(required, fieldName),
                    options = Nil,
                    index = i + 1 // TODO how to handle indexes?
                  ),
                  enumsAcc :+ EnumDescriptorProto(enumTypeName, NonEmptyList.fromListUnsafe(enum.zipWithIndex))
                )
              )
            case bs: BasicSchema =>
              Success((fieldsAcc ++ toPlainFieldDescriptorProto(required, fieldName, i, bs), enumsAcc))
          }
      }
  }

  private def toOneofDescriptorProto(
      asyncApi: AsyncApi,
      required: List[String],
      fieldName: String,
      oneOfs: List[(Schema, Int)]
  ): OneofDescriptorProto =
    OneofDescriptorProto(
      name = fieldName,
      label = toFieldDescriptorProtoLabel(required, fieldName),
      fields = oneOfs.flatMap { case (s, i) =>
        s match {
          case Schema.RefSchema(ref) =>
            val x = resolveMessageDescriptorProtoFromRef(asyncApi, ref).get.head // TODO
            List(
              PlainFieldDescriptorProto(
                name = lowercaseFirstLetter(x.name),
                `type` = NamedTypeProto(x.name),
                label = toFieldDescriptorProtoLabel(required, fieldName),
                options = Nil,
                index = i + 1 // TODO how to handle indexes?
              ).asLeft
            )
          case Schema.SumSchema(_)       => ??? // TODO not supported in protobuf?
          case Schema.ObjectSchema(_, _) => ??? // TODO not supported in protobuf?
          case Schema.ArraySchema(_)     => ??? // TODO not supported in protobuf?
          case Schema.EnumSchema(enum) =>
            List(
              EnumFieldDescriptorProto(
                name = fieldName,
                enum = EnumDescriptorProto(
                  name = fieldName,
                  symbols = NonEmptyList.fromListUnsafe(enum.zipWithIndex)
                ),
                label = toFieldDescriptorProtoLabel(required, fieldName),
                index = i
              ).asRight
            )
          case schema: BasicSchema =>
            toPlainFieldDescriptorProto(required, fieldName, i, schema).map(_.asLeft)
        }
      }
    )

  private def lowercaseFirstLetter(str: String): String =
    s"${Character.toLowerCase(str.charAt(0))}${str.substring(1)}"

  private def uppercaseFirstLetter(str: String): String =
    s"${Character.toUpperCase(str.charAt(0))}${str.substring(1)}"

  private def toPlainFieldDescriptorProto(
      required: List[String],
      fieldName: String,
      i: Int,
      bs: BasicSchema
  ): List[PlainFieldDescriptorProto] = {
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
    List(
      PlainFieldDescriptorProto(
        name = fieldName,
        `type` = pbType,
        label = toFieldDescriptorProtoLabel(required, fieldName),
        options = Nil,
        index = i + 1 // TODO how to handle indexes?
      )
    )
  }

  private def toFieldDescriptorProtoLabel(required: List[String], fieldName: String): FieldDescriptorProtoLabel =
    if (required.contains(fieldName)) FieldDescriptorProtoLabel.Required
    else FieldDescriptorProtoLabel.Optional
}
