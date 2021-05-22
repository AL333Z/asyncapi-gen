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

//import asyncapigen.kafkaprotobuf.Topic
//import cats.effect.{IO, IOApp}
//import cats.implicits._
//import gen.{Topics, UserSignedUp}
//import kafka.Producer
//import org.typelevel.log4cats.SelfAwareStructuredLogger
//import org.typelevel.log4cats.slf4j.Slf4jLogger
//
//import java.util.Properties
//
//object SampleProducer extends IOApp.Simple {
//  implicit val unsafeLogger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]
//
//  val topic: Topic[String, UserSignedUp] = Topics.userEvents(
//    Map(
//      "auto.register.schemas" -> "true",
//      "use.latest.version"    -> "true",
//      "schema.registry.url"   -> "http://localhost:8081"
//    )
//  )
//
//  override def run: IO[Unit] = {
//    val props = buildKafkaProps
//
//    kafka.Topics.from(props).createIfMissing(Set(topic.name)) >>
//      Producer.make[String, UserSignedUp](props, topic.keySerde.serializer(), topic.valueSerde.serializer()).use {
//        producer =>
//          messages.traverse_ { msg =>
//            producer.send(topic.name, msg.email, msg)
//          }
//      }
//  }
//
//  private def buildKafkaProps: Properties = {
//    val props: Properties = new Properties
//    props.setProperty("bootstrap.servers", "localhost:9092")
//    props.setProperty("application.id", "app-publisher")
//    props
//  }
//
//  private def messages: List[UserSignedUp] = {
//    for {
//      i <- (0 until 10).toList
//      y <- (10 until 20).toList
//    } yield UserSignedUp(Some(s"al333z$i"), s"email@example.com$y")
//  }
//}
