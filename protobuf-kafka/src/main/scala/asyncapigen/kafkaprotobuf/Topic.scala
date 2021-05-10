package asyncapigen.kafkaprotobuf

import asyncapigen.kafkaprotobuf.serde.KafkaScalaPBSerde
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import org.apache.kafka.common.serialization.Serde
import scalapb.{GeneratedMessage, GeneratedMessageCompanion, JavaProtoSupport}

// TODO add the type of the topic as well
abstract class Topic[ScalaPB <: GeneratedMessage](val name: String) {
  type JavaPB <: com.google.protobuf.Message
  type Companion = GeneratedMessageCompanion[ScalaPB] with JavaProtoSupport[ScalaPB, JavaPB]

  val companion: Companion
  val serde: Serde[ScalaPB]

  // TODO add `Consumed`, `Produced`, `Grouped`, etc..
}

object Topic {
  def mkTopic[ScalaPB <: GeneratedMessage, JavaPBMessage <: com.google.protobuf.Message](
      name: String,
      comp: GeneratedMessageCompanion[ScalaPB] with JavaProtoSupport[ScalaPB, JavaPBMessage]
  ): Topic[ScalaPB] = new Topic[ScalaPB](name) {
    override type JavaPB = JavaPBMessage
    override val companion: Companion  = comp
    override val serde: Serde[ScalaPB] = KafkaScalaPBSerde.make[ScalaPB, JavaPB](companion)
  }

  def mkTopic[ScalaPB <: GeneratedMessage, JavaPBMessage <: com.google.protobuf.Message](
      name: String,
      comp: GeneratedMessageCompanion[ScalaPB] with JavaProtoSupport[ScalaPB, JavaPBMessage],
      schemaRegistryClient: SchemaRegistryClient
  ): Topic[ScalaPB] = new Topic[ScalaPB](name) {
    override type JavaPB = JavaPBMessage
    override val companion: Companion  = comp
    override val serde: Serde[ScalaPB] = KafkaScalaPBSerde.make[ScalaPB, JavaPB](companion, schemaRegistryClient)
  }
}
