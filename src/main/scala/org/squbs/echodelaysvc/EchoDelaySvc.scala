package org.squbs.echodelaysvc

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.pattern.ask
import akka.util.Timeout
import org.squbs.unicomplex.RouteDefinition

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

class EchoDelaySvc extends RouteDefinition {

  import org.squbs.util.ConfigUtil._
  implicit val askTimeout: Timeout =
    Timeout(context.system.settings.config.get[FiniteDuration]("akka.http.server.request-timeout"))

  val delayActor = Lookup("/user/echodelaysvc/delayactor")
  delayActor ! UpdateDelayRequest(() => random.nextNegativeExponential(50 millis, 1 second, 20 seconds))

  val random = new Random(System.nanoTime)

  def route =
      path("delay" / "ne") {
        get {
          parameters(
            'min.as[FiniteDuration],
            'mean.as[FiniteDuration],
            'max.as[FiniteDuration],
            'truncate.as[Boolean] ? false) { (min, mean, max, truncate) =>

            delayActor ! UpdateDelayRequest(() => random.nextNegativeExponential(min, mean, max, truncate))
            complete(HttpEntity(ContentTypes.`application/json`,
              s"""{
                 |  "type" : "NegativeExponential",
                 |  "min" : "$min",
                 |  "mean" : "$mean",
                 |  "max" : "$max",
                 |  "truncate" : $truncate
                 |}
             """.stripMargin))
          }
        }
      } ~
      path("delay" / "gaussian") {
        get {
          parameters(
            'min.as[FiniteDuration],
            'mean.as[FiniteDuration],
            'max.as[FiniteDuration],
            'sigma.as[FiniteDuration]) { (min, mean, max, sigma) =>

            delayActor ! UpdateDelayRequest(() => random.nextGaussian(min, mean, max, sigma))
            complete(HttpEntity(ContentTypes.`application/json`,
              s"""{
                 |  "type" : "Gaussian",
                 |  "min" : "$min",
                 |  "mean" : "$mean",
                 |  "max" : "$max",
                 |  "sigma" : "$sigma"
                 |}
             """.stripMargin))
          }
        }
      } ~
      path("delay" / "uniform") {
        get {
          parameters('min.as[FiniteDuration], 'max.as[FiniteDuration]) { (min, max) =>

            delayActor ! UpdateDelayRequest(() => random.nextUniform(min, max))
            complete(HttpEntity(ContentTypes.`application/json`,
              s"""{
                 |  "type" : "Uniform",
                 |  "min" : "$min",
                 |  "max" : "$max"
                 |}
               """.stripMargin))
          }
        }
      } ~
      path("delay" / "compensate") {
        get {
          onSuccess((delayActor ? CheckCompensate).mapTo[HttpResponse]) { complete(_) }
        }
      } ~
      path("hello") {
        get {
          complete(
            """
              |<html>
              |  <body>
              |    <h1>Hello!</h1>
              |  </body>
              |</html>
            """.stripMargin
          )
        }
      }
}