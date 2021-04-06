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

  test("asyncapi to protobuf - refs and oneof") {
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
         |          documentType:
         |            type: string
         |            description: Type of the document
         |          eventType:
         |            oneOf:
         |              - $$ref: '#/components/messages/DocumentCreatedEvent'
         |              - $$ref: '#/components/messages/DocumentSignedEvent'
         |    DocumentCreatedEvent:
         |      payload:
         |        type: object
         |    DocumentSignedEvent:
         |      payload:
         |        type: object
         |
         |""".stripMargin

    // FIXME the indexes in the oneof are wrong, adjust the test once we add support for those!
    val expectedProtobufs = List(
      s"""
         |syntax = "proto3";
         |
         |message DocumentStateChange {
         |  string id = 1;
         |  string documentType = 2;
         |  oneof eventType {
         |    DocumentCreatedEvent documentCreatedEvent = 1;
         |    DocumentSignedEvent documentSignedEvent = 2;
         |  }
         |  message DocumentCreatedEvent {}
         |  message DocumentSignedEvent {}
         |}
         |""".stripMargin
    )

    checkConversion(input, expectedProtobufs)
  }

  test("asyncapi to protobuf - enums") {
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
         |          properties:
         |            myEnum:
         |              type: string
         |              enum: [ bar, foo ]
         |""".stripMargin

    val expectedProtobufs = List(
      s"""
         |syntax = "proto3";
         |
         |message UserSignedUp {
         |  optional MyEnum myEnum = 1;
         |  enum MyEnum {
         |    bar = 0;
         |    foo = 1;
         |  }
         |}
         |""".stripMargin
    )

    checkConversion(input, expectedProtobufs)
  }

  test("asyncapi to protobuf - array") {
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
         |          properties:
         |            strings:
         |              type: array
         |              items:
         |                type: string
         |                format: uuid
         |            stuffs:
         |              type: array
         |              items:
         |                $$ref: '#/components/messages/Item'
         |            myEnums:
         |              type: array
         |              items:
         |                type: string
         |                enum: [ bar, foo ]
         |components:
         |  messages:
         |    Item:
         |      payload:
         |        type: object
         |""".stripMargin

    val expectedProtobufs = List(
      s"""
         |syntax = "proto3";
         |
         |message UserSignedUp {
         |  repeated string strings = 1;
         |  repeated Item stuffs = 2;
         |  repeated MyEnums myEnums = 3;
         |  message Item {}
         |  enum MyEnums {
         |    bar = 0;
         |    foo = 1;
         |  }
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
