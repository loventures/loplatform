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

import org.apache.pekko.actor.{Actor, ActorRef, ActorRefFactory, PoisonPill, Props, Terminated}
import com.learningobjects.cpxp.scala.actor.TaggedActors.*
import loi.cp.presence.PresenceActor.Heartbeat
import loi.cp.presence.SceneActor.SceneId
import loi.cp.presence.SessionActor.UserActorInfo
import loi.cp.presence.SessionsActor.DomainMessage
import scalaz.std.iterable.*
import scalaz.syntax.foldable.*
import scalaz.{@@, Tag}
import scaloi.syntax.DateOps.*
import scaloi.syntax.OptionOps.*

import java.util.Date
import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*

/** A session actor that associates various presence actors with a particular http session.
  * @param user
  *   the associated user actor
  * @param scenes
  *   the scenes actor reference
  */
class SessionActor private (
  userActorInfo: UserActorInfo,
  user: UserActor.Ref,
  scenes: ScenesActor.Ref,
) extends Actor:
  import SessionActor.*
  implicit val ec: ExecutionContext = ExecutionContext.global

  private var presences                                      = mutable.Map.empty[ActorRef, PresenceInfo]
  private var presencesState: PresencesState                 = PresencesState()
  private var prevSessionInfo: Option[UserActor.SessionInfo] = None

  /** When this actor starts up, it registers with and watches the user.
    */
  override def preStart(): Unit =
    logger.info(s"Pre-start for session actor for user ${userActorInfo.userId}")
    context `watch` user
    informUser()

  override def postStop(): Unit =
    logger.info(s"Post-stop for session actor for user ${userActorInfo.userId}")

  /** The actor message handler. */
  override val receive: Receive = {
    case CreatePresence(lastActive, visible, heartbeat, inScenes, followScenes) =>
      onCreatePresence(lastActive, visible, heartbeat, inScenes, followScenes)
    case CheckIdle                                                              => updateStateAndInformUser()
    case ClearPresenceState                                                     =>
      logger.debug(s"Clearing PresenceState")
      presencesState = PresencesState()
    case info: PresenceInfo                                                     => onPresenceInfo(info)
    case domainMessage: DomainMessage                                           => onDeliverMessage(domainMessage)
    case message: DeliverMessage                                                => onDeliverMessage(DomainMessage(None, message))
    case Terminated(actor)                                                      => onTerminated(actor)
  }

  /** Handle a presence creation request.
    *
    * @param lastActive
    *   when the user was last active
    * @param visible
    *   the user's visibility
    * @param inScenes
    *   the scenes in which the user is present
    * @param followScenes
    *   the scenes the user is following
    */
  private def onCreatePresence(
    lastActive: Date,
    visible: Boolean,
    heartbeat: Heartbeat,
    inScenes: Set[SceneId],
    followScenes: Set[SceneId]
  ): Unit =
    if presences.size > MaxPresences then
      logger.warn(s"Poisoning presence due to fecundity of user ${userActorInfo.userId}")
      presences.toSeq.minBy(_._2.lastReported)._1 ! PoisonPill
    logger.info(s"Creating presence actor for ${userActorInfo.userId} (existing count: ${presences.size})")
    val timeOnTaskActor = TimeOnTaskActor.create(userActorInfo.userId, userActorInfo.sessionPk)
    val presence        = PresenceActor.create(
      lastActive,
      visible,
      heartbeat,
      inScenes,
      followScenes,
      userActorInfo.userId,
      userActorInfo.userHandle,
      scenes,
      timeOnTaskActor
    )
    context `watch` presence
    sender() ! PresenceIdentity(presence)
    // the presence actor will immediately report in its presence so i don't need to fabricate state
  end onCreatePresence

  /** Handle presence information.
    *
    * @param info
    *   the presence information
    */
  private def onPresenceInfo(info: PresenceInfo): Unit =
    if presences.put(sender(), info).isEmpty then
      logger.info(s"Storing presence actor for user ${userActorInfo.userId}")

    updateStateAndInformUser()

  /** Handle a deliver message request.
    *
    * @param domainMessage
    *   the message to forward
    */
  private def onDeliverMessage(domainMessage: DomainMessage): Unit =
    presences.keys foreach { presence =>
      val send = domainMessage.domainId.forall(id => id == userActorInfo.domainId)
      if send then
        logger.debug(
          s"SessionActor $self deliver ${domainMessage.message} to presence $presence from session for user $userActorInfo.userId"
        )
        presence forward domainMessage.message
    }

  private def startTimer(): Unit =
    context.system.scheduler.scheduleOnce(IdleTimeout, self, CheckIdle)

  /** Handle when an actor dies.
    *
    * @param actor
    *   the dead actor
    */
  private def onTerminated(actor: ActorRef): Unit =
    if actor == user then self ! PoisonPill
    else if presences.remove(actor).isDefined then
      logger.info(s"Removed presence actor for user ${userActorInfo.userId}")
      updateStateAndInformUser()

  /** Update Presence State and starts timer if active. Should be called whenever there are presence changes.
    */
  private def updateStateAndInformUser(): Unit =
    // Get largest of active values
    val lastActive        = presencesState.lastActive `max` maxLastActive
    val lastVisibleActive = presencesState.lastVisibleActive `max` maxLastVisibleActive

    val timeOfTimeout   = new Date() - IdleTimeout
    val presencesActive = lastActive.exists(_.after(timeOfTimeout))

    presencesState = presencesState.copy(
      active = presencesActive,
      lastActive = lastActive,
      lastVisibleActive = lastVisibleActive,
      becameActive = if presencesState.becameActive.isEmpty then lastActive else presencesState.becameActive
    )

    if presencesActive then startTimer()
    informUser()
  end updateStateAndInformUser

  private def createSessionInfo(
    presencesState: PresencesState,
    presences: mutable.Map[ActorRef, PresenceInfo]
  ): UserActor.SessionInfo =
    UserActor.SessionInfo(
      lastVisibleActive = presencesState.lastVisibleActive,
      lastActive = presencesState.lastActive,
      becameActive = presencesState.becameActive,
      active = presencesState.active,
      presenceCount = presences.values.size
    )

  /** Inform the user of the current session information.
    */
  private def informUser(): Unit =
    val newSessionInfo = createSessionInfo(presencesState, presences)

    // Only send if new SessionInfo is different from last time
    if !prevSessionInfo.contains(newSessionInfo) then
      prevSessionInfo = Some(newSessionInfo)
      user ! newSessionInfo

  private def maxLastVisibleActive: Option[Date] =
    presences.values.filter(_.visible).map(_.lastActive).maximum

  private def maxLastActive: Option[Date] =
    presences.values.map(_.lastActive).maximum
end SessionActor

/** The session actor companion.
  */
object SessionActor:

  /** The logger. */
  private val logger = org.log4s.getLogger

  /** A session actor reference. */
  type Ref = ActorRef @@ SessionActor

  /** Create a new session actor.
    * @param user
    *   the associated user actor
    * @param scenes
    *   the scenes actor reference
    * @param factory
    *   the actor creation factory
    * @return
    *   the actor properties
    */
  def create(userActorInfo: UserActorInfo, user: UserActor.Ref, scenes: ScenesActor.Ref)(implicit
    factory: ActorRefFactory
  ): Ref =
    Tag.of[SessionActor] {
      factory.actorOf(Props(new SessionActor(userActorInfo, user, scenes)))
    }

  /** Creates a new presence actor within this session. Expect a {PresenceIdentity} response.
    *
    * @param lastActive
    *   when the user was last active
    * @param visible
    *   the user's visibility
    * @param inScenes
    *   the scenes in which the user is present
    * @param followScenes
    *   the scenes which the user is following
    */
  case class CreatePresence(
    lastActive: Date,
    visible: Boolean,
    heartbeat: Heartbeat,
    inScenes: Set[SceneId] = Set.empty,
    followScenes: Set[SceneId] = Set.empty,
  )

  /** Presence actor identity response to a creation request.
    *
    * @param actor
    *   the presence actor reference
    */
  case class PresenceIdentity(actor: PresenceActor.Ref)

  /** Presence actor state information. This is just an alias for the type in PresenceActor.
    */
  type PresenceInfo = PresenceActor.PresenceInfo

  /** Deliver a message onto all the presence actors. This is just an alias for the type in PresenceActor.
    */
  type DeliverMessage = PresenceActor.DeliverMessage

  /** Useful information about a user. */
  case class UserActorInfo(sessionPk: Long, userId: Long, userHandle: String, domainId: Long)

  /** The maximum number of presence actors allowed for a session. */
  val MaxPresences = 8

  case class PresencesState(
    active: Boolean = false,
    lastActive: Option[Date] = None,
    lastVisibleActive: Option[Date] = None,
    becameActive: Option[Date] = None,
  )

  case object CheckIdle
  case object ClearPresenceState

  private final val IdleTimeout = 3.minutes
end SessionActor
