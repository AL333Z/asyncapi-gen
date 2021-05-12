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
