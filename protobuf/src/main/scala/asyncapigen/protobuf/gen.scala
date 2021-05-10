package asyncapigen.protobuf

import asyncapigen.Printer.syntax._
import asyncapigen.protobuf.print._
import asyncapigen.{protobuf, ParseAsyncApi}
import cats.effect.{IO, Resource}
import cats.implicits._
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scalapb.ScalaPBCOps

import java.io.{File, PrintWriter}

object gen {

  def run(input: String, schemaFolder: String, scalaTargetFolder: String, javaTargetFolder: String): IO[Unit] =
    for {
      logger    <- Slf4jLogger.create[IO]
      asyncApi  <- ParseAsyncApi.parseYamlAsyncApiContent[IO](input)
      _         <- logger.info(s"Parsed asyncapi spec: $asyncApi.")
      protobufs <- IO.fromTry(protobuf.protobuf.fromAsyncApi(asyncApi, "org.demo"))
      _         <- logger.info(s"Converted to protobuf specs: $protobufs.")
      _ <- logger.info(
        s"Ensuring schema folder ($schemaFolder), " +
          s"scala target folder ($scalaTargetFolder) and " +
          s"java target folder ($javaTargetFolder) are created..."
      )
      _ <- IO.delay(new File(schemaFolder).mkdirs())
      _ <- IO.delay(new File(scalaTargetFolder).mkdirs())
      _ <- IO.delay(new File(javaTargetFolder).mkdirs())
      _ <- logger.info("Generating schemas and code for protos...")
      _ <- protobufs.traverse_ { x =>
        val proto = s"$schemaFolder/${x.name}.proto"
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
