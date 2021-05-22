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

package asyncapigen.protobuf

import asyncapigen.Printer.syntax._
import asyncapigen.protobuf
import asyncapigen.protobuf.print._
import asyncapigen.schema.AsyncApi
import cats.effect.{IO, Resource}
import cats.implicits._
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scalapb.ScalaPBCOps

import java.io.{File, PrintWriter}

object gen {

  def run(
      asyncApi: AsyncApi,
      targetPackageName: String,
      schemaTargetFolder: String,
      scalaTargetFolder: String,
      javaTargetFolder: String
  ): IO[Unit] =
    for {
      logger    <- Slf4jLogger.create[IO]
      protobufs <- IO.fromTry(protobuf.protobuf.fromAsyncApi(asyncApi, targetPackageName))
      _         <- logger.info(s"Converted to protobuf specs: $protobufs.")
      _ <- logger.info(
        s"Ensuring schema folder ($schemaTargetFolder), " +
          s"scala target folder ($scalaTargetFolder) and " +
          s"java target folder ($javaTargetFolder) are created..."
      )
      _ <- IO.delay(new File(schemaTargetFolder).mkdirs())
      _ <- IO.delay(new File(scalaTargetFolder).mkdirs())
      _ <- IO.delay(new File(javaTargetFolder).mkdirs())
      _ <- logger.info("Generating schemas and code for protos...")
      _ <- protobufs.traverse_ { x =>
        val proto = s"$schemaTargetFolder/${x.name}.proto"
        Resource
          .fromAutoCloseable(IO.delay(new PrintWriter(proto)))
          .use(w => IO.delay(w.write(x.print.normalized))) >>
          ScalaPBCOps.run(
            s"$proto --scala_out=flat_package,java_conversions:$scalaTargetFolder --java_out=$javaTargetFolder"
              .split(" ")
          )
      }
      _ <- logger.info("All done.")
    } yield ()
}
