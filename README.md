An attempt to transform an [asyncapi](https://www.asyncapi.com/docs/specifications/2.0.0) spec to a target schema.

Ideally this should:
- [x] decode an asyncapi spec
- [ ] be able to generate the schema for the models in:
    - [x] `protobuf`: able to output a .proto file content
    - [ ] `json`
    - [ ] `avro` 
- [ ] be able to generate the scala code for the models (models, serdes)
    - [x] `protobuf`: done via `scalapb`
    - [ ] `json`
    - [ ] `avro`

# Rationale

This can be useful if in your organization there's a definition-first approach to define event-streaming platforms.

You can attach this codegen tool to a PR merge, commit or any relevant event of your event definition lifecycle, 
so that the events configured will auto-magically be transformed in all the sources needed from consumer/producer applications.
After the generation, a good approach would be to actually tag the generated artifacts with a version and upload to a registry.

The reason behind this is that we should only be defining our contracts once, and let the machine generate the low level 
details such as value classes, serdes, etc.

# Example

- Check and run the main in `protobuf-kafka-example/src/main/scala/gen/Gen.scala`. 
  This will generate the schema (`.proto`) and the sources (`java` and `scala`) for the models and for the serdes, and will
  return a `Topics` companion object which will offer the user a nice way to consume/produce from/to a configured topic (e.g. `user_events`).
```scala
 object Topics {
   def userEvents: Topic[Int, gen.UserSignedUp] = ???
  // ...
 }
```
- run `docker-compose -f "protobuf-kafka-example/docker-compose.yml" up`, to turn on locally a working kafka env.
- To see how the generated sources can be used, uncomment and run the `SampleConsumer.scala` and `SampleProducer.scala`.
- You should see messages flowing, and thus, everything just working (de/serialization, topic consumption/production, etc...).

NB: the example here is not how you should use this tool. See Rationale section for a more principled approach.

# Contributing:

No docs provided yet. 
The best things you can do to see how it works is checking/playing with tests.
