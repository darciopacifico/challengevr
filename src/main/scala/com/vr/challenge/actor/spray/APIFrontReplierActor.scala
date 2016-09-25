package com.vr.challenge.actor.spray

import spray.httpx.SprayJsonSupport
import spray.routing.directives.{RouteDirectives, RespondWithDirectives}
import spray.routing.{HttpService, HttpServiceActor, RequestContext}
import akka.actor.{Actor, ActorLogging, PoisonPill, _}
import spray.http.{HttpEntity, HttpResponse, StatusCodes}

import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps

/**
 * Route every possible reply to request context origin. Treat operation timeout
 * Created by darcio
 */
class APIFrontReplierActor(reqCtx: RequestContext, timeout: FiniteDuration) extends Actor with HttpService with RespondWithDirectives with RouteDirectives with SprayJsonSupport {
  val actorRefFactory: ActorContext = context
  implicit val ec = context.system.dispatcher

  import com.vr.challenge.protocol.PropertyProtocol._

  val timeoutSchedule = context.system.scheduler.scheduleOnce(timeout, self, RequestTimeout)

  override def receive: Receive = {
    case PropertyByGeoReply(propertyLot) =>
      reqCtx.complete(propertyLot)
      finishActor

    case PropertyByIdReply(property) =>
      reqCtx.complete(property)
      finishActor

    case PropertyCreated =>
      reqCtx.complete(StatusCodes.OK)
      finishActor

    case PropertyCreationError(err) =>
      reqCtx.complete(HttpResponse(status = 500, entity = HttpEntity(s"Error trying to create the Property! (devenv=true) ${err.getMessage()}")))
      finishActor

    case RequestTimeout =>
      reqCtx.complete(HttpResponse(status = 408, entity = HttpEntity("Request timeout!")))
      finishActor
  }

  /**
   * Cancel the timeout policy and kill the actor
   */
  private def finishActor = {
    this.timeoutSchedule.cancel()
    self ! PoisonPill
  }
}