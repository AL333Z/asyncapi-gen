package asyncapigen

import asyncapigen.ParseAsyncApi.YamlSource
import asyncapigen.schema.Channel.{ItemObject, Operation}
import asyncapigen.schema.Schema.BasicSchema.StringSchema
import asyncapigen.schema.Schema.ObjectSchema
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
                          "displayName" -> StringSchema,
                          "email"       -> StringSchema
                        )
                      )
                    ),
                    tags = Nil,
                    name = None,
                    description = None
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
