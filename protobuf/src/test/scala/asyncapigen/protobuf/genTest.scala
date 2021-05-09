package asyncapigen.protobuf

import cats.effect.IO
import munit.CatsEffectSuite

import java.io.File

class genTest extends CatsEffectSuite {
  test("asyncapi to protobuf to gen sources and schema") {
    val baseFolder        = "./target/src_managed/asyncapi-gen"
    val schemaFolder      = s"$baseFolder/schemas"
    val scalaTargetFolder = s"$baseFolder/scalapb"
    val javaTargetFolder  = s"$baseFolder/javapb"

    for {
      _ <- gen.run(
        input = Samples.basicProtobuf,
        schemaFolder = schemaFolder,
        scalaTargetFolder = scalaTargetFolder,
        javaTargetFolder = javaTargetFolder
      )
      schemaGenFile = s"$schemaFolder/Signedup.proto"
      scalaGenFile  = s"$scalaTargetFolder/org/demo/SignedupProto.scala"
      javaGenFile   = s"$javaTargetFolder/org/demo/Signedup.java"
      _ <- assertIOBoolean(IO.delay(new File(schemaGenFile).exists()), s"$schemaGenFile does not exist!")
      _ <- assertIOBoolean(IO.delay(new File(scalaGenFile).exists()), s"$scalaGenFile does not exist!")
      _ <- assertIOBoolean(IO.delay(new File(javaGenFile).exists()), s"$javaGenFile does not exist!")
    } yield ()
  }
}
