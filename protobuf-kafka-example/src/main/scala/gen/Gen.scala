package gen

import asyncapigen.ParseAsyncApi
import asyncapigen.kafkaprotobuf.gen
import cats.effect.{IO, IOApp}

object Gen extends IOApp.Simple {
  val input: String =
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

  val baseFolder         = "./protobuf-kafka-example/src/main/scala"
  val schemaTargetFolder = "./protobuf-kafka-example/src/main/resources"
  val scalaTargetFolder  = s"$baseFolder"
  val javaTargetFolder   = s"$baseFolder"
  val targetPackageName  = "gen"

  override def run: IO[Unit] =
    ParseAsyncApi
      .parseYamlAsyncApiContent[IO](input)
      .flatMap(asyncApi =>
        gen.run(
          asyncApi = asyncApi,
          targetPackageName = targetPackageName,
          schemaTargetFolder = schemaTargetFolder,
          scalaTargetFolder = scalaTargetFolder,
          javaTargetFolder = javaTargetFolder
        )
      )
}
