package com.bigdata.testcontainers

import java.net.URL
import java.time.Duration

import com.dimafeng.testcontainers.{ForAllTestContainer, GenericContainer}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.testcontainers.containers.wait.strategy.Wait

import scala.io.Source

class GenericContainerSpec extends AnyFlatSpec with ForAllTestContainer with BeforeAndAfterAll {
  override val container = GenericContainer(
    "nginx:latest",
    exposedPorts = Seq(80),
    waitStrategy = Wait.forListeningPort().withStartupTimeout(Duration.ofMillis(5000))
  )

  "GenericContainer" should "start nginx and expose 80 port" in {
    assert(Source.fromInputStream(
      new URL(s"http://${container.containerIpAddress}:${container.mappedPort(80)}/").openConnection().getInputStream
    ).mkString.contains("If you see this page, the nginx web server is successfully installed"))
  }

}
