package org.squbs.echodelaysvc

import org.scalatest.{FunSpecLike, Matchers}
import org.squbs.testkit.{CustomRouteTestKit, TestRoute}

import scala.concurrent.duration._

class EchoDelaySvcSpec extends CustomRouteTestKit with FunSpecLike with Matchers {

  implicit val timeout = 10.seconds

  describe ("The EchoDelaySvc route") {

    val route = TestRoute[EchoDelaySvc]

    it ("should provide confirmation for setting delay to NegativeExponential") {
      Get("/delay/ne?min=50ms&mean=200ms&max=2s") ~> route ~> check {
        responseAs[String] should include (""""type" : "NegativeExponential"""")
      }
    }

    it ("should provide confirmation for setting delay to Gaussian") {
      Get("/delay/gaussian?min=50ms&mean=1s&max=2s&sigma=330ms") ~> route ~> check {
        responseAs[String] should include (""""type" : "Gaussian"""")
      }
    }

    it ("should provide confirmation for setting delay to Uniform") {
      Get("/delay/uniform?min=30ms&max=1s") ~> route ~> check {
        responseAs[String] should include (""""type" : "Uniform"""")
      }
    }

    it ("should provide the compensate time on request") {
      Get("/delay/compensate") ~> route ~> check {
        responseAs[String] should include regex """"total-compensate" : "(-)?(\d+)(\.\d*)? ms""""
      }
    }
  }
}
