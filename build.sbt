name := "reactive-sqs"

scalaVersion := "2.12.7"

version := "1.0"

libraryDependencies ++= Seq(
  // https://mvnrepository.com/artifact/com.lightbend.akka/akka-stream-alpakka-sqs
  "com.lightbend.akka" %% "akka-stream-alpakka-sqs" % "0.20",
  // https://mvnrepository.com/artifact/com.typesafe.scala-logging/scala-logging
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "org.slf4j" % "slf4j-simple" % "1.7.25",
  // https://mvnrepository.com/artifact/com.typesafe.akka/akka-testkit
  "com.typesafe.akka" %% "akka-testkit" % "2.5.16" % Test,
  // https://mvnrepository.com/artifact/org.scalatest/scalatest
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  // https://mvnrepository.com/artifact/org.mockito/mockito-core
  "org.mockito" % "mockito-core" % "2.23.0"
)
