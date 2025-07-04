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

import org.apache.pekko.actor.{Actor, ActorRef, Cancellable, PoisonPill, Props, Terminated}
import org.apache.pekko.cluster.pubsub.DistributedPubSubMediator
import com.learningobjects.cpxp.event.SessionStreamEvent
import com.learningobjects.cpxp.scala.actor.CpxpActorSystem
import com.learningobjects.cpxp.scala.actor.TaggedActors.*
import com.learningobjects.cpxp.util.Box
import loi.cp.presence.PresenceActor.DeliverMessage
import loi.cp.presence.SessionActor.UserActorInfo
import scalaz.syntax.tag.*
import scalaz.{@@, Tag}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*

/** A sessions actor that manages all the session actors on a node.
  */
class SessionsActor private extends Actor:
  import SessionsActor.*

  /** The sessions actors. */
  private val sessions =
    mutable.TreeMap.empty[String, SessionActor.Ref]

  /** A scheduled log event. */
  private val logStuff = Box.empty[Cancellable]

  /** When this actor starts up, it registers with the event stream for session invalidation events.
    */
  override def preStart(): Unit =
    context.system.eventStream.subscribe(self, classOf[SessionStreamEvent])
    ClusterBroadcaster.subscribe() // Follow cluster-wide broadcasts
    logStuff.value = context.system.scheduler.scheduleWithFixedDelay(5.minutes, 5.minutes, self, LogStuff)

  override def postStop(): Unit =
    logStuff foreach { _.cancel() }

  /** The actor message handler. */
  override val receive: Receive = {
    case GetSession(sessionId, userActorInfo, user, scenes) =>
      onGetSession(sessionId, userActorInfo, user, scenes)
    case SessionStreamEvent(sessionId, message)             =>
      onSessionStreamEvent(sessionId, message)
    case Terminated(actor)                                  => onTerminated(actor)
    case DistributedPubSubMediator.SubscribeAck(_)          => onDPSubscribeAck()
    case message: DomainMessage                             => onDeliverMessage(message)
    case LogStuff                                           => logger.info(s"SessionsActor holding ${sessions.size} session references")
  }

  /** Handle a session get request.
    *
    * @param sessionId
    *   the http session identifier
    * @param user
    *   the associated user actor
    * @param scenes
    *   the scenes actor reference
    */
  private def onGetSession(
    sessionId: String,
    userActorInfo: UserActorInfo,
    user: UserActor.Ref,
    scenes: ScenesActor.Ref,
  ): Unit =
    val session = sessions.getOrElseUpdate(
      sessionId, {
        logger.info(s"Creating session actor for ${userActorInfo.userId} in session $sessionId")
        context `watch` SessionActor.create(userActorInfo, user, scenes)
      }
    )
    sender() ! SessionRef(sessionId, session)
  end onGetSession

  /** Handle when a session event appears on the event stream. Also passed to all "subsessions" aka fake presence actors
    * for preview users.
    *
    * @param sessionId
    *   the session id
    * @param message
    *   the message
    */
  private def onSessionStreamEvent(sessionId: String, message: SessionStreamEvent.Message): Unit =
    // this weird prefix check iteration is so we can cast death upon fake preview session actors
    // ${sessionId}:${userPk}...
    sessions.iteratorFrom(sessionId).takeWhile(_._1.startsWith(sessionId)) foreach { case (sessionId, session) =>
      session ! sessionMessage(sessionId, message)
    }

  /** Ack from cluster-wide subscription. Ignore.
    */
  private def onDPSubscribeAck(): Unit = ()

  /** Handle when a session event appears on the event stream.
    *
    * @param message
    *   the message
    */
  private def onDeliverMessage(message: DomainMessage): Unit =
    sessions.values foreach { _ ! message }

  /** Handle when an actor dies.
    *
    * @param actor
    *   the dead actor
    */
  private def onTerminated(actor: ActorRef): Unit =
    sessions filterInPlace { case (_, session) => session.unwrap != actor }
    ()
end SessionsActor

/** The session actor companion.
  */
object SessionsActor:

  /** The logger. */
  private val logger = org.log4s.getLogger

  /** A sessions actor reference. */
  type Ref = ActorRef @@ SessionsActor

  /** Properties to create a new sessions actor. */
  val props: Props = Props(new SessionsActor)

  /** The local system sessions actor. */
  lazy val localActor: Ref =
    Tag.of[SessionsActor](CpxpActorSystem.system.actorOf(props, "sessions"))

  /** Return a message to deliver to a session actor if something happens to the associated http session.
    *
    * @param message
    *   the session message
    * @return
    *   the corresponding message
    */
  private def sessionMessage(sessionId: String, message: SessionStreamEvent.Message): Any =
    message match
      case SessionStreamEvent.Message.Login =>
        PresenceActor.DeliverMessage(LoginEvent) // if a login occurs inform the client
      case SessionStreamEvent.Message.Logout =>
        PresenceActor.DeliverMessage(LogoutEvent) // if a logout occurs inform the client
      case SessionStreamEvent.Message.Destroyed =>
        logger.info(s"Heard about session death for $sessionId")
        PoisonPill // on session death, poison

  /** Gets or creates a session actor. Expect a {SessionRef} response.
    *
    * @param sessionId
    *   the http session identifier
    * @param user
    *   the associated user actor
    * @param scenes
    *   the scenes actor reference
    */
  case class GetSession(
    sessionId: String,
    userActorInfo: UserActorInfo,
    user: UserActor.Ref,
    scenes: ScenesActor.Ref,
  )

  /** Session actor identity response to a creation request.
    *
    * @param sessionId
    *   the session id
    * @param actor
    *   the session actor reference
    */
  case class SessionRef(sessionId: String, actor: SessionActor.Ref)

  case class DomainMessage(domainId: Option[Long], message: DeliverMessage)

  /** Log some stuff.
    */
  private case object LogStuff
end SessionsActor
