package asyncapigen

import cats.kernel.Eq

import java.time.{LocalDate, ZonedDateTime}

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
  )

  final case class Info(title: String, description: Option[String], version: String)

  final case class Server(
      url: String,
      protocol: String,
      protocolVersion: Option[String],
      description: Option[String],
      variables: Map[String, Server.Variable]
  )

  object Server {
    final case class Variable(enum: List[String], default: String, description: Option[String])
  }

  final case class Reference(ref: String)

  object Channel {
    final case class ItemObject(
        ref: Option[String], // $ref
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
      description: Option[String],
      tags: List[String],
      payload: Either[Schema, Reference]
  )

  final case class Encoding(
      contentType: Option[String],
      headers: Map[String, Either[Header, Reference]],
      style: Option[String],
      explode: Option[Boolean],
      allowReserved: Option[Boolean]
  )

  final case class Tag(name: String, description: Option[String], externalDocs: Option[ExternalDocs])

  final case class ExternalDocs(url: String, description: Option[String])

  type Callback = Map[String, Channel.ItemObject]

  object Callback {
    def apply(values: (String, Channel.ItemObject)*): Callback =
      values.toMap
  }

  final case class Header(description: String, schema: Schema)

  final case class Parameter(description: Option[String], schema: Schema, location: String)

  sealed abstract class Schema extends Product with Serializable

  object Schema {
    final case class SumSchema(oneOf: List[Schema])                                                extends Schema
    final case class ObjectSchema(required: Option[List[String]], properties: Map[String, Schema]) extends Schema
    final case class ArraySchema(items: List[Schema])                                              extends Schema
    final case class EnumSchema(enum: List[String])                                                extends Schema
    final case class RefSchema(ref: Reference)                                                     extends Schema
    final case class IntegerSchema(value: Int)                                                     extends Schema
    final case class LongSchema(value: Long)                                                       extends Schema
    final case class FloatSchema(value: Float)                                                     extends Schema
    final case class DoubleSchema(value: Double)                                                   extends Schema
    final case class ByteSchema(value: Array[Byte])                                                extends Schema
    final case class BinarySchema(value: List[Boolean])                                            extends Schema
    final case class BooleanSchema(value: Boolean)                                                 extends Schema
    final case class DateSchema(value: LocalDate)                                                  extends Schema
    final case class DateTimeSchema(value: ZonedDateTime)                                          extends Schema
    final case class PasswordSchema(value: String)                                                 extends Schema
    final case class StringSchema(value: String)                                                   extends Schema
  }

}
