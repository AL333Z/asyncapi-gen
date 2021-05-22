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
