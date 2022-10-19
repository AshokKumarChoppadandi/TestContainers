name := "UnitTestingWithTestContainers"

version := "0.1"

scalaVersion := "2.11.8"

Test / fork := true

resolvers += "Confluent" at "https://packages.confluent.io/maven/"

libraryDependencies ++= Seq(
  "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.40.10" % "test",
  "com.dimafeng" %% "testcontainers-scala-mysql" % "0.40.10" % "test",
  "org.scalatest" %% "scalatest" % "3.2.13" % Test,
  "org.scalatest" %% "scalatest-flatspec" % "3.2.13" % Test,
  "org.scalactic" %% "scalactic" % "3.2.13",
  "mysql" % "mysql-connector-java" % "8.0.30",
  "org.apache.kafka" %% "kafka" % "2.4.1",
  "org.apache.kafka" % "kafka-clients" % "2.4.1",
  "org.apache.avro" % "avro" % "1.11.0",
  "commons-io" % "commons-io" % "2.7",
  "io.confluent" % "kafka-streams-avro-serde" % "5.3.3",
  "org.apache.avro" % "avro" % "1.11.1",
  "io.confluent" % "kafka-schema-registry-client" % "5.3.0"
)
