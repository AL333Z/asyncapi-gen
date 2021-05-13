package asyncapigen.kafkaprotobuf

import asyncapigen.schema.Schema.BasicSchema
import asyncapigen.schema.{AsyncApi, Message, Reference, RichString}
import cats.effect.{IO, Resource}
import cats.implicits._
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.io.{File, PrintWriter}
import scala.annotation.tailrec
import scala.util.{Success, Try}

object gen {
  def run(
      asyncApi: AsyncApi,
      targetPackageName: String,
      schemaTargetFolder: String,
      scalaTargetFolder: String,
      javaTargetFolder: String
  ): IO[Unit] = for {
    logger <- Slf4jLogger.create[IO]
    _ <- asyncapigen.protobuf.gen.run(
      asyncApi = asyncApi,
      targetPackageName = targetPackageName,
      schemaTargetFolder = schemaTargetFolder,
      scalaTargetFolder = scalaTargetFolder,
      javaTargetFolder = javaTargetFolder
    )
    topicSpecs <- IO.fromTry(
      asyncApi.channels.toList
        .traverse { case (name, item) =>
          // we just want to describe THE schema schema for the topic, so why multiple ops? the schema should cover all possible messages
          val op = (item.publish ++ item.subscribe).toList.head
          extractMessage(asyncApi, op.message).map(_.map(m => (name, m)))
        }
        .map(_.flatten)
    )
    _ <- logger.info(s"Got topic specs: $topicSpecs")
    content         = getTopics(targetPackageName, topicSpecs)
    topicTargetFile = s"$scalaTargetFolder/${targetPackageName.replace('.', '/')}/Topics.scala"
    _ <- logger.info(s"Saving to file $topicTargetFile...")
    _ <- IO.delay(new File(scalaTargetFolder).mkdirs())
    _ <- Resource
      .fromAutoCloseable(IO.delay(new PrintWriter(topicTargetFile)))
      .use(w => IO.delay(w.write(content)))
    _ <- logger.info("All done.")
  } yield ()

  private def getTopics(targetPackageName: String, topicSpecs: List[(String, Message)]): String =
    s"""package $targetPackageName
       |
       |import asyncapigen.kafkaprotobuf.Topic
       |import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
       |
       |object Topics {
       |${topicSpecs
      .map { case (name, message) => genTopic(name, message, targetPackageName) }
      .mkString("\n")}
       |}
       |""".stripMargin

  private def genTopic(topicName: String, message: Message, targetPackageName: String): String = {
    val keySchema: BasicSchema           = message.bindings.flatMap(_.kafka).map(_.key).getOrElse(BasicSchema.StringSchema)
    val (keyKafkaType, keyKafkaTypeName) = basicSchemaToKafkaType(keySchema)
    val eventName                        = message.name.get.toJavaClassCompatible
    val name                             = topicName.toJavaClassCompatible
    val scalaValueClass                  = s"$targetPackageName.$eventName"
    val javaValueClass                   = s"$targetPackageName.$name.$eventName"

    s"""
       |  def ${name.lowercaseFirstLetter}: Topic[$keyKafkaType, $scalaValueClass] = 
       |    Topic.mk${keyKafkaTypeName}KeyedTopic[$scalaValueClass, $javaValueClass](
       |      name = "$topicName",
       |      valueCompanion = $scalaValueClass,
       |      schemaRegistryClient = None,
       |      serdeConfig = Map.empty[String, Any]
       |    )
       |    
       |  def ${name.lowercaseFirstLetter}(serdeConfig: Map[String, Any]): Topic[$keyKafkaType, $scalaValueClass] = 
       |    Topic.mk${keyKafkaTypeName}KeyedTopic[$scalaValueClass, $javaValueClass](
       |      name = "$topicName",
       |      valueCompanion = $scalaValueClass,
       |      schemaRegistryClient = None,
       |      serdeConfig = serdeConfig
       |    )
       |    
       |  def ${name.lowercaseFirstLetter}(schemaRegistryClient: SchemaRegistryClient, serdeConfig: Map[String, Any] = Map()): Topic[$keyKafkaType, $scalaValueClass] = 
       |    Topic.mk${keyKafkaTypeName}KeyedTopic[$scalaValueClass, $javaValueClass](
       |      name = "$topicName",
       |      valueCompanion = $scalaValueClass,
       |      schemaRegistryClient = Some(schemaRegistryClient),
       |      serdeConfig = serdeConfig
       |    )
       |""".stripMargin
  }

  private def basicSchemaToKafkaType(basicSchema: BasicSchema): (String, String) = basicSchema match {
    case BasicSchema.IntegerSchema  => ("Int", "Int")
    case BasicSchema.LongSchema     => ("Long", "Long")
    case BasicSchema.FloatSchema    => ("Float", "Float")
    case BasicSchema.DoubleSchema   => ("Double", "Double")
    case BasicSchema.ByteSchema     => ("String", "String") // TODO
    case BasicSchema.BinarySchema   => ("Array[Byte]", "ByteArray")
    case BasicSchema.BooleanSchema  => ("String", "String") // TODO
    case BasicSchema.DateSchema     => ("String", "String") // TODO
    case BasicSchema.DateTimeSchema => ("String", "String") // TODO
    case BasicSchema.PasswordSchema => ("String", "String")
    case BasicSchema.UUIDSchema     => ("java.util.UUID", "UUID")
    case BasicSchema.StringSchema   => ("String", "String")
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
