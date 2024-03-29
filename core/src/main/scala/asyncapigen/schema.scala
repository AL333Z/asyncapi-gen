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

package asyncapigen

import asyncapigen.schema.Schema.BasicSchema._
import asyncapigen.schema.Schema.{BasicSchemaValue, ObjectSchema, _}
import cats.implicits._
import cats.kernel.Eq
import io.circe._

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

/**
 * @see https://www.asyncapi.com/docs/specifications/2.0.0
 */
object schema {
  object AsyncApi {
    implicit def openApiEq[T]: Eq[AsyncApi] =
      Eq.fromUniversalEquals
  }

  final case class AsyncApi(
      asyncapi: String,
      id: Option[String],
      info: Info,
      servers: List[Server],
      channels: Map[String, Channel.ItemObject],
      components: Option[Components],
      tags: List[Tag],
      externalDocs: Option[ExternalDocs]
  ) {
    // TODO this is a strong and wrong assumption. Refs should be fully supported!
    def resolveMessageFromRef(ref: Reference): (String, Option[Either[Message, Reference]]) = {
      val messageName = ref.value.split("#/components/messages/")(1)
      (messageName, components.flatMap(_.messages.get(messageName)))
    }
  }

  final case class Info(title: String, description: Option[String], version: String)

  final case class Server(
      url: String,
      protocol: String,
      protocolVersion: Option[String],
      description: Option[String],
      variables: Map[String, Server.Variable]
  )

  object Server {
    final case class Variable(`enum`: List[String], default: String, description: Option[String])
  }

  final case class Reference(value: String)

  object Channel {
    final case class ItemObject(
        ref: Option[String],
        description: Option[String],
        subscribe: Option[Operation],
        publish: Option[Operation],
        parameters: List[Either[Parameter, Reference]]
    )

    final case class Operation(
        operationId: Option[String],
        summary: Option[String],
        description: Option[String],
        tags: List[String],
        externalDocs: Option[ExternalDocs],
        message: Option[Either[Message, Reference]]
    )
  }
  final case class Components(
      schemas: Map[String, Schema],
      messages: Map[String, Either[Message, Reference]],
      parameters: Map[String, Either[Parameter, Reference]]
  )

  final case class Message(
      payload: Either[Schema, Reference],
      tags: List[String],
      name: Option[String],
      description: Option[String],
      bindings: Option[Message.Bindings]
  )

  object Message {
    case class Bindings(kafka: Option[Bindings.KafkaBinding])
    object Bindings {
      case class KafkaBinding(key: BasicSchema) // TODO support all `Schema`s, not just `BasicSchema`s
    }
  }

  final case class Tag(name: String, description: Option[String], externalDocs: Option[ExternalDocs])

  final case class ExternalDocs(url: String, description: Option[String])

  final case class Header(description: String, schema: Schema)

  final case class Parameter(description: Option[String], schema: Schema, location: String)

  sealed abstract class Schema extends Product with Serializable

  object Schema {
    final case class RefSchema(ref: Reference)                                                        extends Schema
    final case class ObjectSchema(required: List[String], properties: Map[String, ObjectSchema.Elem]) extends Schema
    final case class ArraySchema(items: Schema)                                                       extends Schema
    final case class EnumSchema(`enum`: List[String])                                                 extends Schema
    final case class SumSchema(oneOf: List[SumSchema.Elem])                                           extends Schema

    final case class CustomFields(inner: Map[String, BasicSchemaValue]) extends AnyVal
    object CustomFields {
      val empty: CustomFields = CustomFields(Map.empty)
    }

    object ObjectSchema {
      final case class Elem(schema: Schema, customFields: CustomFields = CustomFields.empty)
    }

    object SumSchema {
      final case class Elem(schema: Schema, name: Option[String], customFields: CustomFields = CustomFields.empty)
    }

    sealed abstract class BasicSchema extends Schema with Product with Serializable
    object BasicSchema {
      final case object IntegerSchema  extends BasicSchema
      final case object LongSchema     extends BasicSchema
      final case object FloatSchema    extends BasicSchema
      final case object DoubleSchema   extends BasicSchema
      final case object ByteSchema     extends BasicSchema
      final case object BinarySchema   extends BasicSchema
      final case object BooleanSchema  extends BasicSchema
      final case object DateSchema     extends BasicSchema
      final case object DateTimeSchema extends BasicSchema
      final case object PasswordSchema extends BasicSchema
      final case object UUIDSchema     extends BasicSchema
      final case object StringSchema   extends BasicSchema
    }

    sealed abstract class BasicSchemaValue extends Product with Serializable
    object BasicSchemaValue {
      final case class IntegerValue(value: Int)            extends BasicSchemaValue
      final case class LongValue(value: Long)              extends BasicSchemaValue
      final case class FloatValue(value: Float)            extends BasicSchemaValue
      final case class DoubleValue(value: Double)          extends BasicSchemaValue
      final case class ByteValue(value: Byte)              extends BasicSchemaValue
      final case class BinaryValue(value: Array[Byte])     extends BasicSchemaValue
      final case class BooleanValue(value: Boolean)        extends BasicSchemaValue
      final case class DateValue(value: LocalDate)         extends BasicSchemaValue
      final case class DateTimeValue(value: LocalDateTime) extends BasicSchemaValue
      final case class PasswordValue(value: String)        extends BasicSchemaValue
      final case class UUIDValue(value: UUID)              extends BasicSchemaValue
      final case class StringValue(value: String)          extends BasicSchemaValue
    }
  }

  implicit val referenceDecoder: Decoder[Reference] = Decoder.forProduct1(s"$$ref")(Reference.apply)

  private val refSchemaDecoder: Decoder[RefSchema] =
    Decoder[Reference].map(RefSchema)

  private val customFieldsDecoder: Decoder[CustomFields] = Decoder
    .decodeOption(
      Decoder.decodeMap[String, BasicSchemaValue](
        KeyDecoder.decodeKeyString,
        basicSchemaDecoder.flatMap[BasicSchemaValue] { basicSchema =>
          val basicSchemaValueDecoder: Decoder[BasicSchemaValue] = basicSchema match {
            case BasicSchema.IntegerSchema  => Decoder.decodeInt.map(BasicSchemaValue.IntegerValue)
            case BasicSchema.LongSchema     => Decoder.decodeLong.map(BasicSchemaValue.LongValue)
            case BasicSchema.FloatSchema    => Decoder.decodeFloat.map(BasicSchemaValue.FloatValue)
            case BasicSchema.DoubleSchema   => Decoder.decodeDouble.map(BasicSchemaValue.DoubleValue)
            case BasicSchema.ByteSchema     => Decoder.decodeByte.map(BasicSchemaValue.ByteValue)
            case BasicSchema.BinarySchema   => Decoder.decodeArray[Byte].map(BasicSchemaValue.BinaryValue)
            case BasicSchema.BooleanSchema  => Decoder.decodeBoolean.map(BasicSchemaValue.BooleanValue)
            case BasicSchema.DateSchema     => Decoder.decodeLocalDate.map(BasicSchemaValue.DateValue)
            case BasicSchema.DateTimeSchema => Decoder.decodeLocalDateTime.map(BasicSchemaValue.DateTimeValue)
            case BasicSchema.PasswordSchema => Decoder.decodeString.map(BasicSchemaValue.StringValue)
            case BasicSchema.UUIDSchema     => Decoder.decodeUUID.map(BasicSchemaValue.UUIDValue)
            case BasicSchema.StringSchema   => Decoder.decodeString.map(BasicSchemaValue.StringValue)
          }
          basicSchemaValueDecoder.at("value")
        }
      )
    )
    .map(_.getOrElse(Map.empty))
    .at("x-custom-attributes")
    .map(CustomFields.apply)

  implicit val sumSchemaElemDecoder: Decoder[SumSchema.Elem] =
    (Decoder[Schema], Decoder[Option[String]].at("name"), customFieldsDecoder).mapN(SumSchema.Elem.apply)

  private val sumSchemaDecoder: Decoder[SumSchema] =
    Decoder.instance(_.downField("oneOf").as[List[SumSchema.Elem]].map(SumSchema.apply))

  implicit val objectSchemaElemDecoder: Decoder[ObjectSchema.Elem] =
    (schemaDecoder, customFieldsDecoder).mapN(ObjectSchema.Elem)

  private val objectSchemaDecoder: Decoder[ObjectSchema] =
    Decoder.instance { c =>
      def propertyExists(name: String): Decoder.Result[Unit] =
        c.downField(name)
          .success
          .fold(DecodingFailure(s"$name property does not exist", c.history).asLeft[Unit])(_ =>
            ().asRight[DecodingFailure]
          )
      def isObject: Decoder.Result[Unit] =
        validateType(c, "object") orElse
          propertyExists("properties") orElse
          propertyExists("allOf")
      for {
        _        <- isObject
        required <- c.downField("required").as[Option[List[String]]]
        properties <-
          c.downField("properties")
            .as[Option[Map[String, ObjectSchema.Elem]]](
              Decoder.decodeOption(
                Decoder.decodeMap[String, ObjectSchema.Elem](KeyDecoder.decodeKeyString, objectSchemaElemDecoder)
              )
            )
            .map(_.getOrElse(Map.empty))
      } yield ObjectSchema(required.getOrElse(List.empty), properties)
    }

  private val arraySchemaDecoder: Decoder[ArraySchema] =
    Decoder.instance { c =>
      for {
        items <- c.downField("items").as[Schema]
      } yield ArraySchema(items)
    }

  private val enumJsonSchemaDecoder: Decoder[EnumSchema] =
    Decoder.instance(c =>
      for {
        values <- c.downField("enum").as[List[String]]
        _      <- validateType(c, "string")
      } yield EnumSchema(values)
    )

  private def basicSchemaDecoder: Decoder[BasicSchema] =
    Decoder.forProduct2[(String, Option[String]), String, Option[String]]("type", "format")(Tuple2.apply).emap {
      case ("integer", Some("int32"))    => IntegerSchema.asRight
      case ("integer", Some("int64"))    => LongSchema.asRight
      case ("integer", _)                => IntegerSchema.asRight
      case ("number", Some("float"))     => FloatSchema.asRight
      case ("number", Some("double"))    => DoubleSchema.asRight
      case ("number", _)                 => FloatSchema.asRight
      case ("string", Some("byte"))      => ByteSchema.asRight
      case ("string", Some("binary"))    => BinarySchema.asRight
      case ("boolean", _)                => BooleanSchema.asRight
      case ("string", Some("date"))      => DateSchema.asRight
      case ("string", Some("date-time")) => DateTimeSchema.asRight
      case ("string", Some("password"))  => PasswordSchema.asRight
      case ("string", Some("uuid"))      => UUIDSchema.asRight
      case ("string", _)                 => StringSchema.asRight
      case (x, _)                        => s"$x is not well formed type".asLeft
    }

  implicit def schemaDecoder: Decoder[Schema] =
    refSchemaDecoder.widen[Schema] orElse
      sumSchemaDecoder.widen[Schema] orElse
      objectSchemaDecoder.widen[Schema] orElse
      arraySchemaDecoder.widen[Schema] orElse
      enumJsonSchemaDecoder.widen[Schema] orElse
      basicSchemaDecoder.widen[Schema]

  private def validateType(c: HCursor, expected: String): Decoder.Result[Unit] =
    c.downField("type").as[String].flatMap {
      case `expected` => ().asRight
      case actual     => DecodingFailure(s"$actual is not expected type $expected", c.history).asLeft
    }

  implicit val externalDocsDecoder: Decoder[ExternalDocs] =
    Decoder.forProduct2(
      "url",
      "description"
    )(ExternalDocs.apply)

  implicit val tagDecoder: Decoder[Tag] =
    Decoder.forProduct3(
      "name",
      "description",
      "externalDocs"
    )(Tag.apply)

  implicit val eitherSchemaReferenceDecoder: Decoder[Either[Schema, Reference]] =
    Decoder[Schema].either(Decoder[Reference])

  implicit val kafkaMessageBindingsDecoder: Decoder[Message.Bindings.KafkaBinding] =
    Decoder.forProduct1("key")(Message.Bindings.KafkaBinding)(basicSchemaDecoder)

  implicit val messageBindingsDecoder: Decoder[Message.Bindings] =
    Decoder.forProduct1("kafka")(Message.Bindings(_))

  implicit val messageDecoder: Decoder[Message] =
    Decoder.forProduct5(
      "payload",
      "name",
      "description",
      "tags",
      "bindings"
    )(
      (
          payload: Either[Schema, Reference],
          name: Option[String],
          description: Option[String],
          tags: Option[List[String]],
          bindings: Option[Message.Bindings]
      ) =>
        Message(
          payload = payload,
          name = name,
          description = description,
          tags = tags.getOrElse(List.empty),
          bindings = bindings
        )
    )

  implicit val parameterDecoder: Decoder[Parameter] =
    Decoder.forProduct3(
      "description",
      "schema",
      "location"
    )(Parameter.apply)

  implicit val eitherMessageReferenceDecoder: Decoder[Either[Message, Reference]] =
    Decoder[Message].either(Decoder[Reference])

  implicit val operationDecoder: Decoder[Channel.Operation] =
    Decoder.instance(c =>
      for {
        operationId  <- c.downField("operationId").as[Option[String]]
        summary      <- c.downField("summary").as[Option[String]]
        description  <- c.downField("description").as[Option[String]]
        tags         <- c.downField("tags").as[Option[List[String]]]
        externalDocs <- c.downField("externalDocs").as[Option[ExternalDocs]]
        message      <- c.downField("message").as[Option[Either[Message, Reference]]]
      } yield Channel.Operation(
        operationId = operationId,
        summary = summary,
        description = description,
        tags = tags.getOrElse(List.empty),
        externalDocs = externalDocs,
        message = message
      )
    )

  implicit val eitherParameterReferenceDecoder: Decoder[Either[Parameter, Reference]] =
    Decoder[Parameter].either(Decoder[Reference])

  implicit val itemObjectDecoder: Decoder[Channel.ItemObject] =
    Decoder.forProduct5(
      s"$$ref",
      "description",
      "subscribe",
      "publish",
      "parameters"
    )(
      (
          ref: Option[String],
          description: Option[String],
          subscribe: Option[Channel.Operation],
          publish: Option[Channel.Operation],
          parameters: Option[List[Either[Parameter, Reference]]]
      ) =>
        Channel.ItemObject(
          ref,
          description,
          subscribe,
          publish,
          parameters.getOrElse(List.empty)
        )
    )

  implicit val componentsDecoder: Decoder[Components] =
    Decoder.forProduct3(
      "schemas",
      "messages",
      "parameters"
    )(
      (
          schemas: Option[Map[String, Schema]],
          messages: Option[Map[String, Either[Message, Reference]]],
          parameters: Option[Map[String, Either[Parameter, Reference]]]
      ) =>
        Components(
          schemas = schemas.getOrElse(Map.empty),
          messages = messages.getOrElse(Map.empty),
          parameters = parameters.getOrElse(Map.empty)
        )
    )

  implicit val infoDecoder: Decoder[Info] =
    Decoder.forProduct3(
      "title",
      "description",
      "version"
    )(Info)

  implicit val serverVariableDecoder: Decoder[Server.Variable] =
    Decoder.forProduct3(
      "enum",
      "default",
      "description"
    )((enumeration: Option[List[String]], default: String, description: Option[String]) =>
      Server.Variable(enumeration.getOrElse(List.empty), default, description)
    )

  implicit val serverDecoder: Decoder[Server] =
    Decoder.forProduct5(
      "url",
      "protocol",
      "protocolVersion",
      "description",
      "variables"
    ) {
      (
          url: String,
          protocol: String,
          protocolVersion: Option[String],
          description: Option[String],
          variables: Option[Map[String, Server.Variable]]
      ) =>
        Server(
          url = url,
          protocol = protocol,
          protocolVersion = protocolVersion,
          description = description,
          variables = variables.getOrElse(Map.empty)
        )
    }

  implicit val openApiDecoder: Decoder[AsyncApi] =
    Decoder.forProduct8(
      "asyncapi",
      "id",
      "info",
      "servers",
      "channels",
      "components",
      "tags",
      "externalDocs"
    )(
      (
          openapi: String,
          id: Option[String],
          info: Info,
          servers: Option[List[Server]],
          channels: Option[Map[String, Channel.ItemObject]],
          components: Option[Components],
          tags: Option[List[Tag]],
          externalDocs: Option[ExternalDocs]
      ) =>
        AsyncApi(
          asyncapi = openapi,
          id = id,
          info = info,
          servers = servers.getOrElse(List.empty),
          channels = channels.getOrElse(Map.empty),
          components = components,
          tags = tags.getOrElse(List.empty),
          externalDocs = externalDocs
        )
    )

  implicit class RichString(val inner: String) extends AnyVal {
    def lowercaseFirstLetter: String =
      if (inner.isEmpty) "" else s"${Character.toLowerCase(inner.charAt(0))}${inner.substring(1)}"

    def uppercaseFirstLetter: String =
      if (inner.isEmpty) "" else s"${Character.toUpperCase(inner.charAt(0))}${inner.substring(1)}"

    def toJavaClassCompatible: String =
      inner.split("[._/\\-]").map(_.uppercaseFirstLetter).mkString
  }
}
