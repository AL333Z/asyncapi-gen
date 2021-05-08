package scalapb

import cats.effect.IO
import scalapb.ScalaPBC.{processArgs, runProtoc}

// The same as `ScalaPBC.main`, but wrapped in IO.
// Wrapping with `IO.delay(ScalaPBC.main(...))` was causing the deadlock of the caller `IOApp` (I believe it may be for the usage of `System.exit`).
// With this trick the IO program will work and terminate just fine.
object ScalaPBCOps {
  def run(args: Array[String]): IO[Unit] = {
    for {
      config <- IO.delay(processArgs(args))
      code   <- IO.delay(runProtoc(config))
      _ <-
        if (!config.throwException) IO.unit
        else IO.raiseError(new ScalaPbcException(s"Exit with code $code"))
    } yield ()
  }
}
