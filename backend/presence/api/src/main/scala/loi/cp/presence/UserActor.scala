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

import java.util.{Date, UUID}

import org.apache.pekko.actor.{
  Actor,
  ActorRef,
  ActorRefFactory,
  Cancellable,
  PoisonPill,
  Props,
  ReceiveTimeout,
  Terminated
}
import loi.cp.AnalyticsActor
import loi.cp.AnalyticsActor.SessionsActivityEvent
import scaloi.syntax.DateOps.*
import scaloi.syntax.OptionOps.*

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*
import scalaz.syntax.tag.*
import scalaz.{@@, Tag}
import scalaz.std.iterable.*
import scalaz.syntax.foldable.*

/** The user actor aggregates activity information for all a user's sessions and allows subscribers to monitor the
  * user's presence. Access control over who many follow whom is the responsibility of the Web API.
  *
  * @param id
  *   the user id
  */
class UserActor(id: Long) extends Actor:
  import UserActor.*
  implicit val ec: ExecutionContext = ExecutionContext.global

  /** Actors who are following this user's presence. */
  private val followers = mutable.Set.empty[ActorRef]

  /** Sessions that the user has open. */
  private val sessions = mutable.Map.empty[ActorRef, UserActor.SessionInfo]

  /** aggregated state of all sessions. */
  private var sessionsState: Option[SessionsState]             = None
  private var sessionStateTimerCancelable: Option[Cancellable] = None

  private def usersActor: ActorRef = context.parent

  /** When this actor starts up, it sets an idle timeout.
    */
  override def preStart(): Unit =
    context setReceiveTimeout IdleTimeout

  override def postStop(): Unit =
    logger.debug(s"User Actor dying with state: $sessionsState")
    transition(None)

  /** The actor message handler.
    */
  override val receive: Receive = {
    case Follow                                => onFollow()
    case Unfollow                              => onUnfollow()
    case info: UserActor.SessionInfo           => onSessionInfo(info)
    case message: PresenceActor.DeliverMessage => onDeliverMessage(message)
    case Terminated(actor)                     => onTerminated(actor)
    case ReceiveTimeout                        => onReceiveTimeout()
    case ClearSessionStateTimer                => transition(None) // Clear Session State
  }

  /** Add a new follower.
    */
  private def onFollow(): Unit =
    if followers add sender() then
      context watch sender()
      sender() ! myPresence

  /** Remove a follower.
    */
  private def onUnfollow(): Unit =
    // Technically (because followers should be PresenceActor and sessions
    // SessionActor) nobody should ever be in both, but nothing in the API
    // guarantees this, so double-check before unwatching.
    if (followers remove sender()) && !(sessions contains sender()) then context unwatch sender()

  /** Handle presence information from a user's session. Emits Session Active Events when: A new SessionInfo with
    * lastActive and becameActive is given for the first time When the User is inactive but a SessionInfo now reports
    * that it is active Emits Session Idle Events when: When the User is active and all sessions become inactive
    * @param info
    *   the session information
    */
  private def onSessionInfo(info: UserActor.SessionInfo): Unit =
    logger.debug(s"Received SessionInfo $info. Current state $sessionsState.")
    val previous = sessions.put(sender(), info)
    if previous.isEmpty then context watch sender()
    if previous.flatMap(_.lastVisibleActive) != info.lastVisibleActive then broadcastPresence()

    transition(
      sessionsState.fold({
        for (
          lastActive   <- info.lastActive;
          becameActive <- info.becameActive
        )
          yield
            // Initialize SessionState. First time a Session State is created with presence info.
            SessionsState(
              active = true,
              lastActive = lastActive,
              becameActive = becameActive,
              id = UUID.randomUUID(),
              presenceCounts = Map(sender() -> info.presenceCount)
            )
      })(ss =>
        Some(
          ss.copy(
            active = sessions.values.exists(_.active),
            lastActive =
              info.lastActive.fold(ss.lastActive)(la => if la.after(ss.lastActive) then la else ss.lastActive),
            presenceCounts = ss.presenceCounts + (sender() -> info.presenceCount)
          )
        )
      )
    )

    /* also send last active info to the singleton for access stats tracking */
    val lastActive = LastActive(sessionsState.map(_.lastActive))
    if previous.flatMap(_.lastActive) != lastActive.value then usersActor ! lastActive
  end onSessionInfo

  private def analyticsActor: ActorRef = AnalyticsActor.localActor.unwrap

  /** Sets new SessionState and emits events if state has changed. Also starts the SessionStateTimer if we have no
    * presences or cancels one it if a presence has been created. Tells sessions to clear their state if the new state
    * is None.
    * @param newState
    */
  def transition(newState: Option[SessionsState]): Unit =
    newState match
      case None       =>
        sessionsState.foreach(prevState =>
          if prevState.active then analyticsActor ! SessionsActivityEvent(id, prevState.copy(active = false))
        )
      case Some(newS) =>
        sessionsState match
          case None if newS.active                                => analyticsActor ! SessionsActivityEvent(id, newS)
          case Some(prevState) if newS.active != prevState.active => analyticsActor ! SessionsActivityEvent(id, newS)
          case _                                                  => ()

        sessionStateTimerCancelable = sessionStateTimerCancelable match
          case None if newS.presenceCounts.values.sum == 0             =>
            Some(context.system.scheduler.scheduleOnce(ClearSessionStateTimeout, self, ClearSessionStateTimer))
          case Some(cancellable) if newS.presenceCounts.values.sum > 0 =>
            cancellable.cancel()
            None
          case _                                                       => sessionStateTimerCancelable
    end match

    sessionsState = newState
  end transition

  /** Forward a message to all open sessions.
    *
    * @param message
    *   the message
    */
  private def onDeliverMessage(message: PresenceActor.DeliverMessage): Unit =
    sessions.keys foreach { session =>
      logger.debug(s"UserActor $self deliver $message to session $session from user $id")
      session forward message
    }

  /** Handle death of a session or a follower. If there is a session that changes the overall state to inactive than a
    * transition also occurs.
    * @param actor
    *   the dead actor
    */
  private def onTerminated(actor: ActorRef): Unit =
    // Technically (because followers should be PresenceActor and sessions
    // SessionActor) nobody should ever be in both, but nothing in the API
    // guarantees this, so remove from both.
    followers remove actor
    logger.debug(s"Actor dying $actor state before remove: $sessionsState")
    if sessions.remove(actor).isDefined then broadcastPresence()

    for ss <- sessionsState if ss.active && !sessions.values.exists(_.active) do
      logger.debug(s"Removing the previous session actor makes the state inactive now $sessions")
      transition(Some(ss.copy(active = false)))
  end onTerminated

  /** Terminate this user if there are no sessions or followers.
    */
  private def onReceiveTimeout(): Unit =
    if followers.isEmpty && sessions.isEmpty then self ! PoisonPill

  /** Broadcast the presence of this user to all followers.
    */
  private def broadcastPresence(): Unit =
    val s = myPresence
    followers foreach { follower =>
      follower ! s
    }

  /** Construct this user's presence information.
    *
    * @return
    *   this user's presence information
    */
  private def myPresence: UserPresence = UserPresence(id, lastActive)

  /** Get the most recent activity time of any visible session.
    *
    * @return
    *   the last active time
    */
  private def lastActive: Option[Date] =
    sessionView.map(_.lastVisibleActive).suml(using maxMonoid)

  /** Get a view over the sessions. */
  private def sessionView: Iterable[UserActor.SessionInfo] = sessions.values.view
end UserActor

/** The user actor companion.
  */
object UserActor:

  /** The logger. */
  private val logger = org.log4s.getLogger

  /** A user actor reference. */
  type Ref = ActorRef @@ UserActor

  case class SessionsState(
    active: Boolean,
    lastActive: Date,
    becameActive: Date,
    id: UUID,
    presenceCounts: Map[ActorRef, Int]
  )

  /** Create a user actor.
    *
    * @param id
    *   the user id
    * @param factory
    *   the actor factory
    * @return
    *   the new user actor
    */
  def create(id: Long)(implicit factory: ActorRefFactory): Ref =
    Tag.of[UserActor] {
      factory.actorOf(Props(new UserActor(id)))
    }

  /** A follow request. The sender() will receive {UserPresence} messages.
    */
  case object Follow

  /** An unfollow request. The sender() will no longer receive presence messages.
    */
  case object Unfollow

  case object ClearSessionStateTimer

  /** User presence information broadcast to followers.
    *
    * @param id
    *   the user id
    * @param lastVisibleActive
    *   when the user was last active
    */
  case class UserPresence(id: Long, lastVisibleActive: Option[Date])

  case class LastActive(value: Option[Date])

  /** Session information. Aggregates information about associated presence actors.
    *
    * @param lastVisibleActive
    *   the last active time of any visible session
    * @param lastActive
    *   the last active time of any session whatsoever
    * @param becameActive
    *   the first time the session became active
    * @param active
    *   if the session has been active for the last 3 minutes
    */
  case class SessionInfo(
    lastVisibleActive: Option[Date],
    lastActive: Option[Date],
    becameActive: Option[Date],
    active: Boolean,
    presenceCount: Int = 0
  )

  private final val ClearSessionStateTimeout = 15.minute

  /** The idle timeout after which an unwanted user actor will poison itself. Should be larger than
    * ClearSessionStateTimeout.
    */
  private final val IdleTimeout = ClearSessionStateTimeout.plus(2.minute)
end UserActor
