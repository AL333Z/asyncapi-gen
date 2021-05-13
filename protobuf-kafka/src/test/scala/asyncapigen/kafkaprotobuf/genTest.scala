package asyncapigen.kafkaprotobuf

import asyncapigen.ParseAsyncApi
import cats.effect.{IO, Resource}
import munit.CatsEffectSuite

import java.io.File

object Samples {
  val basicProtobuf: String =
    s"""
       |asyncapi: 2.0.0
       |info:
       |  title: Account Service
       |  version: 1.0.0
       |  description: This service is in charge of processing user signups
       |channels:
       |  user_events:
       |    subscribe:
       |      message:
       |        name: User-Signed/Up
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
}

class genTest extends CatsEffectSuite {
  test("asyncapi to protobuf to gen sources and schema") {
    val baseFolder         = "./target/src_managed/asyncapi-gen"
    val schemaTargetFolder = s"$baseFolder/schemas"
    val scalaTargetFolder  = s"$baseFolder/scalapb"
    val javaTargetFolder   = s"$baseFolder/javapb"
    val targetPackageName  = "org.demo"

    val expectedGenTopic =
      s"""
         |package org.demo
         |
         |import asyncapigen.kafkaprotobuf.Topic
         |import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
         |
         |object Topics {
         |
         |  def userEvents: Topic[String, org.demo.UserSignedUp] = 
         |    Topic.mkStringKeyedTopic[org.demo.UserSignedUp, org.demo.UserEvents.UserSignedUp](
         |      name = "user_events",
         |      valueCompanion = org.demo.UserSignedUp,
         |      schemaRegistryClient = None,
         |      serdeConfig = Map.empty[String, Any]
         |    )
         |    
         |  def userEvents(serdeConfig: Map[String, Any]): Topic[String, org.demo.UserSignedUp] = 
         |    Topic.mkStringKeyedTopic[org.demo.UserSignedUp, org.demo.UserEvents.UserSignedUp](
         |      name = "user_events",
         |      valueCompanion = org.demo.UserSignedUp,
         |      schemaRegistryClient = None,
         |      serdeConfig = serdeConfig
         |    )
         |    
         |  def userEvents(schemaRegistryClient: SchemaRegistryClient, serdeConfig: Map[String, Any] = Map()): Topic[String, org.demo.UserSignedUp] = 
         |    Topic.mkStringKeyedTopic[org.demo.UserSignedUp, org.demo.UserEvents.UserSignedUp](
         |      name = "user_events",
         |      valueCompanion = org.demo.UserSignedUp,
         |      schemaRegistryClient = Some(schemaRegistryClient),
         |      serdeConfig = serdeConfig
         |    )
         |
         |}
         |""".stripMargin

    for {
      asyncApi <- ParseAsyncApi.parseYamlAsyncApiContent[IO](Samples.basicProtobuf)
      _ <- gen.run(
        asyncApi = asyncApi,
        targetPackageName = targetPackageName,
        schemaTargetFolder = schemaTargetFolder,
        scalaTargetFolder = scalaTargetFolder,
        javaTargetFolder = javaTargetFolder
      )
      schemaGenFile      = s"$schemaTargetFolder/UserEvents.proto"
      scalaGenFile       = s"$scalaTargetFolder/org/demo/UserEventsProto.scala"
      javaGenFile        = s"$javaTargetFolder/org/demo/UserEvents.java"
      scalaTopicsGenFile = s"$scalaTargetFolder/org/demo/Topics.scala"
      _ <- assertIOBoolean(IO.delay(new File(schemaGenFile).exists()), s"$schemaGenFile does not exist!")
      _ <- assertIOBoolean(IO.delay(new File(scalaGenFile).exists()), s"$scalaGenFile does not exist!")
      _ <- assertIOBoolean(IO.delay(new File(javaGenFile).exists()), s"$javaGenFile does not exist!")
      _ <- assertIOBoolean(IO.delay(new File(scalaTopicsGenFile).exists()), s"$scalaTopicsGenFile does not exist!")
      genTopic <- Resource
        .fromAutoCloseable(IO.delay(scala.io.Source.fromFile(scalaTopicsGenFile)))
        .use(f => IO.delay(f.mkString))
      _ = assertNoDiff(genTopic, expectedGenTopic)
    } yield ()
  }
}
