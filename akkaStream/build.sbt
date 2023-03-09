ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.2"

lazy val root = (project in file("."))
  .settings(
    name := "akkaStream"
  )

val AkkaVersion = "2.7.0"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % AkkaVersion