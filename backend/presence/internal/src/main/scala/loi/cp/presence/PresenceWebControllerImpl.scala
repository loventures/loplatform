/*
 * LO Platform copyright (C) 2007–2025 LO Ventures LLC.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package loi.cp.presence

import org.apache.pekko.actor.{ActorRef, ActorSystem, PoisonPill}
import org.apache.pekko.pattern.AskTimeoutException
import org.apache.pekko.util.Timeout
import argonaut.Json
import com.fasterxml.jackson.databind.ObjectMapper
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.actor.TaggedActors.*
import com.learningobjects.cpxp.scala.util.HttpSessionOps.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.exception.AccessForbiddenException
import com.learningobjects.cpxp.service.user.{UserDTO, UserType}
import com.learningobjects.cpxp.util.{GuidUtil, ManagedUtils}
import de.tomcat.juli.LogMeta
import jakarta.servlet.http.*
import loi.apm.Apm
import loi.authoring.project.ProjectService
import loi.cp.content.ContentAccessService
import loi.cp.presence.PresenceActor.{DeliverMessage, Heartbeat}
import loi.cp.presence.SceneActor.{InBranch, InContext, InContextWithEdgePath, SceneId}
import loi.cp.presence.SessionActor.UserActorInfo
import loi.cp.presence.SessionsActor.DomainMessage
import loi.cp.sse.{SseEvent, SseResponse}
import loi.cp.web.HandleService
import scalaz.\/
import scalaz.std.list.*
import scalaz.std.option.*
import scalaz.syntax.either.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scaloi.misc.TryInstances.*
import scaloi.syntax.option.*
import scaloi.syntax.string.*
import scaloi.syntax.ʈry.*

import java.util.Date
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/** Implementation of the presence root API.
  *
  * @param componentInstance
  *   the component instance
  */
@Component
class PresenceWebControllerImpl(val componentInstance: ComponentInstance, user: UserDTO, now: Date)(implicit
  actorSystem: ActorSystem,
  mapper: ObjectMapper,
  ec: ExecutionContext,
  hs: HandleService,
  projectService: ProjectService,
  contentAccessService: ContentAccessService,
) extends PresenceWebController
    with ComponentImplementation:

  import PresenceWebController.*
  import PresenceWebControllerImpl.*

  /** Create a new presence.
    *
    * @param createArg
    *   the initial session data
    * @param http
    *   the http context
    * @return
    *   the future presence identifier
    */
  override def createPresence(
    createArg: ArgoBody[PresenceIn],
    http: HttpContext
  ): Future[Unit \/ ArgoBody[PresenceId]] =
    val httpSession   = http.session
    // Aside: It is somewhat ugly and limiting for us to send the handle to the actor
    // framework in this way, it effectively means that we can't rotate handles or
    // customize handles to users. However in order to send handles via pekko otherwise
    // we would have to have bizarre post-processing on the SSE stream that turned
    // PKs into handles during serialization. Arguably this could be done by hijacking
    // Jackson but much magic would be involved.
    val handle        = hs.mask(user)
    val sessionPk     = Current.getSessionPk
    // The presence framework assumes one user actor equals one session actor. Preview
    // users make a lie of this because they run on the one session. So we make fake
    // session ids for preview users. The session death gets magically broadcast to
    // the child session actors by sessions actor.
    val sessionId     = if user.userType == UserType.Preview then s"${httpSession.getId}:${user.id}" else httpSession.getId
    val logMeta       = LogMeta.capture
    logger info s"Opening presence for ${user.getId}"
    val userActorInfo = UserActorInfo(sessionPk, user.getId, handle, Current.getDomain)
    val future        = for
      create       <- Future.fromTry(createArg.decode_!)
      // get the current user actor
      userActor    <- UsersActor.clusterActor.askFor[UsersActor.UserRef](UsersActor.GetUser(user.getId))
      // get the session actor
      session      <- SessionsActor.localActor.askFor[SessionsActor.SessionRef](
                        SessionsActor.GetSession(sessionId, userActorInfo, userActor.actor, ScenesActor.clusterActor)
                      )
      // validate the scenes
      inScenes     <- Future.fromTry(logMeta(ManagedUtils.perform(() => create.inScenes.traverse(validateScenes))))
      followScenes <- Future.fromTry(logMeta(ManagedUtils.perform(() => create.followScenes.traverse(validateScenes))))
      // ask session to open a new presence for the user
      presence     <- session.actor.askFor[SessionActor.PresenceIdentity](
                        SessionActor.CreatePresence(
                          create.millisSinceActive
                            .fold(now)(d => new Date(now.getTime - d)),
                          create.visible,
                          Heartbeat(now, 0),
                          inScenes.getOrElse(Set.empty),
                          followScenes.getOrElse(Set.empty)
                        )
                      )
    yield logMeta {
      val guid = GuidUtil.guid
      logger info s"Opened presence $guid -> $presence"
      // bind the new presence actor into the http session for easy lookup
      httpSession.setAttribute(sessionAttribute(guid), presence.actor)
      // return the new presence identifier
      ArgoBody(PresenceId(guid)).right[Unit]
    }
    future recover { case e: AskTimeoutException =>
      logger.warn(e)("Create presence timeout")
      ().left
    }
  end createPresence

  private def validateScenes(scenes: List[SceneId]): Try[Set[SceneId]] =
    scenes.traverse(scene => validateScene(scene).tapFailure(e => logger.warn(e)(s"Invalid scene $scene"))).map(_.toSet)

  /** Validate whether you have the right to be in this scene. */
  private def validateScene(scene: SceneId): Try[SceneId] = scene match
    case ic @ InContext(context)                             =>
      contentAccessService.getCourseAsLearner(context, user).as(ic)
    case icwep @ InContextWithEdgePath(context, edgePath, _) =>
      contentAccessService.readContent(context, edgePath, user).as(icwep)
    case ib @ InBranch(branch, _)                            =>
      projectService.loadBronch(branch).as(ib).toTry(BranchAccessError(branch))

  /** Get information about presence.
    *
    * @param presenceId
    *   the presence identifier
    * @param http
    *   the http context
    * @return
    *   the future session information
    */
  override def getPresence(presenceId: String, http: HttpContext): ErrorResponse \/ Future[ArgoBody[PresenceOut]] =
    for presence <- presenceActor(http.session, presenceId) \/> ErrorResponse.notFound
    yield for info: PresenceActor.PresenceInfo <- presence.askFor[PresenceActor.PresenceInfo](PresenceActor.InfoRequest)
    yield
      val millisSinceActive = now.getTime - info.lastActive.getTime
      ArgoBody(PresenceOut(info.visible, millisSinceActive, info.inScenes))

  /** Update presence.
    *
    * @param presenceId
    *   the presence identifier
    * @param update
    *   the session update data
    * @param http
    *   the http context
    */
  override def updatePresence(
    presenceId: String,
    update0: ArgoBody[PresenceIn],
    http: HttpContext
  ): ErrorResponse \/ Unit =
    for
      update   <- update0.decode_!.toOption \/> ErrorResponse.badRequest
      presence <- presenceActor(http.session, presenceId) \/> ErrorResponse.notFound
    yield

      // when a user is active, course-lw beats fast (every 45 seconds). The millis should
      // never be above 45ish seconds. I let them breathe at 70 sec.
      val abnormal     = update.activeMillis.exists(millis => millis < 0 || millis > 70 * 1000)
      val activeMillis = if abnormal then
        LogMeta
          .let("heartbeat" -> update0.json.getOrElse(Json.jEmptyObject))(logger.info("ignoring abnormal heartbeat"))
        None
      else update.activeMillis

      presence ! PresenceActor.PresenceUpdate(
        update.millisSinceActive.map { d => new Date(now.getTime - d) },
        Some(update.visible),
        update.inScenes.traverse(validateScenes).get,
        update.followScenes.traverse(validateScenes).get,
        update.lastEventId,
        activeMillis.map(am => Heartbeat(now, am)),
        now,
      )

  /** Delete presence.
    *
    * @param presenceId
    *   the presence identifier
    * @param http
    *   the http context
    */
  override def deletePresence(presenceId: String, http: HttpContext): ErrorResponse \/ Unit =
    for presence <- presenceActor(http.session, presenceId) \/> ErrorResponse.notFound
    yield
      try http.session.removeAttribute(sessionAttribute(presenceId))
      catch
        case _: IllegalStateException => // session already closed
      presence ! PoisonPill

  override def postlyDeletePresence(presenceId: String, http: HttpContext): ErrorResponse \/ Unit =
    deletePresence(presenceId, http)

  /** Stream the SSE events for a particular presence.
    *
    * @param presenceId
    *   the presence identifier
    * @param lastEventId
    *   the last-received SSE event id
    * @param http
    *   the http context
    * @return
    *   a future Web response
    */
  override def presenceEvents(
    presenceId: String,
    lastEventId: Option[String],
    http: HttpContext
  ): ErrorResponse \/ WebResponse =
    for presence <- presenceActor(http.session, presenceId) \/> ErrorResponse.notFound
    yield
      Apm.ignoreTransaction()
      val stream = SseResponse.openAsyncStream(http)
      SseActor.create(presence, lastEventId.flatMap(_.toLong_?), stream)
      NoResponse // The request will remain in async mode, owned by the actor

  /** Poll for events from a particular presence.
    *
    * @param presenceId
    *   the presence identifier
    * @param lastEventId
    *   the last received event id
    * @param http
    *   the http context
    * @return
    *   a future containing a list of events
    */
  override def pollPresence(
    presenceId: String,
    lastEventId: Option[String],
    http: HttpContext
  ): ErrorResponse \/ Future[ArgoBody[List[SseEvent]]] =
    for presence <- presenceActor(http.session, presenceId) \/> ErrorResponse.notFound
    yield for response <- presence.askFor[PresenceActor.PresenceEvents](
                            PresenceActor.EventsRequest(lastEventId flatMap (_.toLong_?))
                          )
    yield ArgoBody(
      response.events
        .map(ev => SseEvent(ev.id.toString, ev.event, mapper.writeValueAsString(ev.body)))
        .toList
    )

  /** Temporary hack: Send a maintenance message to everything.
    */
  override def simulateMaintenance(): Unit =
    ClusterBroadcaster.broadcast(
      DomainMessage(None, DeliverMessage(MaintenanceEvent("Simulating maintenance mode.", None)))
    )
end PresenceWebControllerImpl

object PresenceWebControllerImpl:

  /** The logger. */
  private val logger = org.log4s.getLogger

  /** The pekko response timeout. */
  private implicit val PresencePekkoTimeout: Timeout = Timeout(5.seconds)

  /** Get the presence actor associated with a http request by presence identifier.
    *
    * @param httpSession
    *   the http session
    * @param presenceId
    *   the presence identifier
    * @return
    *   a reference to the actor
    */
  private def presenceActor(httpSession: HttpSession, presenceId: String): Option[PresenceActor.Ref] =
    httpSession.attrTag[PresenceActor, ActorRef](sessionAttribute(presenceId))

  /** Get the attribute under which a presence actor is stored in a http session. */
  private def sessionAttribute(presenceId: String): String =
    s"ug:presenceActor:$presenceId"
end PresenceWebControllerImpl

final case class BranchAccessError(branch: Long) extends AccessForbiddenException(s"No access to branch $branch")
