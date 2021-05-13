package kafka

import cats.effect.{IO, Resource}
import org.apache.kafka.clients.admin.{AdminClient, NewTopic}
import org.typelevel.log4cats.Logger

import java.util.Properties
import scala.jdk.CollectionConverters._

class Topics private (props: Properties)(implicit logger: Logger[IO]) {

  def createIfMissing(topics: Set[String]): IO[Unit] =
    Resource
      .fromAutoCloseable(IO(AdminClient.create(props)))
      .use { adminClient =>
        IO.blocking {
          val existingTopics = adminClient.listTopics().names().get.asScala
          val topicsToCreate = topics.diff(existingTopics)
          val res            = adminClient.createTopics(topicsToCreate.map(new NewTopic(_, 1, 1.toShort)).asJava).all()
          (topicsToCreate, res)
        }.flatMap { case (topicsToCreate, res) => logger.info(s"Topics created $topicsToCreate. $res") }
      }
}

object Topics {
  def from(props: Properties)(implicit logger: Logger[IO]): Topics = new Topics(props)
}
