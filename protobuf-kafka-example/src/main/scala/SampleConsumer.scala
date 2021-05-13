//import asyncapigen.kafkaprotobuf.Topic
//import cats.effect.{IO, IOApp, Resource, Sync}
//import cats.implicits._
//import gen.{Topics, UserSignedUp}
//import kafka.Platform
//import org.apache.kafka.streams.kstream.KStream
//import org.apache.kafka.streams.{KafkaStreams, StreamsBuilder, Topology}
//import org.typelevel.log4cats.SelfAwareStructuredLogger
//import org.typelevel.log4cats.slf4j.Slf4jLogger
//
//import java.time.Duration
//import java.util.Properties
//
//object SampleConsumer extends IOApp.Simple {
//  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]
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
//    val props              = buildKafkaProps
//    val sb: StreamsBuilder = new StreamsBuilder
//
//    val input: KStream[String, UserSignedUp] = sb.stream(topic.name, topic.consumed)
//    input.foreach((key: String, value: UserSignedUp) => logger.info(s"Got $key -> $value").unsafeRunSync()(runtime))
//    val topology = sb.build(props)
//
//    kafka.Topics.from(props).createIfMissing(Set(topic.name)) >>
//      Platform.run[IO](topology, props, Duration.ofSeconds(2))
//  }
//
//  def streamsResource[F[_]: Sync](top: Topology, props: Properties, timeout: Duration): Resource[F, KafkaStreams] =
//    Resource.make(Sync[F].delay(new KafkaStreams(top, props)))(s => Sync[F].delay(s.close(timeout)).void)
//
//  private def buildKafkaProps: Properties = {
//    val props: Properties = new Properties
//    props.setProperty("bootstrap.servers", "localhost:9092")
//    props.setProperty("application.id", "app-publisher")
//    props.setProperty("auto.register.schemas", "true")
//    props.setProperty("use.latest.version", "true")
//    props.setProperty("schema.registry.url", "localhost:8081")
//    props
//  }
//
//}
