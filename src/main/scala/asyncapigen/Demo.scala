package asyncapigen

import asyncapigen.ParseAsyncApi.YamlSource
import cats.effect.{ExitCode, IO, IOApp}

import java.io.File

object Demo extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      file <- IO.delay(new File(getClass.getResource(s"/a.yaml").toURI))
      res  <- ParseAsyncApi.parseYamlAsyncApi[IO](YamlSource(file))
      _    <- IO.delay(println(res))
    } yield ExitCode.Success
}
