package com.vr.challenge.actor.spray

import java.util.concurrent.TimeUnit
import javax.ws.rs.Path

import akka.actor._
import akka.util.Timeout
import com.github.swagger.spray.SwaggerHttpService
import com.vr.challenge.actor.repo.RepoFacadeActor
import com.vr.challenge.protocol.PropertyProtocol._
import io.swagger.annotations._
import spray.httpx.SprayJsonSupport._
import spray.routing._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.reflect.runtime.{universe=>ru}
/**
 * (Not Spray-test-kit testable) Http actor implementation
 * @param lot
 */
class APIFrontActor(lot: PropertyLot, mapProvinces: Map[String, Province]) extends HttpServiceActor with APIFrontActorTrait {
  val repoFacadeActor = context.actorOf(Props(new RepoFacadeActor(lot, mapProvinces)),name = "repoFacadeActor")
  val actorContext = context


  val swaggerRoute = new SwaggerHttpService {
    implicit def actorRefFactory = actorContext
    override val apiTypes = Seq(ru.typeOf[APIFrontActorTrait])
    override val host = "localhost:9090" //the url of your api, not swagger's json endpoint
  }.routes



  def receive = runRoute(routes /*~ swaggerRoute*/)


}

/**
 * Testable trait containing the route for Rest API.
 * Spray http actor to handle requests.
 */
//@Path("/properties")
//@Api(value = "/properties")
trait APIFrontActorTrait extends HttpService {
  val DEFAULT_REQUEST_TIMEOUT: FiniteDuration = FiniteDuration(3, TimeUnit.SECONDS)
  implicit val timeout = Timeout(10 seconds)

  val actorContext: ActorRefFactory
  val repoFacadeActor: ActorRef


  /**
   * REST routing patterns
   */
  //@ApiOperation(httpMethod = "GET", response = classOf[Property], value = "Returns a Property based on ID")
  //@ApiImplicitParams(Array(new ApiImplicitParam(name = "id", required = false, dataType = "integer", paramType = "path", value = "ID of Property that needs to be fetched")))
  //@ApiResponses(Array(new ApiResponse(code = 400, message = "Invalid ID Supplied"), new ApiResponse(code = 404, message = "Property not found")))
  def routes: Route =
    pathPrefix("properties") {
      post {
        entity(as[Property]) { property => requestContext =>
          repoFacadeActor ! PropertyCreate(property, replyTo(requestContext))
        }
      } ~
      path(IntNumber) { id => requestContext =>
        repoFacadeActor ! PropertyById(id, replyTo(requestContext))
      } ~
      parameters('ax.as[Int], 'ay.as[Int], 'bx.as[Int], 'by.as[Int]) { (ax, ay, bx, by) =>
        get { ctx =>
          repoFacadeActor ! PropertyByGeo(ax, ay, bx, by, replyTo(ctx))
        }
      }
    }

  /**
   * Create a dedicated actor to handle the reply to the request context using a default timeout
   * @param requestContext
   * @return
   */
  def replyTo(requestContext: RequestContext): ActorRef =
    replyTo(requestContext, DEFAULT_REQUEST_TIMEOUT)

  /**
   * Create dedicated actor to handle the reply to the request context using a given timeout
   * @param requestContext
   * @param timeout
   * @return
   */
  def replyTo(requestContext: RequestContext, timeout: FiniteDuration): ActorRef =
    actorContext.actorOf(Props(new APIFrontReplierActor(requestContext, timeout)))


}
