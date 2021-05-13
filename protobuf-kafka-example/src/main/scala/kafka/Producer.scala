package kafka

import cats.effect.{IO, Resource}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.Serializer
import org.typelevel.log4cats.Logger

import java.util.Properties

object Producer {
  def make[K, V](props: Properties, keySerde: Serializer[K], valueSerde: Serializer[V])(implicit
      logger: Logger[IO]
  ): Resource[IO, Producer[K, V]] =
    Resource
      .fromAutoCloseable(IO.delay(new KafkaProducer(props, keySerde, valueSerde)))
      .map(new Producer[K, V](_))
}

class Producer[K, V] private (kafkaProducer: KafkaProducer[K, V])(implicit logger: Logger[IO]) {
  def send(topic: String, key: K, value: V): IO[Unit] =
    logger.info(s"Sending $key -> $value to $topic...") >>
      IO.blocking {
        kafkaProducer.send(new ProducerRecord(topic, key, value)).get()
      } >>
      logger.info(s"Sent $key -> $value to $topic.")
}
