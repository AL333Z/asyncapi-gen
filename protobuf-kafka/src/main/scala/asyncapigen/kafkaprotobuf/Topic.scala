package asyncapigen.kafkaprotobuf

import asyncapigen.kafkaprotobuf.serde.KafkaScalaPBSerde
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import org.apache.kafka.common.serialization.{Serde, Serdes => JSerdes}
import org.apache.kafka.streams.scala.Serdes
import org.apache.kafka.streams.scala.kstream.Consumed
import scalapb.{GeneratedMessage, GeneratedMessageCompanion, JavaProtoSupport}

import java.util.UUID

// TODO add support for structured keys (and not only basic values)
abstract class Topic[KeyScalaPB, ValueScalaPB <: GeneratedMessage](val name: String) {
  type ValueJavaPB <: com.google.protobuf.Message
  type ValueCompanion = GeneratedMessageCompanion[ValueScalaPB] with JavaProtoSupport[ValueScalaPB, ValueJavaPB]

  val keySerde: Serde[KeyScalaPB]

  val valueCompanion: ValueCompanion
  val valueSerde: Serde[ValueScalaPB]

  // TODO add `Consumed`, `Produced`, `Grouped`, etc..
  val consumed: Consumed[KeyScalaPB, ValueScalaPB] = Consumed.`with`(keySerde, valueSerde)
}

// TODO reduce duplication
object Topic {
  def mkIntKeyedTopic[ValueScalaPB <: GeneratedMessage, ValueJavaPBMessage <: com.google.protobuf.Message](
      name: String,
      valueComp: GeneratedMessageCompanion[ValueScalaPB] with JavaProtoSupport[ValueScalaPB, ValueJavaPBMessage],
      schemaRegistryClient: Option[SchemaRegistryClient] = None
  ): Topic[Int, ValueScalaPB] = new Topic[Int, ValueScalaPB](name) {
    override type ValueJavaPB = ValueJavaPBMessage
    override val keySerde: Serde[Int]           = Serdes.Integer
    override val valueCompanion: ValueCompanion = valueComp
    override val valueSerde: Serde[ValueScalaPB] = schemaRegistryClient match {
      case Some(src) => KafkaScalaPBSerde.make[ValueScalaPB, ValueJavaPB](valueCompanion, src)
      case None      => KafkaScalaPBSerde.make[ValueScalaPB, ValueJavaPB](valueCompanion)
    }
  }

  def mkLongKeyedTopic[ValueScalaPB <: GeneratedMessage, ValueJavaPBMessage <: com.google.protobuf.Message](
      name: String,
      valueComp: GeneratedMessageCompanion[ValueScalaPB] with JavaProtoSupport[ValueScalaPB, ValueJavaPBMessage],
      schemaRegistryClient: Option[SchemaRegistryClient] = None
  ): Topic[Long, ValueScalaPB] = new Topic[Long, ValueScalaPB](name) {
    override type ValueJavaPB = ValueJavaPBMessage
    override val keySerde: Serde[Long]          = Serdes.Long
    override val valueCompanion: ValueCompanion = valueComp
    override val valueSerde: Serde[ValueScalaPB] = schemaRegistryClient match {
      case Some(src) => KafkaScalaPBSerde.make[ValueScalaPB, ValueJavaPB](valueCompanion, src)
      case None      => KafkaScalaPBSerde.make[ValueScalaPB, ValueJavaPB](valueCompanion)
    }
  }

  def mkStringKeyedTopic[ValueScalaPB <: GeneratedMessage, ValueJavaPBMessage <: com.google.protobuf.Message](
      name: String,
      valueComp: GeneratedMessageCompanion[ValueScalaPB] with JavaProtoSupport[ValueScalaPB, ValueJavaPBMessage],
      schemaRegistryClient: Option[SchemaRegistryClient] = None
  ): Topic[String, ValueScalaPB] = new Topic[String, ValueScalaPB](name) {
    override type ValueJavaPB = ValueJavaPBMessage
    override val keySerde: Serde[String]        = Serdes.String
    override val valueCompanion: ValueCompanion = valueComp
    override val valueSerde: Serde[ValueScalaPB] = schemaRegistryClient match {
      case Some(src) => KafkaScalaPBSerde.make[ValueScalaPB, ValueJavaPB](valueCompanion, src)
      case None      => KafkaScalaPBSerde.make[ValueScalaPB, ValueJavaPB](valueCompanion)
    }
  }

  def mkByteArrayKeyedTopic[ValueScalaPB <: GeneratedMessage, ValueJavaPBMessage <: com.google.protobuf.Message](
      name: String,
      valueComp: GeneratedMessageCompanion[ValueScalaPB] with JavaProtoSupport[ValueScalaPB, ValueJavaPBMessage],
      schemaRegistryClient: Option[SchemaRegistryClient] = None
  ): Topic[Array[Byte], ValueScalaPB] = new Topic[Array[Byte], ValueScalaPB](name) {
    override type ValueJavaPB = ValueJavaPBMessage
    override val keySerde: Serde[Array[Byte]]   = Serdes.ByteArray
    override val valueCompanion: ValueCompanion = valueComp
    override val valueSerde: Serde[ValueScalaPB] = schemaRegistryClient match {
      case Some(src) => KafkaScalaPBSerde.make[ValueScalaPB, ValueJavaPB](valueCompanion, src)
      case None      => KafkaScalaPBSerde.make[ValueScalaPB, ValueJavaPB](valueCompanion)
    }
  }

  def mkFloatKeyedTopic[ValueScalaPB <: GeneratedMessage, ValueJavaPBMessage <: com.google.protobuf.Message](
      name: String,
      valueComp: GeneratedMessageCompanion[ValueScalaPB] with JavaProtoSupport[ValueScalaPB, ValueJavaPBMessage],
      schemaRegistryClient: Option[SchemaRegistryClient] = None
  ): Topic[Float, ValueScalaPB] = new Topic[Float, ValueScalaPB](name) {
    override type ValueJavaPB = ValueJavaPBMessage
    override val keySerde: Serde[Float]         = Serdes.Float
    override val valueCompanion: ValueCompanion = valueComp
    override val valueSerde: Serde[ValueScalaPB] = schemaRegistryClient match {
      case Some(src) => KafkaScalaPBSerde.make[ValueScalaPB, ValueJavaPB](valueCompanion, src)
      case None      => KafkaScalaPBSerde.make[ValueScalaPB, ValueJavaPB](valueCompanion)
    }
  }

  def mkDoubleKeyedTopic[ValueScalaPB <: GeneratedMessage, ValueJavaPBMessage <: com.google.protobuf.Message](
      name: String,
      valueComp: GeneratedMessageCompanion[ValueScalaPB] with JavaProtoSupport[ValueScalaPB, ValueJavaPBMessage],
      schemaRegistryClient: Option[SchemaRegistryClient] = None
  ): Topic[Double, ValueScalaPB] = new Topic[Double, ValueScalaPB](name) {
    override type ValueJavaPB = ValueJavaPBMessage
    override val keySerde: Serde[Double]        = Serdes.Double
    override val valueCompanion: ValueCompanion = valueComp
    override val valueSerde: Serde[ValueScalaPB] = schemaRegistryClient match {
      case Some(src) => KafkaScalaPBSerde.make[ValueScalaPB, ValueJavaPB](valueCompanion, src)
      case None      => KafkaScalaPBSerde.make[ValueScalaPB, ValueJavaPB](valueCompanion)
    }
  }

  def mkUUIDKeyedTopic[ValueScalaPB <: GeneratedMessage, ValueJavaPBMessage <: com.google.protobuf.Message](
      name: String,
      valueComp: GeneratedMessageCompanion[ValueScalaPB] with JavaProtoSupport[ValueScalaPB, ValueJavaPBMessage],
      schemaRegistryClient: Option[SchemaRegistryClient] = None
  ): Topic[UUID, ValueScalaPB] = new Topic[UUID, ValueScalaPB](name) {
    override type ValueJavaPB = ValueJavaPBMessage
    override val keySerde: Serde[UUID]          = JSerdes.UUID()
    override val valueCompanion: ValueCompanion = valueComp
    override val valueSerde: Serde[ValueScalaPB] = schemaRegistryClient match {
      case Some(src) => KafkaScalaPBSerde.make[ValueScalaPB, ValueJavaPB](valueCompanion, src)
      case None      => KafkaScalaPBSerde.make[ValueScalaPB, ValueJavaPB](valueCompanion)
    }
  }

}
