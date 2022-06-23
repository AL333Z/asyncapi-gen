val Java18   = "adopt@1.8"
val Java11   = "adopt@1.11"
val Scala213 = "2.13.8"

import sbt.Keys.resolvers
enablePlugins(SonatypeCiReleasePlugin)
Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / mimaFailOnProblem := false
ThisBuild / mimaFailOnNoPrevious := false
ThisBuild / scalaVersion := "2.13.8"
ThisBuild / organization := "com.al333z"
ThisBuild / organizationName := "al333z"
ThisBuild / publishFullName := "Alessandro Zoffoli"
ThisBuild / publishGithubUser := "al333z"
ThisBuild / githubWorkflowJavaVersions := Seq(Java18, Java11)
ThisBuild / baseVersion := "0.0.1"

//CI definition
val MicrositesCond = s"matrix.scala == '$Scala213'"

def micrositeWorkflowSteps(cond: Option[String] = None): List[WorkflowStep] = List(
  WorkflowStep.Use(
    UseRef.Public("ruby", "setup-ruby", "v1"),
    params = Map("ruby-version" -> "2.6"),
    cond = cond
  ),
  WorkflowStep.Run(List("gem update --system"), cond = cond),
  WorkflowStep.Run(List("gem install sass"), cond = cond),
  WorkflowStep.Run(List("gem install jekyll -v 4"), cond = cond)
)

ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(List("test"), name = Some("Test"))
) ++ micrositeWorkflowSteps(Some(MicrositesCond))
//:+ WorkflowStep.Sbt(
//  List("site/makeMicrosite"),
//  cond = Some(MicrositesCond)
//)

ThisBuild / githubWorkflowAddedJobs ++= Seq(
  WorkflowJob(
    "scalafmt",
    "Scalafmt",
    githubWorkflowJobSetup.value.toList ::: List(
      WorkflowStep.Sbt(List("scalafmtCheckAll"), name = Some("Scalafmt"))
    ),
    // Awaiting release of https://github.com/scalameta/scalafmt/pull/2324/files
    scalas = crossScalaVersions.value.toList.filter(_.startsWith("2."))
  ),
  /*  WorkflowJob(
    "microsite",
    "Microsite",
    githubWorkflowJobSetup.value.toList ::: (micrositeWorkflowSteps(None) :+ WorkflowStep
      .Sbt(List("site/makeMicrosite"), name = Some("Build the microsite"))),
    scalas = List(Scala212)
  ),*/
  WorkflowJob( //This step is to collect the entire build outcome since mergify is not acting properly with githubactions.
    id = "build-success",
    name = "Build Success",
    needs = List("build", "scalafmt"),
    steps = List(WorkflowStep.Run(List("echo Build Succeded"))),
    oses = List("ubuntu-latest"),
    //These are useless but we don't know how to remove the scalas and javas attributes
    // (if you provide empty list it will create an empty list in the yml which is wrong)
    scalas = List(Scala213),
    javas = List(Java18)
  )
)

ThisBuild / githubWorkflowTargetBranches := List("*")
ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v")))

ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    List("release")
  )
)

//ThisBuild / githubWorkflowAddedJobs += WorkflowJob(
//  id = "site",
//  name = "Deploy site",
//  needs = List("build"),
//  javas = List(Java11),
//  scalas = List(Scala213),
//  cond = """
//           | always() &&
//           | needs.build.result == 'success' &&
//           | (github.ref == 'refs/heads/main')
//  """.stripMargin.trim.linesIterator.mkString.some,
//  steps = githubWorkflowGeneratedDownloadSteps.value.toList :+
//    WorkflowStep.Use(
//      UseRef.Public("peaceiris", "actions-gh-pages", "v3"),
//      name = Some(s"Deploy site"),
//      params = Map(
//        "publish_dir"  -> "./site/target/site",
//        "github_token" -> "${{ secrets.GITHUB_TOKEN }}"
//      )
//    )
//)

val catsEffects     = "org.typelevel" %% "cats-effect"         % "3.3.4"
val munitCatsEffect = "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test
val circeV          = "0.14.1"
val scalapbV        = "0.11.8"

val commonSettings = Seq(
  scalafmtOnCompile := true,
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
)

val releaseSettings = {
  Seq(
    Test / publishArtifact := false,
    homepage := Some(url("https://github.com/al333z/asyncapi-gen")),
    startYear := Some(2021),
    licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/al333z/asyncapi-gen"),
        "git@github.com:al333z/asyncapi-gen.git"
      )
    ),
    developers := List(
      Developer("al333z", "Alessandro Zoffoli", "alessandro.zoffoli@gmail.com", url("https://github.com/al333z"))
    )
  )
}

val core = project
  .in(file("core"))
  .settings(commonSettings, releaseSettings)
  .settings(
    name := "asyncapi-gen-core",
    libraryDependencies ++= List(
      catsEffects,
      "io.circe"                %% "circe-core"       % circeV,
      "io.circe"                %% "circe-parser"     % circeV,
      "io.circe"                %% "circe-yaml"       % "0.14.1",
      "org.typelevel"           %% "log4cats-slf4j"   % "2.1.1",
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.17.1",
      munitCatsEffect
    )
  )

val protobuf = project
  .in(file("protobuf"))
  .settings(commonSettings, releaseSettings)
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
  .settings(commonSettings, releaseSettings)
  .settings(
    name := "asyncapi-gen-protobuf-kafka",
    resolvers += "confluent" at "https://packages.confluent.io/maven/",
    libraryDependencies ++= List(
      "io.confluent"          % "kafka-protobuf-serializer" % "7.0.4",
      "org.apache.kafka"     %% "kafka-streams-scala"       % "7.0.0-ce",
      "com.thesamet.scalapb" %% "scalapb-runtime"           % scalapbV,
      munitCatsEffect
    )
  )
  .dependsOn(protobuf)

val `protobuf-kafka-example` = project
  .in(file("protobuf-kafka-example"))
  .settings(commonSettings, releaseSettings)
  .settings(
    name := "asyncapi-gen-protobuf-kafka-example",
    libraryDependencies ++= List()
  )
  .dependsOn(`protobuf-kafka`)

val `asyncapi-gen` = project
  .in(file("."))
  .enablePlugins(NoPublishPlugin)
  .aggregate(core, protobuf, `protobuf-kafka`, `protobuf-kafka-example`)

addCommandAlias("buildAll", ";clean;scalafmtAll;+test")
