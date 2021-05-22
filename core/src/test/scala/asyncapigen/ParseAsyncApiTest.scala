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

package asyncapigen

import asyncapigen.ParseAsyncApi.YamlSource
import asyncapigen.schema.Channel.{ItemObject, Operation}
import asyncapigen.schema.Message.Bindings
import asyncapigen.schema.Message.Bindings.KafkaBinding
import asyncapigen.schema.Schema.BasicSchema.StringSchema
import asyncapigen.schema.Schema.{BasicSchema, BasicSchemaValue, CustomFields, ObjectSchema}
import asyncapigen.schema.{AsyncApi, Info, Message}
import cats.effect.IO
import munit.CatsEffectSuite

import java.io.File

class ParseAsyncApiTest extends CatsEffectSuite {

  // TODO add more more fields and split to multiple smaller specs
  test("parse Yaml AsyncApi spec") {
    val expected = AsyncApi(
      asyncapi = "2.0.0",
      id = None,
      info = Info(
        title = "Account Service",
        description = Some("This service is in charge of processing user signups"),
        version = "1.0.0"
      ),
      servers = List(),
      channels = Map(
        "user/signedup" -> ItemObject(
          ref = None,
          description = None,
          subscribe = Some(
            Operation(
              operationId = None,
              summary = None,
              description = None,
              tags = List(),
              externalDocs = None,
              message = Some(
                Left(
                  value = Message(
                    payload = Left(
                      value = ObjectSchema(
                        required = Nil,
                        properties = Map(
                          "displayName" -> ObjectSchema
                            .Elem(
                              StringSchema,
                              CustomFields(Map("x-protobuf-index" -> BasicSchemaValue.IntegerValue(1)))
                            ),
                          "email" -> ObjectSchema
                            .Elem(
                              StringSchema,
                              CustomFields(Map("x-protobuf-index" -> BasicSchemaValue.IntegerValue(2)))
                            )
                        )
                      )
                    ),
                    tags = Nil,
                    name = None,
                    description = None,
                    bindings = Some(
                      value = Bindings(
                        kafka = Some(KafkaBinding(BasicSchema.IntegerSchema))
                      )
                    )
                  )
                )
              )
            )
          ),
          publish = None,
          parameters = List()
        )
      ),
      components = None,
      tags = List(),
      externalDocs = None
    )

    for {
      file <- IO.delay(new File(getClass.getResource(s"/basic-asyncapi.yaml").toURI))
      res  <- ParseAsyncApi.parseYamlAsyncApiSource[IO](YamlSource(file))
    } yield assertEquals(res, expected)
  }

}
