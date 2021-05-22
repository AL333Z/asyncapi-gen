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
