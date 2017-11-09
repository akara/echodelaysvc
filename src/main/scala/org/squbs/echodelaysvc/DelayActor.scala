package org.squbs.echodelaysvc

import akka.actor.{Actor, ActorRef}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}

import scala.concurrent.duration.{FiniteDuration, _}
import scala.language.postfixOps

case class UpdateDelayRequest(newDelay: ()=>FiniteDuration, ack: Option[Any] = None)
case class ScheduleRequest(startTime: Long, echo: String, responder: Option[ActorRef] = None)
case class Scheduled(respondTime: Long, request: ScheduleRequest)
case object CheckCompensate

/**
 * The delay actor is the proxy to initiate the scheduler and keep calibrating the scheduler.
 */
class DelayActor extends Actor {

  import context.dispatcher

  val `1ms` = 1000000L
  var delay: ()=>FiniteDuration = _
  var compensate = 0L

  def receive = {
    // Message from route
    case req @ ScheduleRequest(startTime, echo, responder) =>
      val thisDelay = delay()
      val targetedRespondTime = thisDelay.toNanos + startTime
      val respondTime = targetedRespondTime - compensate
      val origSender = responder.getOrElse(sender())
      if (respondTime - System.nanoTime < `1ms`) { // less than a millis left
        val response = HttpResponse(entity = HttpEntity(ContentTypes.`application/json`,
          s"""{
             |  "path" : "$echo",
             |  "planned-delay" : ${thisDelay.toMillis},
             |  "real-delay" : ${(System.nanoTime - startTime) / `1ms`}
             |}""".stripMargin))
        origSender ! response
        compensate += System.nanoTime() - targetedRespondTime
      } else
        context.system.scheduler.scheduleOnce((respondTime - System.nanoTime()) nanos, self,
          Scheduled(targetedRespondTime, req.copy(responder = Some(origSender))))

    // Message from scheduler
    case Scheduled(respondTime, ScheduleRequest(startTime, echo, responder)) =>
      val current = System.nanoTime()
      compensate += current - respondTime
      val response = HttpResponse(entity = HttpEntity(ContentTypes.`application/json`,
        s"""{
           |  "path" : "$echo",
           |  "planned-delay" : ${(respondTime - startTime) / `1ms`},
           |  "real-delay" : ${(current - startTime) / `1ms`}
           |}""".stripMargin))
      responder.foreach(_ ! response)

    // Message from route to update the request
    case UpdateDelayRequest(newDelay, ackOption) =>
      delay = newDelay
      compensate = 0L
      ackOption foreach (sender() ! _)

    case CheckCompensate =>
      val response = HttpResponse(entity = HttpEntity(ContentTypes.`application/json`,
        s"""{
           |  "total-compensate" : "${compensate.asInstanceOf[Double] / `1ms`} ms"
           |}
         """.stripMargin
      ))
      sender() ! response
  }
}
