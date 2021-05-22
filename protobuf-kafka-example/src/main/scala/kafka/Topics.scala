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
