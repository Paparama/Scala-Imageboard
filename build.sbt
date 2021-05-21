import Deps._

val name = "final-project-anonymous-imageboard"
lazy val `root` = Project(name, file("."))
  .enablePlugins(DockerPlugin, JavaAppPackaging)
  .settings(
    version := "0.1",
    scalaVersion := "2.13.5",
    scalacOptions ++= Seq(
      "-Xlint:unused",
      "-Xfatal-warnings",
      "-deprecation"
    ),
    dockerExposedPorts += 8080,
    dockerBaseImage := "adoptopenjdk/openjdk11",
    dockerRepository := Some("eu.gcr.io/dins-scala-school"),
    version in Docker := git.gitHeadCommit.value.map(_.take(7)).getOrElse("UNKNOWN").toString,
    libraryDependencies ++= (
      http4s ++
      circe ++
      doobie ++
      logging ++
      tapir ++
      configReader ++
      flyway ++
      javax ++
      (scalaTest ++ scalaMock ++ testContainers).map(_ % Test)
    )
  )
