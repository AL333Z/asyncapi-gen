ThisBuild / scalaVersion := "2.13.4"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")

lazy val root = (project in file("."))
  .settings(
    name := "asyncapi-gen",
    libraryDependencies ++= List(
      "org.scalatest" %% "scalatest"           % "3.2.2"  % Test,
      "org.typelevel" %% "munit-cats-effect-3" % "0.13.1" % Test,
      "org.typelevel" %% "cats-effect"         % "3.0.0-RC2",
      "io.circe"      %% "circe-core"          % "0.13.0",
      "io.circe"      %% "circe-parser"        % "0.13.0",
      "io.circe"      %% "circe-yaml"          % "0.13.1"
    )
  )
