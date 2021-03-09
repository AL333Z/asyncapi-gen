//package asyncapigen
//
//import asyncapigen.ParseAsyncApi.YamlSource
//import asyncapigen.protobuf.print._
//import asyncapigen.protobuf.schema.Protobuf.FieldType
//import asyncapigen.protobuf.schema.Protobuf.FieldType._
//import asyncapigen.protobuf.schema._
//import asyncapigen.schema.AsyncApi
//import asyncapigen.schema.Schema.ObjectSchema
//import cats.effect.{ExitCode, IO, IOApp}
//
//import java.io.File
//
//object Demo extends IOApp {
//  override def run(args: List[String]): IO[ExitCode] =
//    for {
//      file     <- IO.delay(new File(getClass.getResource(s"/a.yaml").toURI))
//      asyncapi <- ParseAsyncApi.parseYamlAsyncApi[IO](YamlSource(file))
//      _        <- IO.delay(println(asyncapi))
//      protobuf = map(asyncapi)
//      _ <- IO.delay(println(protobuf))
//      _ <- IO.delay(println(Printer[FieldType].print(protobuf)))
//    } yield ExitCode.Success
//
//  // TODO this should evolve properly
//  def map(asyncApi: AsyncApi): DescriptorProto = {
//    val (firstMessageName, msg) = asyncApi.components.head.messages.toList.head
//    val firstMessageSchema: schema.Schema =
//      msg.swap.getOrElse(throw new RuntimeException).payload.swap.getOrElse(throw new RuntimeException)
//
//    Protobuf.FieldType.DescriptorProto(
//      name = firstMessageName,
//      fields = firstMessageSchema.asInstanceOf[ObjectSchema].properties.zipWithIndex.toList.map { case ((k, _), i) =>
//        FieldDescriptorProto.FieldDescriptorProto(
//          name = k,
//          `type` = TString,
//          position = i,
//          options = Nil,
//          isRepeated = false,
//          isMapField = false
//        )
//      },
//      reserved = Nil,
//      nestedMessages = Nil,
//      nestedEnums = Nil
//    )
//  }
//}
