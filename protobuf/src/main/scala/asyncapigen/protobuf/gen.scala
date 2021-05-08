package asyncapigen.protobuf

import asyncapigen.Printer.syntax._
import asyncapigen.protobuf.print._
import asyncapigen.{protobuf, ParseAsyncApi}
import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.implicits._
import scalapb.ScalaPBCOps

import java.io.{File, PrintWriter}

object gen {

  def run(input: String, schemaFolder: String, scalaTargetFolder: String, javaTargetFolder: String): IO[Unit] =
    for {
      asyncApi  <- ParseAsyncApi.parseYamlAsyncApiContent[IO](input)
      _         <- IO.delay(println(s"Parsed asyncapi spec: $asyncApi."))
      protobufs <- IO.fromTry(protobuf.protobuf.fromAsyncApi(asyncApi, "org.demo"))
      _         <- IO.delay(println(s"Converted to protobuf specs: $protobufs."))
      _ <- IO.delay(
        println(
          s"Ensuring schema folder ($schemaFolder), " +
            s"scala target folder ($scalaTargetFolder) and " +
            s"java target folder ($javaTargetFolder) are created..."
        )
      )
      _ <- IO.delay(new File(schemaFolder).mkdirs())
      _ <- IO.delay(new File(scalaTargetFolder).mkdirs())
      _ <- IO.delay(new File(javaTargetFolder).mkdirs())
      _ <- protobufs.traverse_ { x =>
        val proto = s"$schemaFolder${x.name}.proto"
        Resource
          .fromAutoCloseable(IO.delay(new PrintWriter(proto)))
          .use(w => IO.delay(w.write(x.print.normalized))) >>
          ScalaPBCOps.run(
            s"$proto --scala_out=flat_package,java_conversions:$scalaTargetFolder --java_out=$javaTargetFolder"
              .split(" ")
          )
      }
      _ <- IO.delay(println("All done."))
    } yield ()
}

// TODO transform this in a test!
object Demo extends IOApp {
  val input: String =
    s"""
       |asyncapi: 2.0.0
       |info:
       |  title: Document Service
       |  version: 1.0.0
       |  description: This service is in charge of processing document updates
       |channels:
       |  document/documentStateChange:
       |    subscribe:
       |      message:
       |        $$ref: '#/components/messages/DocumentStateChange'
       |components:
       |  messages:
       |    DocumentStateChange:
       |      payload:
       |        type: object
       |        required:
       |          - id
       |          - documentType
       |          - eventType
       |        properties:
       |          id:
       |            type: string
       |            format: uuid
       |            description: The message identifier
       |            x-custom-fields:
       |              x-protobuf-index:
       |                type: integer
       |                value: 1
       |          documentType:
       |            type: string
       |            description: Type of the document
       |            x-custom-fields:
       |              x-protobuf-index:
       |                type: integer
       |                value: 2
       |          eventType:
       |            oneOf:
       |              - $$ref: '#/components/messages/DocumentCreatedEvent'
       |                x-custom-fields:
       |                  x-protobuf-index:
       |                    type: integer
       |                    value: 3
       |              - $$ref: '#/components/messages/DocumentSignedEvent'
       |                x-custom-fields:
       |                  x-protobuf-index:
       |                    type: integer
       |                    value: 4
       |    DocumentCreatedEvent:
       |      payload:
       |        type: object
       |    DocumentSignedEvent:
       |      payload:
       |        type: object
       |
       |""".stripMargin

  override def run(args: List[String]): IO[ExitCode] = {
    val prefix = "./target/src_managed/asyncapi-gen/"
    gen
      .run(
        input = input,
        schemaFolder = prefix + "schemas/",
        scalaTargetFolder = prefix + "scalapb/",
        javaTargetFolder = prefix + "javapb/"
      )
      .as(ExitCode.Success)
  }
}
