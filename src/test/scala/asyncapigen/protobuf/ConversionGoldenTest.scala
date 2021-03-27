package asyncapigen.protobuf

import asyncapigen.ParseAsyncApi
import asyncapigen.Printer.syntax._
import asyncapigen.protobuf.print._
import cats.effect.IO
import munit.CatsEffectSuite

class ConversionGoldenTest extends CatsEffectSuite {
  test("asyncapi to protobuf - basic") {
    val input: String =
      s"""
         |asyncapi: 2.0.0
         |info:
         |  title: Account Service
         |  version: 1.0.0
         |  description: This service is in charge of processing user signups
         |channels:
         |  user/signedup:
         |    subscribe:
         |      message:
         |        name: UserSignedUp
         |        payload:
         |          type: object
         |          required:
         |            - email
         |          properties:
         |            displayName:
         |              type: string
         |              description: Name of the user
         |            email:
         |              type: string
         |              format: email
         |              description: Email of the user
         |""".stripMargin

    val expectedProtobufs = List(
      s"""
         |syntax = "proto3";
         |
         |message UserSignedUp {
         |  optional string displayName = 1;
         |  string email = 2;
         |}
         |""".stripMargin
    )

    checkConversion(input, expectedProtobufs)
  }

  test("asyncapi to protobuf - basic with refs") {
    val input: String =
      s"""
         |asyncapi: 2.0.0
         |info:
         |  title: Account Service
         |  version: 1.0.0
         |  description: This service is in charge of processing user signups
         |channels:
         |  user/signedup:
         |    subscribe:
         |      message:
         |        $$ref: '#/components/messages/UserSignedUp'
         |components:
         |  messages:
         |    UserSignedUp:
         |      payload:
         |        type: object
         |        required:
         |          - email
         |        properties:
         |          displayName:
         |            type: string
         |            description: Name of the user
         |          email:
         |            type: string
         |            format: email
         |            description: Email of the user
         |""".stripMargin

    val expectedProtobufs = List(
      s"""
         |syntax = "proto3";
         |
         |message UserSignedUp {
         |  optional string displayName = 1;
         |  string email = 2;
         |}
         |""".stripMargin
    )

    checkConversion(input, expectedProtobufs)
  }

  private def checkConversion(input: String, expectedProtobufs: List[String]): IO[Unit] = {
    for {
      asyncApi  <- ParseAsyncApi.parseYamlAsyncApiContent[IO](input)
      protobufs <- IO.fromTry(fromAsyncApi(asyncApi))
    } yield protobufs.zip(expectedProtobufs).foreach { case (protobuf, expected) =>
      assertNoDiff(protobuf.print.normalized, expected.normalized)
    }
  }
}
