/*
 * Copyright (c) 2021 al333z
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
       |        bindings:
       |          kafka:
       |            key:
       |              type: string
       |        payload:
       |          type: object
       |          required:
       |            - email
       |          properties:
       |            displayName:
       |              type: string
       |              description: Name of the user
       |              x-custom-attributes:
       |                x-protobuf-index:
       |                  type: integer
       |                  value: 1
       |            email:
       |              type: string
       |              format: email
       |              description: Email of the user
       |              x-custom-attributes:
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
