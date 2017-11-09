package org.squbs.echodelaysvc

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.{AsyncFunSpec, FunSpec, Matchers}

import scala.concurrent.duration._
import scala.util.Random

class DurationUnmarshallerSpec extends AsyncFunSpec with Matchers {

  describe("The duration deserializer") {

    implicit val system = ActorSystem()
    implicit val mat = ActorMaterializer()
    import system.dispatcher

    it("should deserialize a valid micros duration") {
      durationUnmarshaller("500micros") map { _ should be(500 micros) }
    }

    it("should deserialize a valid ns duration") {
      durationUnmarshaller("750ns") map { _ should be(750 nanos) }
    }

    it("should deserialize a valid ms duration") {
      durationUnmarshaller("50ms") map { _ should be(50 millis) }
    }

    it("should deserialize a valid s duration") {
      durationUnmarshaller("2s") map { _ should be(2 seconds) }
    }

    it("should deserialize a valid min duration") {
      durationUnmarshaller("1min") map { _ should be(1 minute) }
    }

    it("should throw a ClassCastException on Infinity") {
      recoverToSucceededIf[ClassCastException] {
        durationUnmarshaller("Inf")
      }
    }

    it("should throw a NumberFormatException on invalid time duration") {
      recoverToSucceededIf[NumberFormatException] {
        durationUnmarshaller("10")
      }
    }
  }
}

class DelayCurveSpec extends FunSpec with Matchers {

  describe ("The duration random functions") {
    it ("should provide a relatively correct mean value for negative exponential") {
      var sum = 0L
      val random = new Random
      for (i <- 0 until 1000000) {
        sum += random.nextNegativeExponential(1 micro, 50 micro, 10 millis).toNanos
      }
      val avg = sum / 1000000L
      println(s"Measured negative exponential mean is $avg nanos. Target is 50000 nanos.")
      avg should equal (50000L +- 500L)
    }

    it ("should provide a relatively correct mean value for gaussian") {
      var sum = 0L
      val random = new Random
      for (i <- 0 until 1000000) {
        sum += random.nextGaussian(100 millis, 550 millis, 1 second, 150 millis).toMillis
      }
      val avg = sum / 1000000L
      println(s"Measured gaussian mean is $avg millis. Target is 550 millis.")
      avg should equal (550L +- 5L)
    }

    it ("should provide a relatively correct mean value for uniform") {
      var sum = 0L
      val random = new Random
      for (i <- 0 until 1000000) {
        sum += random.nextUniform(2 seconds, 10 seconds).toMillis
      }
      val avg = sum / 1000000L
      println(s"Measured uniform mean is $avg millis. Target is 6000 millis.")
      avg should equal (6000L +- 40L)
    }
  }
}
