package com.bigdata.kafka

import io.confluent.kafka.serializers.{KafkaAvroDeserializer, KafkaAvroSerializer}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

import java.util.Properties

object EmployeeProducer {
  def main(args: Array[String]): Unit = {
    val bootstrapServers = "localhost:29092"
    val topic = "employee"
    val schemaRegistryURL = "http://localhost:8081"
    val schemaFile = "src/main/resources/employee.avsc"
    val dataFile = "src/main/resources/employee.csv"

    val properties = new Properties()
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer])
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[KafkaAvroSerializer])
    properties.put("schema.registry.url", schemaRegistryURL)

    val employee = new Employee(properties, schemaFile, dataFile, topic)
    employee.generateSchemaRegistrySubject
    // employee.produceAvroData

    val properties2 = new Properties()
    properties2.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    properties2.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer])
    properties2.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[KafkaAvroDeserializer])
    properties2.put(ConsumerConfig.GROUP_ID_CONFIG, "employee-consumer")
    properties2.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    properties2.put("schema.registry.url", schemaRegistryURL)

    // employee.consumeAvroData(properties2, "employee")
  }
}
