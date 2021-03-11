package asyncapigen.protobuf

import asyncapigen.Printer.syntax._
import asyncapigen.protobuf.print._
import asyncapigen.protobuf.schema.FieldDescriptorProto.{EnumFieldDescriptorProto, PlainFieldDescriptorProto}
import asyncapigen.protobuf.schema._
import cats.data.NonEmptyList
import munit.FunSuite

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
}
