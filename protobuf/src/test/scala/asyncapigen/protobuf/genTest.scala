package asyncapigen.protobuf

import asyncapigen.ParseAsyncApi
import cats.effect.IO
import munit.CatsEffectSuite

import java.io.File

class genTest extends CatsEffectSuite {
  test("asyncapi to protobuf to gen sources and schema") {
    val baseFolder         = "./target/src_managed/asyncapi-gen"
    val schemaTargetFolder = s"$baseFolder/schemas"
    val scalaTargetFolder  = s"$baseFolder/scalapb"
    val javaTargetFolder   = s"$baseFolder/javapb"

    for {
      asyncApi <- ParseAsyncApi.parseYamlAsyncApiContent[IO](Samples.basicProtobuf)
      _ <- gen.run(
        asyncApi = asyncApi,
        targetPackageName = "org.demo",
        schemaTargetFolder = schemaTargetFolder,
        scalaTargetFolder = scalaTargetFolder,
        javaTargetFolder = javaTargetFolder
      )
      schemaGenFile = s"$schemaTargetFolder/UserEvents.proto"
      scalaGenFile  = s"$scalaTargetFolder/org/demo/UserEventsProto.scala"
      javaGenFile   = s"$javaTargetFolder/org/demo/UserEvents.java"
      _ <- assertIOBoolean(IO.delay(new File(schemaGenFile).exists()), s"$schemaGenFile does not exist!")
      _ <- assertIOBoolean(IO.delay(new File(scalaGenFile).exists()), s"$scalaGenFile does not exist!")
      _ <- assertIOBoolean(IO.delay(new File(javaGenFile).exists()), s"$javaGenFile does not exist!")
    } yield ()
  }
}
