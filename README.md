An attempt to transform an (asyncapi)[https://www.asyncapi.com/docs/specifications/2.0.0] spec to a target schema.

Not much to see here yet.

Ideally this should:
- [x] decode an asyncapi spec
- [ ] be able to generate the schema for the models in:
    - [x] `protobuf`: currently able to output a .proto file content
    - [ ] `json`
    - [ ] `avro` 
- [ ] be able to generate the scala code for the models (models, serdes)
    - [ ] `protobuf`: should be doable via `scalapb`
    - [ ] `json`
    - [ ] `avro`

# Contributing:

No docs provided yet. 
The best things you can do to see how it works is checking/playing with `ConversionGoldenTest`.
