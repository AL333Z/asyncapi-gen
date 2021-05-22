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
