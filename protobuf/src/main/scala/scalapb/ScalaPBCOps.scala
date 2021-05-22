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
