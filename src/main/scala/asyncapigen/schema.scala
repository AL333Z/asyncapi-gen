package asyncapigen

import asyncapigen.schema.Schema.BasicSchema._
import asyncapigen.schema.Schema._
import cats.implicits._
import cats.kernel.Eq
import io.circe._

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

  final case class Reference(value: String)

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
      payload: Either[Schema, Reference],
      tags: List[String],
      name: Option[String],
      description: Option[String]
  )

  final case class Tag(name: String, description: Option[String], externalDocs: Option[ExternalDocs])

  final case class ExternalDocs(url: String, description: Option[String])

  final case class Header(description: String, schema: Schema)

  final case class Parameter(description: Option[String], schema: Schema, location: String)

  sealed abstract class Schema extends Product with Serializable

  object Schema {
    final case class RefSchema(ref: Reference)                                             extends Schema
    final case class SumSchema(oneOf: List[(Option[String], Schema)])                      extends Schema
    final case class ObjectSchema(required: List[String], properties: Map[String, Schema]) extends Schema
    final case class ArraySchema(items: Schema)                                            extends Schema
    final case class EnumSchema(enum: List[String])                                        extends Schema

    sealed abstract class BasicSchema extends Schema
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
  }

  implicit val referenceDecoder: Decoder[Reference] = Decoder.forProduct1(s"$$ref")(Reference.apply)

  private val refSchemaDecoder: Decoder[RefSchema] =
    Decoder[Reference].map(RefSchema)

  implicit val sumSchemaElemDecoder: Decoder[(Option[String], Schema)] =
    (Decoder[Option[String]].at("name"), Decoder[Schema]).tupled

  private val sumSchemaDecoder: Decoder[SumSchema] =
    Decoder.instance(_.downField("oneOf").as[List[(Option[String], Schema)]].map(SumSchema))

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
            .as[Option[Map[String, Schema]]](
              Decoder.decodeOption(Decoder.decodeMap[String, Schema](KeyDecoder.decodeKeyString, schemaDecoder))
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

  private val basicSchemaDecoder: Decoder[BasicSchema] =
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

  implicit val messageDecoder: Decoder[Message] =
    Decoder.forProduct4(
      "payload",
      "name",
      "description",
      "tags"
    )(
      (
          payload: Either[Schema, Reference],
          name: Option[String],
          description: Option[String],
          tags: Option[List[String]]
      ) => Message(payload = payload, name = name, description = description, tags = tags.getOrElse(List.empty))
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
          ref: Option[String], // $ref
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
    )((enum: Option[List[String]], default: String, description: Option[String]) =>
      Server.Variable(enum.getOrElse(List.empty), default, description)
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
}
