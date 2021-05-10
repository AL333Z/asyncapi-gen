package asyncapigen.kafkaprotobuf.serde

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.protobuf.{AbstractKafkaProtobufSerializer, KafkaProtobufSerializer}
import org.apache.kafka.common.serialization.Serializer
import scalapb.{GeneratedMessage, GeneratedMessageCompanion, JavaProtoSupport}

class KafkaScalaPBSerializer[ScalaPB <: GeneratedMessage, JavaPB <: com.google.protobuf.Message] private (
    companion: GeneratedMessageCompanion[ScalaPB] with JavaProtoSupport[ScalaPB, JavaPB],
    kafkaProtobufSerializer: KafkaProtobufSerializer[JavaPB]
) extends AbstractKafkaProtobufSerializer[JavaPB]
    with Serializer[ScalaPB] {

  override def configure(configs: java.util.Map[String, _], isKey: Boolean): Unit =
    kafkaProtobufSerializer.configure(configs, isKey)

  def serialize(topic: String, record: ScalaPB): Array[Byte] =
    if (record == null) {
      null
    } else {
      kafkaProtobufSerializer.serialize(topic, companion.toJavaProto(record))
    }
}

object KafkaScalaPBSerializer {
  def make[ScalaPB <: GeneratedMessage, JavaPB <: com.google.protobuf.Message](
      companion: GeneratedMessageCompanion[ScalaPB] with JavaProtoSupport[ScalaPB, JavaPB]
  ): KafkaScalaPBSerializer[ScalaPB, JavaPB] =
    new KafkaScalaPBSerializer[ScalaPB, JavaPB](companion, new KafkaProtobufSerializer[JavaPB]())

  def make[ScalaPB <: GeneratedMessage, JavaPB <: com.google.protobuf.Message](
      companion: GeneratedMessageCompanion[ScalaPB] with JavaProtoSupport[ScalaPB, JavaPB],
      client: SchemaRegistryClient
  ): KafkaScalaPBSerializer[ScalaPB, JavaPB] =
    new KafkaScalaPBSerializer[ScalaPB, JavaPB](companion, new KafkaProtobufSerializer[JavaPB](client))
}
