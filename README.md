Not much to see here yet.

Ideally this should:
- [x] decode an asyncapi spec
- [ ] be able to generate the schema for the models in:
    -[x] `protobuf`: currently able to output a .proto file content
    -[ ] `json`
    -[ ] `avro` 
- [ ] be able to generate the scala code for the models
    -[ ] `protobuf`: should be doable via `scalapb`
    -[ ] `json`
    -[ ] `avro`

Similar to https://github.com/higherkindness/skeuomorph and https://github.com/higherkindness/sbt-mu-srcgen, but without recursive schemes and targeting [asyncapi](https://www.asyncapi.com/docs/specifications/2.0.0). 

Contributing:

No docs provided yet. 
The best things you can do to see how it works is checking/playing with `ConversionGoldenTest`.
