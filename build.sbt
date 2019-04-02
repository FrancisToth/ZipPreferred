name := "zippreferred"

organization := "com.example"

version := "0.1"

scalaVersion := "2.12.8"

scalacOptions ++= Seq(
      "-Xfatal-warnings",
      "-language:postfixOps",
      "-language:higherKinds",
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-unchecked")

lazy val alpakkaVersion = "1.0-RC1"
lazy val akkaVersion = "2.5.21"

libraryDependencies ++= Seq(
  "com.lightbend.akka" %% "akka-stream-alpakka-file" % alpakkaVersion,
  "com.lightbend.akka" %% "akka-stream-alpakka-csv" % alpakkaVersion,

  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.0.5" % Test
)