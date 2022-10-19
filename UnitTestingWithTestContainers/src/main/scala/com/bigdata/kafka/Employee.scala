package com.bigdata.kafka

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import java.io.File
import java.time.Duration
import java.util.Properties
import scala.io.Source
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

class Employee(properties: Properties, schemaFile: String, dataFile: String, topic: String) {
  val producer: KafkaProducer[String, GenericRecord] = new KafkaProducer[String, GenericRecord](properties)

  def generateSchemaRegistrySubject: Int = {
    val client = new CachedSchemaRegistryClient(properties.getProperty("schema.registry.url"), 100)
    val subjectName = s"${topic}-value"

    client.register(subjectName, generateSchema)
  }

  def generateSchema: Schema = {
    val parser: Schema.Parser = new Schema.Parser()
    parser.parse(new File(schemaFile))
  }

  def produceAvroData: Unit = {
    val schema = generateSchema
    val dataSource = Source.fromFile(dataFile)
    val data = dataSource.getLines()

    data.foreach(line => {
      val arr = line.split(",")
      if(arr.length == 5) {
        val genericRecord = new GenericData.Record(schema)
        genericRecord.put("eid", arr(0).toInt)
        genericRecord.put("ename", arr(1))
        genericRecord.put("esalary", arr(2).toInt)
        genericRecord.put("edept", arr(3))
        genericRecord.put("eage", arr(4).toInt)

        val record = new ProducerRecord[String, GenericRecord](topic, null, genericRecord)
        try {
          producer.send(record)
        }
        catch {
          case exception: Exception => exception.printStackTrace()
        }
      }
    })

    producer.flush()
    producer.close()
  }

  def consumeAvroData(properties2: Properties, topic: String): List[JsonEmployee] = {
    val consumer: KafkaConsumer[String, GenericRecord] = new KafkaConsumer[String, GenericRecord](properties2)
    consumer.subscribe(List(topic).asJava)

    val records = consumer.poll(Duration.ofMillis(1000))
    val listBuffer = new ListBuffer[JsonEmployee]()

    for (record <- records.asScala) {
      listBuffer.append(parseAvroRecord(record.value()))
    }
    listBuffer.toList
    /*records.asScala.map(record => {
      // parseAvroRecord(record.value())
      record.topic()
    }).toList*/
  }

  def parseAvroRecord(record: GenericRecord): JsonEmployee = {
    JsonEmployee(
      record.get("eid").toString.toInt,
      record.get("ename").toString,
      record.get("esalary").toString.toInt,
      record.get("edept").toString,
      record.get("eage").toString.toInt
    )
  }
}

case class JsonEmployee(eid: Int, ename: String, esalary: Int, edept: String, eage: Int)