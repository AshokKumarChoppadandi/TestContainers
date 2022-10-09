name := "UnitTestingWithTestContainers"

version := "0.1"

scalaVersion := "2.11.8"

Test / fork := true

libraryDependencies ++= Seq(
  "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.40.10" % "test",
  "com.dimafeng" %% "testcontainers-scala-mysql" % "0.40.10" % "test",
  "org.scalatest" %% "scalatest" % "3.2.13" % Test,
  "org.scalatest" %% "scalatest-flatspec" % "3.2.13" % Test,
  "org.scalactic" %% "scalactic" % "3.2.13",
  "mysql" % "mysql-connector-java" % "8.0.30",
  "org.apache.kafka" %% "kafka" % "2.4.1",
  "org.apache.kafka" % "kafka-clients" % "2.4.1"
)
