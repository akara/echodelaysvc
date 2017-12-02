package org.squbs.echodelaysvc

import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{AsyncFlatSpecLike, BeforeAndAfterAll, Matchers}
import org.squbs.httpclient.{ClientFlow, HttpEndpoint}
import org.squbs.resolver.ResolverRegistry
import org.squbs.testkit.{CustomTestKit, PortGetter}
import org.squbs.unicomplex.JMX

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success

object EchoFlowSpec {

  val config: Config = ConfigFactory.parseString(
    s"""
       |squbs {
       |  ${JMX.prefixConfig} = true
       |}
       |
       |default-listener.bind-port = 0
     """.stripMargin)
}

class EchoFlowSpec extends CustomTestKit(EchoFlowSpec.config) with AsyncFlatSpecLike with Matchers with PortGetter
  with BeforeAndAfterAll {

  implicit val _ = ActorMaterializer()

  override def beforeAll(): Unit =
    ResolverRegistry(system).register[HttpEndpoint]("LocalhostEndpointResolver") {
      case ("echotest", _) => Some(HttpEndpoint(s"http://localhost:$port"))
      case _ => None
    }

  it should "respond to the echo requests" in {
    val clientFlow = ClientFlow[Int]("echotest")
    val responseFuture = Source(1 to 10)
      .map(i => HttpRequest(uri = s"/echodelaysvc/echo/$i") -> i)
      .via(clientFlow)
      .collect {
        case (Success(response), _) if response.status == StatusCodes.OK => response.entity
      }
      .mapAsync(1)(_.toStrict(1 second))
      .map(_.toString)
      .runWith(Sink.seq)

    responseFuture.map { seq =>
      seq should have size 10
    }
  }
}
