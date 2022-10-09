package com.bigdata.testcontainers

import java.io.File
import java.time.Duration
import java.util.Properties

import com.dimafeng.testcontainers.{DockerComposeContainer, ExposedService, ForAllTestContainer}
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.testcontainers.containers.wait.strategy.{Wait, WaitStrategy}

class DockerComposeSpec extends AnyFlatSpec with ForAllTestContainer with BeforeAndAfterAll {
  val DOCKER_COMPOSE_FILE: String = "src/test/resources/docker-compose-test.yaml"
  val ZOOKEEPER_CONTAINER_NAME: String = "zookeeper_1"
  val KAFKA_BROKER_CONTAINER_NAME: String = "broker_1"
  val SCHEMA_REGISTRY_CONTAINER_NAME: String = "schemaregistry_1"

  val ZOOKEEPER_PORT: Int = 2181
  val KAFKA_BROKER_PORT: Int = 29092
  val SCHEMA_REGISTRY_PORT: Int = 8081

  val WAIT_STRATEGY: WaitStrategy = Wait.forHealthcheck().withStartupTimeout(Duration.ofSeconds(30))
  val CREATE_TOPIC_COMMAND: List[String] = "kafka-topics --bootstrap-server broker:9092 --create --topic test --partitions 3 --replication-factor 1"
    .split(" ")
    .toList
  val KAFKA_TOPIC_NAME: String = "test"

  override val container: DockerComposeContainer = DockerComposeContainer(
    new File(DOCKER_COMPOSE_FILE),
    exposedServices = Seq(
      ExposedService(ZOOKEEPER_CONTAINER_NAME, ZOOKEEPER_PORT, WAIT_STRATEGY),
      ExposedService(KAFKA_BROKER_CONTAINER_NAME, KAFKA_BROKER_PORT, WAIT_STRATEGY),
      ExposedService(SCHEMA_REGISTRY_CONTAINER_NAME, SCHEMA_REGISTRY_PORT, WAIT_STRATEGY)
    ),
    localCompose = false,
    pull = true
  )

  override def beforeAll() {
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

  "DockerComposeContainer" should "return list of kafka topics" in {
    val broker = container.getServiceHost(KAFKA_BROKER_CONTAINER_NAME, KAFKA_BROKER_PORT)
    val port = container.getServicePort(KAFKA_BROKER_CONTAINER_NAME, KAFKA_BROKER_PORT)
    val props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, s"$broker:$port");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer]);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer]);

    val consumer = new KafkaConsumer[String, String](props);
    val topics = consumer.listTopics().keySet();

    assert(topics.size() > 0)
  }

  "DockerComposeContainer" should "produce messages to kafka topic" in {
    val broker = container.getServiceHost(KAFKA_BROKER_CONTAINER_NAME, KAFKA_BROKER_PORT)
    val port = container.getServicePort(KAFKA_BROKER_CONTAINER_NAME, KAFKA_BROKER_PORT)
    container.getContainerByServiceName(KAFKA_BROKER_CONTAINER_NAME).get.execInContainer(CREATE_TOPIC_COMMAND: _*)

    val props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, s"$broker:$port")
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer])
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer])
    props.put(ProducerConfig.ACKS_CONFIG, "all")
    props.put(ProducerConfig.LINGER_MS_CONFIG, "10")
    props.put(ProducerConfig.BATCH_SIZE_CONFIG, "100")
    props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
    props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "10000")

    val producer = new KafkaProducer[String, String](props)
    val numberOfMessages = (1 to 10).map(x => {
      val record = new ProducerRecord[String, String](KAFKA_TOPIC_NAME, s"$x", s"This is Message - $x")
      val metadata = producer.send(record)
      metadata.get().hasOffset && metadata.get().hasTimestamp
    }).filter(x => x).toList.size

    producer.flush()
    producer.close()

    assert(numberOfMessages == 10)
  }

  override def afterAll(): Unit = {
    container.stop()
    super.afterAll()
  }
}
