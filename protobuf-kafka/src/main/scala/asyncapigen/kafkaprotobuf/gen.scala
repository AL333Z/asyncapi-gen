package asyncapigen.kafkaprotobuf

import asyncapigen.schema.{AsyncApi, Message, Reference}
import cats.effect.IO
import cats.implicits._

import scala.annotation.tailrec
import scala.util.{Success, Try}

object gen {
  def run(asyncApi: AsyncApi, targetPackageName: String, scalaTargetFolder: String): IO[Unit] = {
    val topicSpecs = asyncApi.channels.toList
      .traverse { case (name, item) =>
        // we just want to describe THE schema schema for the topic, so why multiple ops? the schema should cover all possible messages
        val op = (item.publish ++ item.subscribe).toList.head
        extractMessage(asyncApi, op.message).map(_.map(m => (name, m)))
      }
      .map(_.flatten)

    // TODO generate `Topic` instances in `scalaTargetFolder`, using `targetPackageName` with metaprogramming!
    ???
  }

  @tailrec
  private def extractMessage(
      asyncApi: AsyncApi,
      message: Option[Either[Message, Reference]]
  ): Try[Option[Message]] =
    message match {
      case Some(Left(message)) => Success(Some(message))
      case Some(Right(ref))    => extractMessage(asyncApi, asyncApi.resolveMessageFromRef(ref)._2)
      case _                   => Success(None)
    }
}
