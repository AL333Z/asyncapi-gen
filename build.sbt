import sbt.Keys.resolvers

ThisBuild / scalaVersion := "2.13.4"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")

val catsEffects     = "org.typelevel" %% "cats-effect"         % "3.1.0"
val munitCatsEffect = "org.typelevel" %% "munit-cats-effect-3" % "0.13.1" % Test
val circeV          = "0.13.0"
val scalapbV        = "0.11.2"

val core = project
  .in(file("core"))
  .settings(
    name := "asyncapi-gen-core",
    libraryDependencies ++= List(
      catsEffects,
      "io.circe"                %% "circe-core"       % circeV,
      "io.circe"                %% "circe-parser"     % circeV,
      "io.circe"                %% "circe-yaml"       % "0.13.1",
      "org.typelevel"           %% "log4cats-slf4j"   % "2.1.0",
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.14.1",
      munitCatsEffect
    )
  )

val protobuf = project
  .in(file("protobuf"))
  .settings(
    name := "asyncapi-gen-protobuf",
    libraryDependencies ++= List(
      "com.thesamet.scalapb" %% "scalapbc" % scalapbV,
      munitCatsEffect
    )
  )
  .dependsOn(core)

val `protobuf-kafka` = project
  .in(file("protobuf-kafka"))
  .settings(
    name := "asyncapi-gen-protobuf-kafka",
    resolvers += "confluent" at "https://packages.confluent.io/maven/",
    libraryDependencies ++= List(
      "io.confluent"          % "kafka-protobuf-serializer" % "6.1.1",
      "com.thesamet.scalapb" %% "scalapb-runtime"           % scalapbV,
      munitCatsEffect
    )
  )
  .dependsOn(protobuf)

val `asyncapi-gen` = project
  .in(file("."))
  .aggregate(core, protobuf, `protobuf-kafka`)
