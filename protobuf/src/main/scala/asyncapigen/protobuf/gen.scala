package asyncapigen.protobuf

import asyncapigen.Printer.syntax._
import asyncapigen.protobuf.print._
import asyncapigen.{protobuf, ParseAsyncApi}
import cats.effect.{IO, Resource}
import cats.implicits._
import scalapb.ScalaPBCOps

import java.io.{File, PrintWriter}

object gen {

  def run(input: String, schemaFolder: String, scalaTargetFolder: String, javaTargetFolder: String): IO[Unit] =
    for {
      asyncApi  <- ParseAsyncApi.parseYamlAsyncApiContent[IO](input)
      _         <- IO.delay(println(s"Parsed asyncapi spec: $asyncApi."))
      protobufs <- IO.fromTry(protobuf.protobuf.fromAsyncApi(asyncApi, "org.demo"))
      _         <- IO.delay(println(s"Converted to protobuf specs: $protobufs."))
      _ <- IO.delay(
        println(
          s"Ensuring schema folder ($schemaFolder), " +
            s"scala target folder ($scalaTargetFolder) and " +
            s"java target folder ($javaTargetFolder) are created..."
        )
      )
      _ <- IO.delay(new File(schemaFolder).mkdirs())
      _ <- IO.delay(new File(scalaTargetFolder).mkdirs())
      _ <- IO.delay(new File(javaTargetFolder).mkdirs())
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
      _ <- IO.delay(println("All done."))
    } yield ()
}
