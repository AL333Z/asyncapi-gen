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

package kafka

import cats.effect.{Async, Resource, Sync}
import cats.implicits._
import org.apache.kafka.streams.KafkaStreams.State
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.{KafkaStreams, Topology}

import java.time.Duration
import java.util.Properties

object Platform {

  def streamsResource[F[_]: Sync](top: Topology, props: Properties, timeout: Duration): Resource[F, KafkaStreams] =
    Resource.make(Sync[F].delay(new KafkaStreams(top, props)))(s => Sync[F].delay(s.close(timeout)).void)

  def runStreams[F[_]: Async](streams: KafkaStreams): F[Unit] = Async[F].async { (k: Either[Throwable, Unit] => Unit) =>
    streams.setUncaughtExceptionHandler(new StreamsUncaughtExceptionHandler {
      override def handle(e: Throwable): StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse = {
        k(Left(e))
        StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION
      }
    })

    streams.setStateListener { (state: State, _: State) =>
      state match {
        case State.ERROR => k(Left(new RuntimeException("The KafkaStreams went into an ERROR state.")))
        case _           => ()
      }
    }

    streams.start()
    Sync[F].delay(None)
  }

  def run[F[_]: Async](topo: Topology, props: Properties, timeout: Duration): F[Unit] =
    streamsResource[F](topo, props, timeout).use(runStreams[F])
}
