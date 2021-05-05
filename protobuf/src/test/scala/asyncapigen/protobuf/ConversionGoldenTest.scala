package asyncapigen.protobuf

import asyncapigen.Printer.syntax._
import asyncapigen.protobuf.print._
import asyncapigen.{protobuf, ParseAsyncApi}
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
         |              x-custom-fields:
         |                x-protobuf-index:
         |                  type: integer
         |                  value: 1
         |            email:
         |              type: string
         |              format: email
         |              description: Email of the user
         |              x-custom-fields:
         |                x-protobuf-index:
         |                  type: integer
         |                  value: 2
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
         |            x-custom-fields:
         |              x-protobuf-index:
         |                type: integer
         |                value: 1
         |          email:
         |            type: string
         |            format: email
         |            description: Email of the user
         |            x-custom-fields:
         |              x-protobuf-index:
         |                type: integer
         |                value: 2
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

  test("asyncapi to protobuf - oneof with basic types") {
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
         |              - type: string
         |                name: StringEventType
         |                x-custom-fields:
         |                  x-protobuf-index:
         |                    type: integer
         |                    value: 3
         |              - type: integer
         |                name: IntEventType
         |                x-custom-fields:
         |                  x-protobuf-index:
         |                    type: integer
         |                    value: 4
         |""".stripMargin

    val expectedProtobufs = List(
      s"""
         |syntax = "proto3";
         |
         |message DocumentStateChange {
         |  string id = 1;
         |  string documentType = 2;
         |  oneof eventType {
         |    string stringEventType = 3;
         |    int32 intEventType = 4;
         |  }
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

    val expectedProtobufs = List(
      s"""
         |syntax = "proto3";
         |
         |message DocumentStateChange {
         |  string id = 1;
         |  string documentType = 2;
         |  oneof eventType {
         |    DocumentCreatedEvent documentCreatedEvent = 3;
         |    DocumentSignedEvent documentSignedEvent = 4;
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
         |              x-custom-fields:
         |                x-protobuf-index:
         |                  type: integer
         |                  value: 1
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
         |              x-custom-fields:
         |                x-protobuf-index:
         |                  type: integer
         |                  value: 1
         |            stuffs:
         |              type: array
         |              items:
         |                $$ref: '#/components/messages/Item'
         |              x-custom-fields:
         |                x-protobuf-index:
         |                  type: integer
         |                  value: 2
         |            myEnums:
         |              type: array
         |              items:
         |                type: string
         |                enum: [ bar, foo ]
         |              x-custom-fields:
         |                x-protobuf-index:
         |                  type: integer
         |                  value: 3
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

  test("asyncapi to protobuf - root array") {
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
         |          type: array
         |          items:
         |            type: string
         |            format: uuid
         |          x-custom-fields:
         |            x-protobuf-index:
         |              type: integer
         |              value: 1
         |components:
         |  messages:
         |""".stripMargin

    val expectedProtobufs = List(
      s"""
         |syntax = "proto3";
         |
         |message UserSignedUp {
         |  repeated string items = 1;
         |}
         |""".stripMargin
    )

    checkConversion(input, expectedProtobufs)
  }

  test("asyncapi to protobuf - root enum") {
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
         |          type: string
         |          enum: [ bar, foo ]
         |          x-custom-fields:
         |            x-protobuf-index:
         |              type: integer
         |              value: 1
         |""".stripMargin

    val expectedProtobufs = List(
      s"""
         |syntax = "proto3";
         |
         |message UserSignedUp {
         |  Values values = 1;
         |  enum Values {
         |    bar = 0;
         |    foo = 1;
         |  }
         |}
         |""".stripMargin
    )

    checkConversion(input, expectedProtobufs)
  }

  test("asyncapi to protobuf - root basic") {
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
         |          type: string
         |          x-custom-fields:
         |            x-protobuf-index:
         |              type: integer
         |              value: 1
         |""".stripMargin

    val expectedProtobufs = List(
      s"""
         |syntax = "proto3";
         |
         |message UserSignedUp {
         |  string value = 1;
         |}
         |""".stripMargin
    )

    checkConversion(input, expectedProtobufs)
  }

  test("asyncapi to protobuf - root oneof with basic types") {
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
         |        oneOf:
         |          - type: string
         |            name: StringEventType
         |            x-custom-fields:
         |              x-protobuf-index:
         |                type: integer
         |                value: 3
         |          - type: integer
         |            name: IntEventType
         |            x-custom-fields:
         |              x-protobuf-index:
         |                type: integer
         |                value: 4
         |""".stripMargin

    val expectedProtobufs = List(
      s"""
         |syntax = "proto3";
         |
         |message DocumentStateChange {
         |  oneof kind {
         |    string stringEventType = 3;
         |    int32 intEventType = 4;
         |  }
         |}
         |""".stripMargin
    )

    checkConversion(input, expectedProtobufs)
  }

  private def checkConversion(input: String, expectedProtobufs: List[String]): IO[Unit] = {
    for {
      asyncApi  <- ParseAsyncApi.parseYamlAsyncApiContent[IO](input)
      protobufs <- IO.fromTry(protobuf.protobuf.fromAsyncApi(asyncApi))
    } yield protobufs.zip(expectedProtobufs).foreach { case (protobuf, expected) =>
      assertNoDiff(protobuf.print.normalized, expected.normalized)
    }
  }
}
