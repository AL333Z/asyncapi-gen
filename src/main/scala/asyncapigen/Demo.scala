//package asyncapigen
//
//import asyncapigen.ParseAsyncApi.YamlSource
//import asyncapigen.Printer.syntax._
//import asyncapigen.protobuf.print._
//import asyncapigen.protobuf.schema.FieldDescriptorProto.{
//  EnumDescriptorProto,
//  OneofDescriptorProto,
//  PlainFieldDescriptorProto
//}
//import asyncapigen.protobuf.schema._
//import asyncapigen.schema.Schema.ObjectSchema
//import asyncapigen.schema.{AsyncApi, Message, Reference, Schema}
//import cats.data.NonEmptyList
//import cats.effect.{ExitCode, IO, IOApp}
//import cats.implicits._
//
//import java.io.File
//
//object Demo extends IOApp {
//
//  def process(filename: String): IO[Unit] =
//    for {
//      file     <- IO.delay(new File(getClass.getResource(filename).toURI))
//      asyncapi <- ParseAsyncApi.parseYamlAsyncApi[IO](YamlSource(file))
//      _        <- IO.delay(println(asyncapi))
//      protobufs = map(asyncapi)
//      _ <- IO.delay(println(protobufs))
//      _ <- IO.delay(println(protobufs.print))
//    } yield ()
//
//  override def run(args: List[String]): IO[ExitCode] =
//    List(
////      "/basic-asyncapi.yaml",
////      "/basic-ref-asyncapi.yaml",
//      "/oneOf-asyncapi.yaml"
//    )
//      .traverse(process)
//      .as(ExitCode.Success)
//
//  // TODO this should evolve properly
//  // how to convert from asyncapi.Schema to protobuf.FieldDescriptorProto
//  // how to convert types asyncapi -> protobuf
//  // how to decide field indexes? how to prevent breaking changes? read them from custom prop in the asyncapi schema?
//  // how to generate common models (e.g. models stored in $ref and used by multiple asyncapi components)? duplicate in a nested message in each protobuf? import them?
//  def map(asyncApi: AsyncApi): List[FileDescriptorProto] = {
//
//    // assuming 1 message schema per topic (see:https://docs.confluent.io/platform/current/schema-registry/serdes-develop/serdes-protobuf.html#multiple-event-types-in-the-same-topic)
//    asyncApi.channels.map { case (name, item) =>
//      val messages = item.subscribe.toList.flatMap(op => extractMessages(asyncApi, name, op.message))
//      FileDescriptorProto(
//        name = name.split('/').last,
//        `package` = Some(s"org.demo.${name.replace('/', '.')}"),
//        messageTypes = messages,
//        enumTypes = Nil,
//        syntax = "proto3"
//      )
//    }.toList
//  }
//
//  private def extractMessages(
//      asyncApi: AsyncApi,
//      name: String,
//      message: Option[Either[Message, Reference]]
//  ): List[MessageDescriptorProto] = {
//    message match {
//      case Some(Left(message)) =>
//        message.payload match {
//          case Left(schema) =>
//            List(
//              MessageDescriptorProto(
//                name = name.split('/').last.capitalize, // TODO what to put here?
//                fields = toFieldDescriptorProtos(asyncApi, schema),
//                nestedMessages = Nil,
//                nestedEnums = Nil,
//                options = Nil
//              )
//            )
//          case Right(ref) => resolveMessageDescriptorProtoFromRef(asyncApi, ref)
//        }
//      case Some(Right(ref)) => resolveMessageDescriptorProtoFromRef(asyncApi, ref)
//      case None             => Nil
//    }
//  }
//
//  // TODO this is a strong and wrong assumption. Refs should be fully supported!
//  private def resolveMessageDescriptorProtoFromRef(asyncApi: AsyncApi, ref: Reference): List[MessageDescriptorProto] = {
//    val messageName: String = ref.ref.split("#/components/messages/")(1)
//    val message: Option[Either[Message, Reference]] =
//      asyncApi.components.flatMap(_.messages.get(messageName))
//    extractMessages(asyncApi, messageName, message)
//  }
//
//  private def toFieldDescriptorProtos(asyncApi: AsyncApi, schema: Schema): List[FieldDescriptorProto] = {
//    schema match {
//      case Schema.RefSchema(ref) => resolveMessageDescriptorProtoFromRef(asyncApi, ref).flatMap(_.fields)
//      case Schema.SumSchema(_)   => ???
//      case Schema.ObjectSchema(required, properties) =>
//        properties.zipWithIndex.toList
//          .map { case ((fieldName, v), i) =>
//            v match {
//              case Schema.RefSchema(_) => ???
//              case Schema.SumSchema(_) =>
//                OneofDescriptorProto(
//                  name = fieldName,
//                  fields = Nil // oneOf.flatMap(x => toFieldDescriptorProtos(asyncApi, x))
//                )
//              case Schema.ArraySchema(_) => ???
//              case Schema.EnumSchema(enum) =>
//                EnumDescriptorProto(
//                  name = fieldName,
//                  symbols = NonEmptyList.fromListUnsafe(enum.zipWithIndex)
//                )
//              case _ =>
//                PlainFieldDescriptorProto(
//                  name = fieldName,
//                  `type` = ???,
//                  label = toFieldDescriptorProtoLabel(required, fieldName),
//                  options = Nil,
//                  index = i
//                )
//            }
//          }
//      case Schema.ArraySchema(_) => ???
//      case Schema.EnumSchema(_)  => ???
//
//      case Schema.BasicSchema.IntegerSchema  => ???
//      case Schema.BasicSchema.LongSchema     => ???
//      case Schema.BasicSchema.FloatSchema    => ???
//      case Schema.BasicSchema.DoubleSchema   => ???
//      case Schema.BasicSchema.ByteSchema     => ???
//      case Schema.BasicSchema.BinarySchema   => ???
//      case Schema.BasicSchema.BooleanSchema  => ???
//      case Schema.BasicSchema.DateSchema     => ???
//      case Schema.BasicSchema.DateTimeSchema => ???
//      case Schema.BasicSchema.PasswordSchema => ???
//      case Schema.BasicSchema.StringSchema   => ???
//    }
//  }
//
//  private def toFieldDescriptorProtoType(v: Schema): FieldProtoType = {
//    v match {
//      case _: ObjectSchema                   => FieldProtoType.ObjectProto
//      case Schema.RefSchema(_)               => ???
//      case Schema.SumSchema(_)               => FieldProtoType.NamedTypeProto("<FieldDescriptorProtoType.SumSchema>")
//      case Schema.ArraySchema(_)             => ???
//      case Schema.EnumSchema(_)              => ???
//      case Schema.BasicSchema.IntegerSchema  => FieldProtoType.Int32Proto
//      case Schema.BasicSchema.LongSchema     => FieldProtoType.Int64Proto
//      case Schema.BasicSchema.FloatSchema    => FieldProtoType.FloatProto
//      case Schema.BasicSchema.DoubleSchema   => FieldProtoType.DoubleProto
//      case Schema.BasicSchema.ByteSchema     => FieldProtoType.BytesProto
//      case Schema.BasicSchema.BinarySchema   => FieldProtoType.BytesProto
//      case Schema.BasicSchema.BooleanSchema  => FieldProtoType.BoolProto
//      case Schema.BasicSchema.DateSchema     => ???
//      case Schema.BasicSchema.DateTimeSchema => ???
//      case Schema.BasicSchema.PasswordSchema => FieldProtoType.StringProto
//      case Schema.BasicSchema.StringSchema   => FieldProtoType.StringProto
//    }
//  }
//
//  private def toFieldDescriptorProtoLabel(required: List[String], fieldName: String): FieldDescriptorProtoLabel =
//    if (required.contains(fieldName)) FieldDescriptorProtoLabel.Required
//    else FieldDescriptorProtoLabel.Optional
//}
