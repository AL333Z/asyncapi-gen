package kafka

import cats.effect.{Async, Resource, Sync}
import cats.implicits._
import org.apache.kafka.streams.KafkaStreams.State
import org.apache.kafka.streams.{KafkaStreams, Topology}

import java.time.Duration
import java.util.Properties

object Platform {

  def streamsResource[F[_]: Sync](top: Topology, props: Properties, timeout: Duration): Resource[F, KafkaStreams] =
    Resource.make(Sync[F].delay(new KafkaStreams(top, props)))(s => Sync[F].delay(s.close(timeout)).void)

  def runStreams[F[_]: Async](streams: KafkaStreams): F[Unit] = Async[F].async { (k: Either[Throwable, Unit] => Unit) =>
    streams.setUncaughtExceptionHandler { (_: Thread, e: Throwable) =>
      k(Left(e))
    }

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
