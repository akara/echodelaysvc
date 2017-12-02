package org.squbs.echodelaysvc

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.HttpResponse
import akka.pattern._
import akka.stream.ActorMaterializer
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.util.Timeout
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.MapModule
import org.scalatest.{BeforeAndAfterAll, FunSpecLike, Matchers}
import org.squbs.testkit.stress.{LoadActor, LoadStats, StartLoad}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

class DelayActorSpec(system: ActorSystem) extends TestKit(system) with ImplicitSender
  with FunSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("DelayActorSpec"))

  implicit val _system = system
  implicit val mat = ActorMaterializer()
  import _system.dispatcher
  val actorRef = TestActorRef[DelayActor]
  val actor = actorRef.underlyingActor
  val random = new Random
  implicit val timeout = Timeout(1 second)
  val mapper = new ObjectMapper
  mapper.registerModule(new MapModule {})

  override def afterAll() = {
    system.shutdown()
  }

  describe ("The delay actor") {
    it ("should have the new delay function set to the function sent in") {
      val myDelayFunction = {() => random.nextUniform(10 millis, 100 millis)}
      case object Ack
      val ack = actorRef ? UpdateDelayRequest(myDelayFunction, Some(Ack))
      Await.ready(ack, 1 second)
      actor.delay should be (myDelayFunction)
    }

    it ("should send a response in time close to given timing parameters") {

      // Set the delay function
      val myDelayFunction = {() => random.nextGaussian(0 millis, 100 millis, 200 millis, 30 millis)}
      actorRef ! UpdateDelayRequest(myDelayFunction)

      val loadActor = system.actorOf(Props[LoadActor])
      loadActor ! StartLoad(System.nanoTime, 10000, 2 seconds, 10 seconds) {
        actorRef ! ScheduleRequest(System.nanoTime, "test")
      }

      var sumSquare = 0L
      var sumDelta = 0L
      var count = 0L
      var running = true

      while (running) {
        receiveOne(1 second) match {
          case r: HttpResponse =>
            val entityString = Await.result(r.entity.toStrict(1 second).map(_.data.utf8String), 1 second)
            val result = mapper.readValue(entityString, classOf[Map[String, Any]])
            val sampled = result("planned-delay").asInstanceOf[Int]
            val real = result("real-delay").asInstanceOf[Int]
            count += 1
            val delta = real - sampled
            sumDelta += delta
            sumSquare += delta * delta
          case LoadStats(tps) =>
            println(s"Achieved $tps TPS.")
            running = false
          case null =>
            println("Timed out without receiving a response")
            running = false
          case _ =>
        }
      }

      println(s"Sample count: $count")
      val avgDelta = sumDelta / count
      val variance = sumSquare / count
      println(s"Aggregate delta: $sumDelta")
      println(s"Avg delta: $avgDelta")
      println(s"Variance: $variance")

      avgDelta should be < 50L // Really, this should be close to 0.
    }

    it ("should report the delay compensation") {
      actorRef ! CheckCompensate
      val response = expectMsgType[HttpResponse](1 second)
      val entityString = Await.result(response.entity.toStrict(1 second).map(_.data.utf8String), 1 second)
      val result = mapper.readValue(entityString, classOf[Map[String, Any]])
      val compensate = result("total-compensate")
      compensate shouldBe a[String]
      compensate.asInstanceOf[String] should endWith (" ms")
    }
  }
}
