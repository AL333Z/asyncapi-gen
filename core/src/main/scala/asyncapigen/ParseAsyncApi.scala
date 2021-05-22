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
