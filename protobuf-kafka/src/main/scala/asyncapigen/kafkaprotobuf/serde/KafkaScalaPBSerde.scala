package asyncapigen.kafkaprotobuf.serde

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import org.apache.kafka.common.serialization.{Serde, Serdes}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion, JavaProtoSupport}

object KafkaScalaPBSerde {

  def make[ScalaPB <: GeneratedMessage, JavaPB <: com.google.protobuf.Message](
      companion: GeneratedMessageCompanion[ScalaPB] with JavaProtoSupport[ScalaPB, JavaPB]
  ): Serde[ScalaPB] =
    Serdes.serdeFrom(
      KafkaScalaPBSerializer.make[ScalaPB, JavaPB](companion),
      KafkaScalaPBDeserializer.make[ScalaPB, JavaPB](companion)
    )

  def make[ScalaPB <: GeneratedMessage, JavaPB <: com.google.protobuf.Message](
      companion: GeneratedMessageCompanion[ScalaPB] with JavaProtoSupport[ScalaPB, JavaPB],
      client: SchemaRegistryClient
  ): Serde[ScalaPB] =
    Serdes.serdeFrom(
      KafkaScalaPBSerializer.make[ScalaPB, JavaPB](companion, client),
      KafkaScalaPBDeserializer.make[ScalaPB, JavaPB](companion, client)
    )
}
