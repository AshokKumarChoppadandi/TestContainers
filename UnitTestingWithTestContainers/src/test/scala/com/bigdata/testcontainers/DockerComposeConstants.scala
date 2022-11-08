package com.bigdata.testcontainers

import org.testcontainers.containers.wait.strategy.{Wait, WaitStrategy}

import java.time.Duration
import scala.io.Source

object DockerComposeConstants {
  val DOCKER_COMPOSE_FILE: String = "src/test/resources/docker-compose-test.yaml"
  val ZOOKEEPER_CONTAINER_NAME: String = "zookeeper_1"
  val KAFKA_BROKER_CONTAINER_NAME: String = "broker_1"
  val SCHEMA_REGISTRY_CONTAINER_NAME: String = "schemaregistry_1"
  val KSQL_CONTAINER_NAME: String = "ksql_1"

  val ZOOKEEPER_PORT: Int = 2181
  val KAFKA_BROKER_PORT: Int = 29092
  val SCHEMA_REGISTRY_PORT: Int = 8081
  val KSQL_PORT: Int = 8088

  val KAFKA_TOPIC_NAME_TEST: String = "test"
  val KAFKA_TOPIC_NAME_1: String = "employees1"
  val KAFKA_TOPIC_NAME_2: String = "employees2"
  val KAFKA_TOPIC_NAME_3: String = "employees3"

  val TEST_SCHEMA_FILE: String = "src/test/resources/employee-test.avsc"
  val TEST_DATA_JSON_FILE: String = "src/test/resources/employee-test.json"
  val TEST_DATA_CSV_FILE: String = "src/test/resources/employee-test.csv"
  val SHELL_PATH_IN_HOST: String = "src/test/resources/generate_data.sh"
  val SHELL_PATH: String = "/root/generate_data.sh"
  val DATA_FILE_PATH_IN_HOST: String = "src/test/resources/employee-test.json"
  val FILE_PATH_INSIDE_CONTAINER: String = "/root/employee-test.json"
  val KSQL_DATAGEN_SCHEMA_FILE_PATH_IN_HOST: String = "src/test/resources/ksql-datagen-employees.json"
  val KSQL_DATAGEN_SCHEMA_FILE_INSIDE_CONTAINER: String = "/root/ksql-datagen-employees.json"

  val WAIT_STRATEGY: WaitStrategy = Wait.forHealthcheck().withStartupTimeout(Duration.ofSeconds(30))
  val CREATE_TOPIC_COMMAND: List[String] = s"kafka-topics --bootstrap-server broker:9092 --create --topic ${KAFKA_TOPIC_NAME_TEST} --partitions 3 --replication-factor 1"
    .split(" ")
    .toList

  val SCHEMA_STRING: String = Source
    .fromFile("src/test/resources/employee-test.avsc")
    .getLines()
    .mkString("")
    .replaceAll("\\s", "")
    .replaceAll("\"", "\\\"")

  val KAFKA_AVRO_CONSOLE_CONSUMER: List[String] = s"kafka-avro-console-consumer --bootstrap-server broker:9092 --topic ${KAFKA_TOPIC_NAME_1} --from-beginning --max-messages 5 --property schema.registry.url=http://schemaregistry:8081"
    .split(" ")
    .toList

  def generateDataUsingKsqlDataGen(schemaFile: String, format: String = "avro", topicName: String, key: String, bootStrapServer: String = "broker:9092", numberOfMessages: Int = 10, interval: Int = 500, propertiesFile: String = "/usr/local/configs/ksql-server.properties", schemaRegistryUrl: String = "http://schemaregistry:8081"): List[String] = {
    s"ksql-datagen schema=$schemaFile format=$format topic=$topicName key=$key bootstrap-server=$bootStrapServer iterations=$numberOfMessages maxInterval=$interval propertiesFile=$propertiesFile schemaRegistryUrl=$schemaRegistryUrl"
      .split(" ")
      .toList
  }
}
