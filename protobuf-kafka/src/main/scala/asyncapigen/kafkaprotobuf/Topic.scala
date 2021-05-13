package asyncapigen.kafkaprotobuf

import asyncapigen.kafkaprotobuf.serde.KafkaScalaPBSerde
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializerConfig
import org.apache.kafka.common.serialization.{Serde, Serdes => JSerdes}
import org.apache.kafka.streams.scala.Serdes
import org.apache.kafka.streams.scala.kstream.{Consumed, Grouped, Produced}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion, JavaProtoSupport}

import java.util.UUID
import scala.jdk.CollectionConverters.MapHasAsJava
import scala.reflect.ClassTag

// TODO add support for structured keys (and not only basic values)
abstract class Topic[KeyScalaPB, ValueScalaPB <: GeneratedMessage](val name: String) {
  type ValueJavaPB <: com.google.protobuf.Message
  type ValueCompanion = GeneratedMessageCompanion[ValueScalaPB] with JavaProtoSupport[ValueScalaPB, ValueJavaPB]

  def keySerde: Serde[KeyScalaPB]

  def valueCompanion: ValueCompanion
  def valueSerde: Serde[ValueScalaPB]
  def valueClassTag: ClassTag[ValueJavaPB]

  // TODO add remaining..
  val consumed: Consumed[KeyScalaPB, ValueScalaPB] = Consumed.`with`(keySerde, valueSerde)
  val produced: Produced[KeyScalaPB, ValueScalaPB] = Produced.`with`(keySerde, valueSerde)
  val grouped: Grouped[KeyScalaPB, ValueScalaPB]   = Grouped.`with`(keySerde, valueSerde)
}

object Topic {

  def mkKeyedTopic[Key, ValueScalaPB <: GeneratedMessage, ValueJavaPBMessage <: com.google.protobuf.Message](
      name: String,
      valueComp: GeneratedMessageCompanion[ValueScalaPB] with JavaProtoSupport[ValueScalaPB, ValueJavaPBMessage],
      schemaRegistryClient: Option[SchemaRegistryClient] = None,
      kSerde: Serde[Key],
      serdeConfig: Map[String, Any] = Map()
  )(implicit valueCT: ClassTag[ValueJavaPBMessage]): Topic[Key, ValueScalaPB] = new Topic[Key, ValueScalaPB](name) {
    override type ValueJavaPB = ValueJavaPBMessage
    override def keySerde: Serde[Key]           = kSerde
    override def valueCompanion: ValueCompanion = valueComp
    override def valueSerde: Serde[ValueScalaPB] = {
      val serde = schemaRegistryClient match {
        case Some(src) => KafkaScalaPBSerde.make[ValueScalaPB, ValueJavaPB](valueCompanion, src)
        case None      => KafkaScalaPBSerde.make[ValueScalaPB, ValueJavaPB](valueCompanion)
      }
      serde.configure(
        (serdeConfig ++ Map(
          KafkaProtobufDeserializerConfig.SPECIFIC_PROTOBUF_VALUE_TYPE -> valueClassTag.runtimeClass
        )).asJava,
        false
      )
      serde
    }
    override def valueClassTag: ClassTag[ValueJavaPBMessage] = valueCT
  }

  def mkIntKeyedTopic[ValueScalaPB <: GeneratedMessage, ValueJavaPBMessage <: com.google.protobuf.Message](
      name: String,
      valueCompanion: GeneratedMessageCompanion[ValueScalaPB] with JavaProtoSupport[ValueScalaPB, ValueJavaPBMessage],
      schemaRegistryClient: Option[SchemaRegistryClient] = None,
      serdeConfig: Map[String, Any] = Map()
  )(implicit valueClassTag: ClassTag[ValueJavaPBMessage]): Topic[Int, ValueScalaPB] =
    mkKeyedTopic[Int, ValueScalaPB, ValueJavaPBMessage](
      name,
      valueCompanion,
      schemaRegistryClient,
      Serdes.Integer,
      serdeConfig
    )

  def mkLongKeyedTopic[ValueScalaPB <: GeneratedMessage, ValueJavaPBMessage <: com.google.protobuf.Message](
      name: String,
      valueCompanion: GeneratedMessageCompanion[ValueScalaPB] with JavaProtoSupport[ValueScalaPB, ValueJavaPBMessage],
      schemaRegistryClient: Option[SchemaRegistryClient] = None,
      serdeConfig: Map[String, Any] = Map()
  )(implicit valueClassTag: ClassTag[ValueJavaPBMessage]): Topic[Long, ValueScalaPB] =
    mkKeyedTopic[Long, ValueScalaPB, ValueJavaPBMessage](
      name,
      valueCompanion,
      schemaRegistryClient,
      Serdes.Long,
      serdeConfig
    )

  def mkStringKeyedTopic[ValueScalaPB <: GeneratedMessage, ValueJavaPBMessage <: com.google.protobuf.Message](
      name: String,
      valueCompanion: GeneratedMessageCompanion[ValueScalaPB] with JavaProtoSupport[ValueScalaPB, ValueJavaPBMessage],
      schemaRegistryClient: Option[SchemaRegistryClient] = None,
      serdeConfig: Map[String, Any] = Map()
  )(implicit valueClassTag: ClassTag[ValueJavaPBMessage]): Topic[String, ValueScalaPB] =
    mkKeyedTopic[String, ValueScalaPB, ValueJavaPBMessage](
      name,
      valueCompanion,
      schemaRegistryClient,
      Serdes.String,
      serdeConfig
    )

  def mkByteArrayKeyedTopic[ValueScalaPB <: GeneratedMessage, ValueJavaPBMessage <: com.google.protobuf.Message](
      name: String,
      valueCompanion: GeneratedMessageCompanion[ValueScalaPB] with JavaProtoSupport[ValueScalaPB, ValueJavaPBMessage],
      schemaRegistryClient: Option[SchemaRegistryClient] = None,
      serdeConfig: Map[String, Any] = Map()
  )(implicit valueClassTag: ClassTag[ValueJavaPBMessage]): Topic[Array[Byte], ValueScalaPB] =
    mkKeyedTopic[Array[Byte], ValueScalaPB, ValueJavaPBMessage](
      name,
      valueCompanion,
      schemaRegistryClient,
      Serdes.ByteArray,
      serdeConfig
    )

  def mkFloatKeyedTopic[ValueScalaPB <: GeneratedMessage, ValueJavaPBMessage <: com.google.protobuf.Message](
      name: String,
      valueCompanion: GeneratedMessageCompanion[ValueScalaPB] with JavaProtoSupport[ValueScalaPB, ValueJavaPBMessage],
      schemaRegistryClient: Option[SchemaRegistryClient] = None,
      serdeConfig: Map[String, Any] = Map()
  )(implicit valueClassTag: ClassTag[ValueJavaPBMessage]): Topic[Float, ValueScalaPB] =
    mkKeyedTopic[Float, ValueScalaPB, ValueJavaPBMessage](
      name,
      valueCompanion,
      schemaRegistryClient,
      Serdes.Float,
      serdeConfig
    )

  def mkDoubleKeyedTopic[ValueScalaPB <: GeneratedMessage, ValueJavaPBMessage <: com.google.protobuf.Message](
      name: String,
      valueCompanion: GeneratedMessageCompanion[ValueScalaPB] with JavaProtoSupport[ValueScalaPB, ValueJavaPBMessage],
      schemaRegistryClient: Option[SchemaRegistryClient] = None,
      serdeConfig: Map[String, Any] = Map()
  )(implicit valueClassTag: ClassTag[ValueJavaPBMessage]): Topic[Double, ValueScalaPB] =
    mkKeyedTopic[Double, ValueScalaPB, ValueJavaPBMessage](
      name,
      valueCompanion,
      schemaRegistryClient,
      Serdes.Double,
      serdeConfig
    )

  def mkUUIDKeyedTopic[ValueScalaPB <: GeneratedMessage, ValueJavaPBMessage <: com.google.protobuf.Message](
      name: String,
      valueCompanion: GeneratedMessageCompanion[ValueScalaPB] with JavaProtoSupport[ValueScalaPB, ValueJavaPBMessage],
      schemaRegistryClient: Option[SchemaRegistryClient] = None,
      serdeConfig: Map[String, Any] = Map()
  )(implicit valueClassTag: ClassTag[ValueJavaPBMessage]): Topic[UUID, ValueScalaPB] =
    mkKeyedTopic[UUID, ValueScalaPB, ValueJavaPBMessage](
      name,
      valueCompanion,
      schemaRegistryClient,
      JSerdes.UUID(),
      serdeConfig
    )

}
