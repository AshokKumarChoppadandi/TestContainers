package com.bigdata.testcontainers

import com.bigdata.kafka.Employee
import com.bigdata.testcontainers.DockerComposeConstants._
import com.dimafeng.testcontainers

import java.io.File
import java.util.Properties
import com.dimafeng.testcontainers.{DockerComposeContainer, ExposedService, ForAllTestContainer}
import io.confluent.kafka.serializers.{KafkaAvroDeserializer, KafkaAvroSerializer}
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.testcontainers.utility.MountableFile

import java.nio.file.Paths
import scala.collection.JavaConversions.asScalaSet

class DockerComposeSpec extends AnyFlatSpec with ForAllTestContainer with BeforeAndAfterAll {
  override val container: DockerComposeContainer = DockerComposeContainer(
    new File(DOCKER_COMPOSE_FILE),
    exposedServices = Seq(
      ExposedService(ZOOKEEPER_CONTAINER_NAME, ZOOKEEPER_PORT, WAIT_STRATEGY),
      ExposedService(KAFKA_BROKER_CONTAINER_NAME, KAFKA_BROKER_PORT, WAIT_STRATEGY),
      ExposedService(SCHEMA_REGISTRY_CONTAINER_NAME, SCHEMA_REGISTRY_PORT, WAIT_STRATEGY),
      ExposedService(KSQL_CONTAINER_NAME, KSQL_PORT, WAIT_STRATEGY)
    ),
    localCompose = false,
    pull = true
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    container.start()
  }

  "DockerComposeContainer" should "retrieve non-0 port for any of services" in {
    val port1 = container.getServicePort(ZOOKEEPER_CONTAINER_NAME, ZOOKEEPER_PORT)
    val port2 = container.getServicePort(KAFKA_BROKER_CONTAINER_NAME, KAFKA_BROKER_PORT)
    val port3 = container.getServicePort(SCHEMA_REGISTRY_CONTAINER_NAME, SCHEMA_REGISTRY_PORT)

    assert(port1 > 0)
    assert(port2 > 0)
    assert(port3 > 0)
  }

  def getProducerCommonProperties: Properties = {
    val properties = new Properties()
    val broker = container.getServiceHost(KAFKA_BROKER_CONTAINER_NAME, KAFKA_BROKER_PORT)
    val port = container.getServicePort(KAFKA_BROKER_CONTAINER_NAME, KAFKA_BROKER_PORT)
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, s"$broker:$port")
    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer])
    properties.put(ProducerConfig.ACKS_CONFIG, "all")
    properties.put(ProducerConfig.LINGER_MS_CONFIG, "10")
    properties.put(ProducerConfig.BATCH_SIZE_CONFIG, "100")
    properties.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
    properties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "10000")
    properties
  }

  def getProducerProperties: Properties = {
    val properties = getProducerCommonProperties
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer])
    properties
  }

  def getAvroProducerProperties: Properties = {
    val schemaRegistry = container.getServiceHost(SCHEMA_REGISTRY_CONTAINER_NAME, SCHEMA_REGISTRY_PORT)
    val schemaRegistryPort = container.getServicePort(SCHEMA_REGISTRY_CONTAINER_NAME, SCHEMA_REGISTRY_PORT)
    val properties = getProducerCommonProperties
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[KafkaAvroSerializer])
    properties.put("schema.registry.url", s"http://${schemaRegistry}:${schemaRegistryPort}")
    properties
  }

  def getCommonConsumerProperties: Properties = {
    val broker = container.getServiceHost(KAFKA_BROKER_CONTAINER_NAME, KAFKA_BROKER_PORT)
    val port = container.getServicePort(KAFKA_BROKER_CONTAINER_NAME, KAFKA_BROKER_PORT)
    val properties = new Properties()
    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, s"$broker:$port")
    properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer])
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    properties
  }

  def getConsumerProperties: Properties = {
    val properties = getCommonConsumerProperties
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer])
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-string")
    properties
  }

  def getAvroConsumerProperties: Properties = {
    val schemaRegistry = container.getServiceHost(SCHEMA_REGISTRY_CONTAINER_NAME, SCHEMA_REGISTRY_PORT)
    val schemaRegistryPort = container.getServicePort(SCHEMA_REGISTRY_CONTAINER_NAME, SCHEMA_REGISTRY_PORT)
    val properties = getCommonConsumerProperties
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[KafkaAvroDeserializer])
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-avro")
    properties.put("schema.registry.url", s"http://${schemaRegistry}:${schemaRegistryPort}")
    properties
  }

  "DockerComposeContainer" should "return default kafka topics" in {
    val consumer = new KafkaConsumer[String, String](getConsumerProperties);
    val topics = consumer.listTopics().keySet();
    assert(topics.size() > 0)
  }

  "DockerComposeContainer" should "help users to create a Kafka Topic" in {
    container.getContainerByServiceName(KAFKA_BROKER_CONTAINER_NAME).get.execInContainer(CREATE_TOPIC_COMMAND: _*)
    val consumer = new KafkaConsumer[String, String](getConsumerProperties);
    val topics = consumer.listTopics().keySet()
    val createdTopic = topics.filter(x => x.equalsIgnoreCase(KAFKA_TOPIC_NAME_TEST))
    assert(createdTopic.toList.nonEmpty && createdTopic.head == KAFKA_TOPIC_NAME_TEST)
  }

  "DockerComposeContainer" should "produce string messages to kafka topic" in {
    val producer = new KafkaProducer[String, String](getProducerProperties)
    val numberOfMessages = (1 to 10).map(x => {
      val record = new ProducerRecord[String, String](KAFKA_TOPIC_NAME_TEST, s"$x", s"This is Message - $x")
      val metadata = producer.send(record)
      metadata.get().hasOffset && metadata.get().hasTimestamp
    }).filter(x => x).toList.size
    producer.flush()
    producer.close()

    assert(numberOfMessages == 10)
  }

  "DockerComposeContainer" should "produce avro messages to kafka topic" in {
    val emp1 = new Employee(getAvroProducerProperties, TEST_SCHEMA_FILE, TEST_DATA_CSV_FILE, KAFKA_TOPIC_NAME_1)
    emp1.produceAvroData

    val expectedMessagesProduced = container
      .getContainerByServiceName(KAFKA_BROKER_CONTAINER_NAME)
      .get
      .execInContainer(KAFKA_AVRO_CONSOLE_CONSUMER: _*)
      .getStdout
      .split("\\n")

    assert(expectedMessagesProduced.nonEmpty && 5 == expectedMessagesProduced.size)
  }

  "DockerComposeContainer" should "produce avro messages from command line" in {
    val emp2 = new Employee(getAvroProducerProperties, TEST_SCHEMA_FILE, TEST_DATA_JSON_FILE, KAFKA_TOPIC_NAME_2)
    val schemaId = emp2.generateSchemaRegistrySubject

    val containerState = container.getContainerByServiceName(KAFKA_BROKER_CONTAINER_NAME).get

    containerState.copyFileToContainer(MountableFile.forHostPath(Paths.get(DATA_FILE_PATH_IN_HOST)), FILE_PATH_INSIDE_CONTAINER)
    containerState.copyFileToContainer(MountableFile.forHostPath(Paths.get(SHELL_PATH_IN_HOST)), SHELL_PATH)
    containerState.execInContainer("ls",  "-ltr", "/root")
    containerState.execInContainer("chmod", "+x", SHELL_PATH)

    val expectedExitCode = containerState
      .execInContainer("sh", s"$SHELL_PATH", s"$KAFKA_TOPIC_NAME_2", s"$schemaId", s"$FILE_PATH_INSIDE_CONTAINER")
      .getExitCode
    assert(0 == expectedExitCode)
  }

  "DockerComposeContainer" should "consume avro messages from kafka topic" in {
    val emp3 = new Employee(getAvroProducerProperties, TEST_SCHEMA_FILE, TEST_DATA_JSON_FILE, KAFKA_TOPIC_NAME_3)
    val schemaId = emp3.generateSchemaRegistrySubject

    val containerState = container.getContainerByServiceName(KAFKA_BROKER_CONTAINER_NAME).get
    containerState.copyFileToContainer(MountableFile.forHostPath(Paths.get(DATA_FILE_PATH_IN_HOST)), FILE_PATH_INSIDE_CONTAINER)
    containerState.copyFileToContainer(MountableFile.forHostPath(Paths.get(SHELL_PATH_IN_HOST)), SHELL_PATH)
    containerState.execInContainer("ls", "-ltr", "/root")
    containerState.execInContainer("chmod", "+x", SHELL_PATH)
    containerState.execInContainer("sh", s"$SHELL_PATH", s"$KAFKA_TOPIC_NAME_3", s"$schemaId", s"$FILE_PATH_INSIDE_CONTAINER")

    val result = emp3.consumeAvroData(getAvroConsumerProperties, KAFKA_TOPIC_NAME_3)
    assert(result.nonEmpty && 5 == result.size)
  }

  "DockerComposeContainer" should "consume ksql datagen avro messages from kafka topic" in {
    val kafkaTopicName = "employees-ksql"
    val emp4 = new Employee(getAvroProducerProperties, KSQL_DATAGEN_SCHEMA_FILE_PATH_IN_HOST, TEST_DATA_JSON_FILE, kafkaTopicName)
    val containerState = container.getContainerByServiceName(KSQL_CONTAINER_NAME).get
    containerState.copyFileToContainer(MountableFile.forHostPath(Paths.get(KSQL_DATAGEN_SCHEMA_FILE_PATH_IN_HOST)), KSQL_DATAGEN_SCHEMA_FILE_INSIDE_CONTAINER)
    containerState.execInContainer(
      generateDataUsingKsqlDataGen(
        schemaFile = KSQL_DATAGEN_SCHEMA_FILE_INSIDE_CONTAINER,
        topicName = kafkaTopicName,
        key = "eid",
        numberOfMessages = 20
      ): _*)

    val result = emp4.consumeAvroData(getAvroConsumerProperties, kafkaTopicName)
    result.foreach(println)
    assert(result.nonEmpty && 20 == result.size)
  }

  override def afterAll(): Unit = {
    container.stop()
    super.afterAll()
  }
}
