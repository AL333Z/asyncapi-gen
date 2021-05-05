/*
 * Copyright 2018-2020 47 Degrees Open Source <https://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package asyncapigen

import asyncapigen.schema._
import cats.effect.{Resource, Sync}
import cats.implicits._
import io.circe.Decoder

import java.io.File

object ParseAsyncApi {

  case class YamlSource(file: File)
  case class JsonSource(file: File)

  def parseYamlAsyncApiSource[F[_]: Sync](yamlSource: YamlSource): F[AsyncApi] =
    readContent(yamlSource.file).flatMap(parseYamlAsyncApiContent[F])

  def parseYamlAsyncApiContent[F[_]: Sync](content: String): F[AsyncApi] = {
    Sync[F].fromEither(
      io.circe.yaml.parser
        .parse(content)
        .leftMap(_.asLeft)
        .flatMap(Decoder[AsyncApi].decodeJson(_).leftMap(_.asRight))
        .left
        .map(_.valueOr(identity))
    )
  }

  def parseJsonAsyncApiContent[F[_]: Sync](input: String): F[AsyncApi] =
    for {
      json     <- Sync[F].fromEither(io.circe.parser.parse(input))
      asyncApi <- Sync[F].fromEither(Decoder[AsyncApi].decodeJson(json))
    } yield asyncApi

  def parseJsonAsyncApiSource[F[_]: Sync](input: JsonSource): F[AsyncApi] =
    readContent(input.file).flatMap(parseJsonAsyncApiContent[F])

  private def readContent[F[_]: Sync](file: File): F[String] =
    Resource.fromAutoCloseable(Sync[F].delay(scala.io.Source.fromFile(file))).use { s =>
      Sync[F].delay {
        s.getLines()
          .toList
          .mkString("\n")
      }
    }
}
