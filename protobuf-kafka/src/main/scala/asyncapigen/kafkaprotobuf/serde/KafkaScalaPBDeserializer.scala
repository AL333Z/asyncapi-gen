package asyncapigen.kafkaprotobuf.serde

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.protobuf.{AbstractKafkaProtobufDeserializer, KafkaProtobufDeserializer}
import org.apache.kafka.common.serialization.Deserializer
import scalapb.{GeneratedMessage, GeneratedMessageCompanion, JavaProtoSupport}

class KafkaScalaPBDeserializer[ScalaPB <: GeneratedMessage, JavaPB <: com.google.protobuf.Message] private (
    companion: GeneratedMessageCompanion[ScalaPB] with JavaProtoSupport[ScalaPB, JavaPB],
    kafkaProtobufDeserializer: KafkaProtobufDeserializer[JavaPB]
) extends AbstractKafkaProtobufDeserializer[JavaPB]
    with Deserializer[ScalaPB] {
  override def configure(configs: java.util.Map[String, _], isKey: Boolean): Unit =
    kafkaProtobufDeserializer.configure(configs, isKey)

  def deserialize(s: String, bytes: Array[Byte]): ScalaPB =
    companion.fromJavaProto(kafkaProtobufDeserializer.deserialize(s, bytes))
}

object KafkaScalaPBDeserializer {
  def make[ScalaPB <: GeneratedMessage, JavaPB <: com.google.protobuf.Message](
      companion: GeneratedMessageCompanion[ScalaPB] with JavaProtoSupport[ScalaPB, JavaPB]
  ) =
    new KafkaScalaPBDeserializer[ScalaPB, JavaPB](companion, new KafkaProtobufDeserializer[JavaPB]())

  def make[ScalaPB <: GeneratedMessage, JavaPB <: com.google.protobuf.Message](
      companion: GeneratedMessageCompanion[ScalaPB] with JavaProtoSupport[ScalaPB, JavaPB],
      client: SchemaRegistryClient
  ) =
    new KafkaScalaPBDeserializer[ScalaPB, JavaPB](companion, new KafkaProtobufDeserializer[JavaPB](client))

}
