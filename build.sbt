ThisBuild / scalaVersion := "2.13.4"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")

val catsEffects     = "org.typelevel" %% "cats-effect"         % "3.1.0"
val munitCatsEffect = "org.typelevel" %% "munit-cats-effect-3" % "0.13.1" % Test
val circeV          = "0.13.0"

val core = project
  .in(file("core"))
  .settings(
    name := "asyncapi-gen-core",
    libraryDependencies ++= List(
      catsEffects,
      "io.circe" %% "circe-core"   % circeV,
      "io.circe" %% "circe-parser" % circeV,
      "io.circe" %% "circe-yaml"   % "0.13.1",
      munitCatsEffect
    )
  )

val protobuf = project
  .in(file("protobuf"))
  .settings(
    name := "asyncapi-gen-protobuf",
    libraryDependencies ++= List(
      catsEffects,
      "com.thesamet.scalapb" %% "scalapbc" % "0.11.2",
      munitCatsEffect
    )
  )
  .dependsOn(core)

val `asyncapi-gen` = project
  .in(file("."))
  .aggregate(core, protobuf)
