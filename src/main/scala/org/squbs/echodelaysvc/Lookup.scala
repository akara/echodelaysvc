package org.squbs.echodelaysvc

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._

object Lookup {

  case object GetRef
  case class Request(from: ActorRef)

  def apply(path: String)(implicit refFactory: ActorRefFactory, timeout: Timeout): ActorRef = {
    val refF = (refFactory.actorOf(Props(classOf[Lookup], path, timeout.duration.toNanos + System.nanoTime)) ? GetRef)
      .mapTo[ActorRef]
    // This is ONLY used at initialization. To block just to keep the code base simple.
    Await.result(refF, timeout.duration)
  }
}

class Lookup(wellKnownActorPath: String, endTime: Long) extends Actor with ActorLogging {

  import Lookup._

  import context.dispatcher
  private val selection = context.actorSelection(wellKnownActorPath)

  override def receive: Receive = {
    case GetRef => selection ! Identify(Request(sender()))
    case ActorIdentity(request: Request, None) =>
      if (System.nanoTime < endTime) {
        context.system.scheduler.scheduleOnce(50 millis, self, request)
      } else {
        val err = ActorNotFound(selection)
        request.from ! Status.Failure(err)
        log.error(err, "Actor at {} not started after timeout!", wellKnownActorPath)
        context.stop(self)
      }
    case request: Request => selection ! Identify(request)
    case ActorIdentity(Request(from), Some(actorRef)) =>
      from ! actorRef
      context.stop(self)
  }
}