package com.bigdata.testcontainers

import java.sql.{Connection, DriverManager, Statement}

import com.dimafeng.testcontainers.{ForAllTestContainer, MySQLContainer}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec

import scala.collection.mutable.ListBuffer

case class Employees(id: Int, name: String, salary: Int, dept: Int)

class MysqlSpec extends AnyFlatSpec with ForAllTestContainer with BeforeAndAfterAll {

  override val container: MySQLContainer = MySQLContainer(databaseName = "db1", username = "test", password = "one")

  var connection: Connection = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    container.start()

    connection = DriverManager.getConnection(
      container.jdbcUrl,
      container.username,
      container.password
    )

    val createTableQuery = "CREATE TABLE db1.employees (eid int, ename varchar(30), salary int, dept int)"
    val insertQuery1 = "INSERT INTO db1.employees VALUES (1, 'AAA', 10000, 1111)"
    val insertQuery2 = "INSERT INTO db1.employees VALUES (2, 'BBB', 20000, 2222)"

    val statement: Statement = connection.createStatement()
    statement.addBatch(createTableQuery)
    statement.addBatch(insertQuery1)
    statement.addBatch(insertQuery2)
    statement.executeBatch()
    println(s"Records are inserted for testing")
  }

  override def afterAll(): Unit = {
    container.stop()
    super.afterAll()
  }

  it should "do connect to database" in {
    assert(connection != null)
  }

  it should "return the expected list of databases" in {
    val getDatabasesListQuery = "show databases"
    val statement1 = connection.prepareStatement(getDatabasesListQuery)
    val dbResult = statement1.executeQuery()

    assert(dbResult != null)
    val list = ListBuffer[String]()
    while (dbResult.next) {
      val dbName = dbResult.getString(1)
      list.append(dbName)
    }

    assert(list.toList.contains("db1"))
  }

  it should "return 2 records from table db1.employees" in {
    val getDatabasesListQuery = "select * from db1.employees"
    val statement1 = connection.prepareStatement(getDatabasesListQuery)
    val queryResult = statement1.executeQuery()

    assert(queryResult != null)
    val employees = ListBuffer[Employees]()
    while (queryResult.next) {
      employees.append(
        Employees(
          id = queryResult.getInt(1),
          name = queryResult.getString(2),
          salary = queryResult.getInt(3),
          dept = queryResult.getInt(4)
        )
      )
    }

    assert(employees.length == 2)
  }
}