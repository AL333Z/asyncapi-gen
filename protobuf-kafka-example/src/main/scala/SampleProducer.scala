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
