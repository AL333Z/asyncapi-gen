package asyncapigen.protobuf

import asyncapigen.Printer.syntax._
import asyncapigen.protobuf.print._
import asyncapigen.protobuf.schema.FieldDescriptorProto.{
  EnumFieldDescriptorProto,
  OneofDescriptorProto,
  PlainFieldDescriptorProto
}
import asyncapigen.protobuf.schema.{MessageDescriptorProto, _}
import cats.data.NonEmptyList
import munit.FunSuite
import cats.implicits._

class printTest extends FunSuite {
  test("print a basic file descriptor message") {
    val expected =
      s"""
         |syntax = "proto3";
         |
         |message SearchRequest {
         |  string query = 1;
         |  int32 page_number = 2;
         |  int32 result_per_page = 3;
         |}
         |""".stripMargin

    val input = FileDescriptorProto(
      name = "",
      `package` = None,
      messageTypes = List(
        MessageDescriptorProto(
          name = "SearchRequest",
          fields = List(
            PlainFieldDescriptorProto(
              name = "query",
              `type` = FieldProtoType.StringProto,
              label = FieldDescriptorProtoLabel.Required,
              options = Nil,
              index = 1
            ),
            PlainFieldDescriptorProto(
              name = "page_number",
              `type` = FieldProtoType.Int32Proto,
              label = FieldDescriptorProtoLabel.Required,
              options = Nil,
              index = 2
            ),
            PlainFieldDescriptorProto(
              name = "result_per_page",
              `type` = FieldProtoType.Int32Proto,
              label = FieldDescriptorProtoLabel.Required,
              options = Nil,
              index = 3
            )
          ),
          nestedMessages = Nil,
          nestedEnums = Nil,
          options = Nil
        )
      ),
      enumTypes = List(),
      syntax = "proto3"
    )

    assertNoDiff(input.print.normalized, expected.normalized)
  }

  test("print a file descriptor message with an enum") {
    val expected =
      s"""
         |syntax = "proto3";
         |
         |message SearchRequest {
         |  string query = 1;
         |  int32 page_number = 2;
         |  int32 result_per_page = 3;
         |  Corpus corpus = 4;
         |  enum Corpus {
         |    UNIVERSAL = 0;
         |    WEB = 1;
         |    IMAGES = 2;
         |  }
         |}
         |""".stripMargin

    val enumDescriptorProto = EnumDescriptorProto(
      name = "Corpus",
      symbols = NonEmptyList.of(
        ("UNIVERSAL", 0),
        ("WEB", 1),
        ("IMAGES", 2)
      ),
      options = Nil
    )
    val input = FileDescriptorProto(
      name = "",
      `package` = None,
      messageTypes = List(
        MessageDescriptorProto(
          name = "SearchRequest",
          fields = List(
            PlainFieldDescriptorProto(
              name = "query",
              `type` = FieldProtoType.StringProto,
              label = FieldDescriptorProtoLabel.Required,
              options = Nil,
              index = 1
            ),
            PlainFieldDescriptorProto(
              name = "page_number",
              `type` = FieldProtoType.Int32Proto,
              label = FieldDescriptorProtoLabel.Required,
              options = Nil,
              index = 2
            ),
            PlainFieldDescriptorProto(
              name = "result_per_page",
              `type` = FieldProtoType.Int32Proto,
              label = FieldDescriptorProtoLabel.Required,
              options = Nil,
              index = 3
            ),
            EnumFieldDescriptorProto(
              name = "corpus",
              enum = enumDescriptorProto,
              label = FieldDescriptorProtoLabel.Required,
              index = 4
            )
          ),
          nestedMessages = Nil,
          nestedEnums = List(enumDescriptorProto),
          options = Nil
        )
      ),
      enumTypes = Nil,
      syntax = "proto3"
    )

    assertNoDiff(input.print.normalized, expected.normalized)
  }

  test("print a file descriptor message with enum and oneOf") {
    val expected =
      s"""
         |syntax = "proto3";
         |
         |message SearchRequest {
         |  string query = 1;
         |  int32 page_number = 2;
         |  int32 result_per_page = 3;
         |  Corpus corpus = 4;
         |  
         |  oneof source {
         |    string bar = 5;
         |    Corpus foo = 6;
         |  }
         |  
         |  enum Corpus {
         |    UNIVERSAL = 0;
         |    WEB = 1;
         |    IMAGES = 2;
         |  }
         |}
         |""".stripMargin

    val enumDescriptorProto = EnumDescriptorProto(
      name = "Corpus",
      symbols = NonEmptyList.of(
        ("UNIVERSAL", 0),
        ("WEB", 1),
        ("IMAGES", 2)
      ),
      options = Nil
    )
    val input = FileDescriptorProto(
      name = "",
      `package` = None,
      messageTypes = List(
        MessageDescriptorProto(
          name = "SearchRequest",
          fields = List(
            PlainFieldDescriptorProto(
              name = "query",
              `type` = FieldProtoType.StringProto,
              label = FieldDescriptorProtoLabel.Required,
              options = Nil,
              index = 1
            ),
            PlainFieldDescriptorProto(
              name = "page_number",
              `type` = FieldProtoType.Int32Proto,
              label = FieldDescriptorProtoLabel.Required,
              options = Nil,
              index = 2
            ),
            PlainFieldDescriptorProto(
              name = "result_per_page",
              `type` = FieldProtoType.Int32Proto,
              label = FieldDescriptorProtoLabel.Required,
              options = Nil,
              index = 3
            ),
            EnumFieldDescriptorProto(
              name = "corpus",
              enum = enumDescriptorProto,
              label = FieldDescriptorProtoLabel.Required,
              index = 4
            ),
            OneofDescriptorProto(
              "source",
              FieldDescriptorProtoLabel.Required,
              List(
                PlainFieldDescriptorProto(
                  name = "bar",
                  `type` = FieldProtoType.StringProto,
                  label = FieldDescriptorProtoLabel.Required,
                  options = Nil,
                  index = 5
                ).asLeft,
                EnumFieldDescriptorProto(
                  name = "foo",
                  enum = enumDescriptorProto,
                  label = FieldDescriptorProtoLabel.Required,
                  index = 6
                ).asRight
              )
            )
          ),
          nestedMessages = Nil,
          nestedEnums = List(enumDescriptorProto),
          options = Nil
        )
      ),
      enumTypes = Nil,
      syntax = "proto3"
    )

    assertNoDiff(input.print.normalized, expected.normalized)
  }

  test("print a message with nested messages") {
    val expected =
      s"""
         |syntax = "proto3";
         |
         |message Outer {
         |  message MiddleAA {
         |    message Inner {
         |      int64 ival = 1;
         |      bool booly = 2;
         |    }
         |  }
         |  message MiddleBB {
         |    message Inner {
         |      int32 ival = 1;
         |      bool booly = 2;
         |    }
         |  }
         |}""".stripMargin

    val input = FileDescriptorProto(
      name = "",
      `package` = None,
      messageTypes = List(
        MessageDescriptorProto(
          name = "Outer",
          fields = Nil,
          nestedMessages = List(
            MessageDescriptorProto(
              name = "MiddleAA",
              fields = Nil,
              nestedMessages = List(
                MessageDescriptorProto(
                  name = "Inner",
                  fields = List(
                    PlainFieldDescriptorProto(
                      name = "ival",
                      `type` = FieldProtoType.Int64Proto,
                      label = FieldDescriptorProtoLabel.Required,
                      options = Nil,
                      index = 1
                    ),
                    PlainFieldDescriptorProto(
                      name = "booly",
                      `type` = FieldProtoType.BoolProto,
                      label = FieldDescriptorProtoLabel.Required,
                      options = Nil,
                      index = 2
                    )
                  ),
                  nestedMessages = Nil,
                  nestedEnums = Nil,
                  options = Nil
                )
              ),
              nestedEnums = Nil,
              options = Nil
            ),
            MessageDescriptorProto(
              name = "MiddleBB",
              fields = Nil,
              nestedMessages = List(
                MessageDescriptorProto(
                  name = "Inner",
                  fields = List(
                    PlainFieldDescriptorProto(
                      name = "ival",
                      `type` = FieldProtoType.Int32Proto,
                      label = FieldDescriptorProtoLabel.Required,
                      options = Nil,
                      index = 1
                    ),
                    PlainFieldDescriptorProto(
                      name = "booly",
                      `type` = FieldProtoType.BoolProto,
                      label = FieldDescriptorProtoLabel.Required,
                      options = Nil,
                      index = 2
                    )
                  ),
                  nestedMessages = Nil,
                  nestedEnums = Nil,
                  options = Nil
                )
              ),
              nestedEnums = Nil,
              options = Nil
            )
          ),
          nestedEnums = Nil,
          options = Nil
        )
      ),
      enumTypes = Nil,
      syntax = "proto3"
    )

    assertNoDiff(input.print.normalized, expected.normalized)
  }

  test("print a sample protobuf event hierarchy") {
    val expected =
      s"""
         |syntax = "proto3";
         |
         |package com.domain.events;
         |
         |message MyKindOfDomainEvent {
         |  string id = 1;
         |  string userId = 2;
         |  oneof eventType {
         |    MySpecificEvent1 event1 = 3;
         |    MySpecificEvent2 event2 = 4;
         |  }
         |  message MySpecificEvent1 {}
         |  message MySpecificEvent2 {}
         |}""".stripMargin

    val input = FileDescriptorProto(
      name = "",
      `package` = Some("com.domain.events"),
      messageTypes = List(
        MessageDescriptorProto(
          name = "MyKindOfDomainEvent",
          fields = List(
            PlainFieldDescriptorProto(
              name = "id",
              `type` = FieldProtoType.StringProto,
              label = FieldDescriptorProtoLabel.Required,
              options = Nil,
              index = 1
            ),
            PlainFieldDescriptorProto(
              name = "userId",
              `type` = FieldProtoType.StringProto,
              label = FieldDescriptorProtoLabel.Required,
              options = Nil,
              index = 2
            ),
            OneofDescriptorProto(
              "eventType",
              FieldDescriptorProtoLabel.Required,
              List(
                PlainFieldDescriptorProto(
                  name = "event1",
                  `type` = FieldProtoType.NamedTypeProto("MySpecificEvent1"),
                  label = FieldDescriptorProtoLabel.Required,
                  options = Nil,
                  index = 3
                ).asLeft,
                PlainFieldDescriptorProto(
                  name = "event2",
                  `type` = FieldProtoType.NamedTypeProto("MySpecificEvent2"),
                  label = FieldDescriptorProtoLabel.Required,
                  options = Nil,
                  index = 4
                ).asLeft
              )
            )
          ),
          nestedMessages = List(
            MessageDescriptorProto(
              name = "MySpecificEvent1",
              fields = Nil,
              nestedMessages = Nil,
              nestedEnums = Nil,
              options = Nil
            ),
            MessageDescriptorProto(
              name = "MySpecificEvent2",
              fields = Nil,
              nestedMessages = Nil,
              nestedEnums = Nil,
              options = Nil
            )
          ),
          nestedEnums = Nil,
          options = Nil
        )
      ),
      enumTypes = Nil,
      syntax = "proto3"
    )

    assertNoDiff(input.print.normalized, expected.normalized)
  }
}
