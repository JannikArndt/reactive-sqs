name := "reactive-sqs"

scalaVersion := "2.12.6"

val akkaVersion = "2.5.16"
val akkaHttpVersion = "10.1.5"

version := "1.0"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk-sqs" % "1.11.409",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.lightbend.akka" %% "akka-stream-alpakka-sqs" % "0.20",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "org.slf4j" % "slf4j-simple" % "1.7.25",
)
