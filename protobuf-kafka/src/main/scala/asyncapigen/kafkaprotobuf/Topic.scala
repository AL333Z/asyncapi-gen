package asyncapigen.kafkaprotobuf

import asyncapigen.kafkaprotobuf.serde.KafkaScalaPBSerde
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import org.apache.kafka.common.serialization.{Serde, Serdes => JSerdes}
import org.apache.kafka.streams.scala.Serdes
import org.apache.kafka.streams.scala.kstream.{Consumed, Grouped, Produced}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion, JavaProtoSupport}

import java.util.UUID

// TODO add support for structured keys (and not only basic values)
abstract class Topic[KeyScalaPB, ValueScalaPB <: GeneratedMessage](val name: String) {
  type ValueJavaPB <: com.google.protobuf.Message
  type ValueCompanion = GeneratedMessageCompanion[ValueScalaPB] with JavaProtoSupport[ValueScalaPB, ValueJavaPB]

  val keySerde: Serde[KeyScalaPB]

  val valueCompanion: ValueCompanion
  val valueSerde: Serde[ValueScalaPB]

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
      kSerde: Serde[Key]
  ): Topic[Key, ValueScalaPB] = new Topic[Key, ValueScalaPB](name) {
    override type ValueJavaPB = ValueJavaPBMessage
    override val keySerde: Serde[Key]           = kSerde
    override val valueCompanion: ValueCompanion = valueComp
    override val valueSerde: Serde[ValueScalaPB] = schemaRegistryClient match {
      case Some(src) => KafkaScalaPBSerde.make[ValueScalaPB, ValueJavaPB](valueCompanion, src)
      case None      => KafkaScalaPBSerde.make[ValueScalaPB, ValueJavaPB](valueCompanion)
    }
  }

  def mkIntKeyedTopic[ValueScalaPB <: GeneratedMessage, ValueJavaPBMessage <: com.google.protobuf.Message](
      name: String,
      valueCompanion: GeneratedMessageCompanion[ValueScalaPB] with JavaProtoSupport[ValueScalaPB, ValueJavaPBMessage],
      schemaRegistryClient: Option[SchemaRegistryClient] = None
  ): Topic[Int, ValueScalaPB] =
    mkKeyedTopic[Int, ValueScalaPB, ValueJavaPBMessage](name, valueCompanion, schemaRegistryClient, Serdes.Integer)

  def mkLongKeyedTopic[ValueScalaPB <: GeneratedMessage, ValueJavaPBMessage <: com.google.protobuf.Message](
      name: String,
      valueCompanion: GeneratedMessageCompanion[ValueScalaPB] with JavaProtoSupport[ValueScalaPB, ValueJavaPBMessage],
      schemaRegistryClient: Option[SchemaRegistryClient] = None
  ): Topic[Long, ValueScalaPB] =
    mkKeyedTopic[Long, ValueScalaPB, ValueJavaPBMessage](name, valueCompanion, schemaRegistryClient, Serdes.Long)

  def mkStringKeyedTopic[ValueScalaPB <: GeneratedMessage, ValueJavaPBMessage <: com.google.protobuf.Message](
      name: String,
      valueCompanion: GeneratedMessageCompanion[ValueScalaPB] with JavaProtoSupport[ValueScalaPB, ValueJavaPBMessage],
      schemaRegistryClient: Option[SchemaRegistryClient] = None
  ): Topic[String, ValueScalaPB] =
    mkKeyedTopic[String, ValueScalaPB, ValueJavaPBMessage](name, valueCompanion, schemaRegistryClient, Serdes.String)

  def mkByteArrayKeyedTopic[ValueScalaPB <: GeneratedMessage, ValueJavaPBMessage <: com.google.protobuf.Message](
      name: String,
      valueCompanion: GeneratedMessageCompanion[ValueScalaPB] with JavaProtoSupport[ValueScalaPB, ValueJavaPBMessage],
      schemaRegistryClient: Option[SchemaRegistryClient] = None
  ): Topic[Array[Byte], ValueScalaPB] =
    mkKeyedTopic[Array[Byte], ValueScalaPB, ValueJavaPBMessage](
      name,
      valueCompanion,
      schemaRegistryClient,
      Serdes.ByteArray
    )

  def mkFloatKeyedTopic[ValueScalaPB <: GeneratedMessage, ValueJavaPBMessage <: com.google.protobuf.Message](
      name: String,
      valueCompanion: GeneratedMessageCompanion[ValueScalaPB] with JavaProtoSupport[ValueScalaPB, ValueJavaPBMessage],
      schemaRegistryClient: Option[SchemaRegistryClient] = None
  ): Topic[Float, ValueScalaPB] =
    mkKeyedTopic[Float, ValueScalaPB, ValueJavaPBMessage](name, valueCompanion, schemaRegistryClient, Serdes.Float)

  def mkDoubleKeyedTopic[ValueScalaPB <: GeneratedMessage, ValueJavaPBMessage <: com.google.protobuf.Message](
      name: String,
      valueCompanion: GeneratedMessageCompanion[ValueScalaPB] with JavaProtoSupport[ValueScalaPB, ValueJavaPBMessage],
      schemaRegistryClient: Option[SchemaRegistryClient] = None
  ): Topic[Double, ValueScalaPB] =
    mkKeyedTopic[Double, ValueScalaPB, ValueJavaPBMessage](name, valueCompanion, schemaRegistryClient, Serdes.Double)

  def mkUUIDKeyedTopic[ValueScalaPB <: GeneratedMessage, ValueJavaPBMessage <: com.google.protobuf.Message](
      name: String,
      valueCompanion: GeneratedMessageCompanion[ValueScalaPB] with JavaProtoSupport[ValueScalaPB, ValueJavaPBMessage],
      schemaRegistryClient: Option[SchemaRegistryClient] = None
  ): Topic[UUID, ValueScalaPB] =
    mkKeyedTopic[UUID, ValueScalaPB, ValueJavaPBMessage](name, valueCompanion, schemaRegistryClient, JSerdes.UUID())

}
