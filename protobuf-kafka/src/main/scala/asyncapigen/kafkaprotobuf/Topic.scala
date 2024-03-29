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

package asyncapigen.kafkaprotobuf

import asyncapigen.kafkaprotobuf.serde.KafkaScalaPBSerde
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializerConfig
import org.apache.kafka.common.serialization.{Serde, Serdes => JSerdes}
import org.apache.kafka.streams.scala.serialization.Serdes
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
      Serdes.intSerde,
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
      Serdes.longSerde,
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
      Serdes.stringSerde,
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
      Serdes.byteArraySerde,
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
      Serdes.floatSerde,
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
      Serdes.doubleSerde,
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
