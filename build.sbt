import Deps._

name := "final-project-imageboard"

version := "0.1"

scalaVersion := "2.13.5"

enablePlugins(DockerPlugin)
enablePlugins(JavaAppPackaging)
enablePlugins(FlywayPlugin)

ThisBuild / scalacOptions ++= Seq(
  "-Xlint:unused",
  "-Xfatal-warnings",
  "-deprecation",
)

libraryDependencies ++= (http4s ++ circe ++ doobie ++ logging ++ tapir ++ configReader ++ flyway ++ (scalaTest ++ scalaMock ++ testContainers)
  .map(_ % Test))
