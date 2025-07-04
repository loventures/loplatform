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

import org.apache.pekko.actor.{Actor, ActorRef, ActorRefFactory, PoisonPill, Props, ReceiveTimeout, Terminated}
import com.learningobjects.cpxp.scala.actor.ActorRefOps.*
import com.learningobjects.cpxp.scala.actor.TaggedActors.*
import com.learningobjects.cpxp.scala.util.Misc
import com.learningobjects.cpxp.service.presence.EventType
import loi.cp.presence.SceneActor.SceneId
import loi.cp.presence.ScenesActor.PresenceInScenes
import loi.cp.presence.TimeOnTaskActor.SceneActivity
import scalaz.syntax.std.boolean.*
import scalaz.{@@, Tag}
import scaloi.data.SetDelta
import scaloi.syntax.any.*
import scaloi.syntax.date.*
import scaloi.syntax.option.*

import java.util.Date
import scala.concurrent.duration.*

/** The presence actor models the state of an open browser window or tab. It keeps track of the user's most recent
  * activity time and what scene (context: course or activity) the user is engaged in. It also provides a real-time
  * stream of relevant data to that browser window.
  *
  * @param stateData
  *   the current state of this actor
  * @param userId
  *   the associated user ID
  * @param userHandle
  *   the associated user handle
  * @param scenesActor
  *   the scenes actor
  */
class PresenceActor(
  private var stateData: PresenceActor.StateData,
  userId: Long,
  userHandle: String,
  scenesActor: ScenesActor.Ref,
  timeOnTaskActor: TimeOnTaskActor.Ref
) extends Actor:
  import PresenceActor.*

  /** When this actor starts up, it sets an idle timeout and notifies the session of the user's activity.
    */
  override def preStart(): Unit =
    logger.info(s"Pre-start presence actor for user $userId")
    context setReceiveTimeout IdleTimeout
    sendState(stateData, init = true)

  override def postStop(): Unit =
    logger.info(s"Post-stop presence actor for user $userId")

  /** The actor message handler. This is modeled as a simple state machine with a series of message handlers that
    * perform a side effect and optionally return an updated the presence state.
    */
  override val receive: Receive = new Receive:

    /** Handle an incoming message. This delegates to the {handleMessage}.
      *
      * @param a
      *   the message
      */
    override def apply(a: Any): Unit =
      handleMessage(a)

    /** Return whether a message is supported.
      * @param a
      *   the message
      * @return
      *   whether a state transition function is defined for this message
      */
    override def isDefinedAt(a: Any): Boolean =
      messageHandlers.isDefinedAt(a)

  /** Handle an incoming message.This invokes the associated message handler and, if a new state results, updates the
    * user actor and any associated scenes.
    *
    * @param a
    *   the incoming message
    */
  private def handleMessage(a: Any): Unit =
    messageHandlers(a)(stateData) foreach updateState

  /** Update the state of this actor. This informs the session and scenes actor of the new state.
    *
    * @param data
    *   the new presence state
    */
  private def updateState(data: StateData): Unit =
    sendState(data, init = false)
    stateData = data

  /** Send out state data to interested actors.
    * @param data
    *   the new state data
    * @param init
    *   whether this should include initial state information
    */
  private def sendState(data: StateData, init: Boolean): Unit =
    // Tell the session about my state
    sessionActor ! PresenceInfo(data)
    if init || (data.lastActive != stateData.lastActive) || (data.visible != stateData.visible) ||
      (data.lastReported != stateData.lastReported) || (data.inScenes != stateData.inScenes) ||
      (data.followScenes != stateData.followScenes || (data.lastHeartbeat != stateData.lastHeartbeat))
    then // eesh. there should be a better way to signal visible change vs, say, sse subscribe
      val presenceInScenes = PresenceInScenes(
        userHandle,
        data.lastActive,
        data.visible,
        data.lastReported,
        data.inScenes,
        stateData.inScenes -- data.inScenes,
        SetDelta `from` init.fold(Set.empty[SceneId], stateData.followScenes) `to` data.followScenes
      )
      scenesActor ! presenceInScenes
      timeOnTaskActor ! SceneActivity(data.inScenes, data.lastHeartbeat)
    end if
  end sendState

  /** Get the session actor, my parent. */
  private def sessionActor: ActorRef = context.parent

  /** Receive timeout handler. If no activity has occurred in a substantial time, this actor poisons itself.
    *
    * @return
    *   the new actor state
    */
  private val onReceiveTimeout: StateData => Option[StateData] = _ =>
    // I shut down on expire timeout even if there is an associated SSE actor
    // in case the SSE actor has just not heard about browser death.
    logger.info(s"Receive timeout for presence actor for user $userId")
    if new Date() - stateData.lastReported >= IdleTimeout then
      logger.info(s"Poisoning presence actor for user $userId")
      self ! PoisonPill
    None

  /** Get the supported message handlers. This is a partial function that accepts in incoming message and returns a
    * side-effecting function from the current actor state to the new actor state.
    */
  private val messageHandlers: StateTransitions = {
    case ReceiveTimeout                                                                           =>
      onReceiveTimeout
    case Subscribe(lastId)                                                                        =>
      onSseSubscribe(lastId)
    case InfoRequest                                                                              =>
      onInfoRequest
    case EventsRequest(id)                                                                        =>
      onEventsRequest(id)
    case PresenceUpdate(lastActive, visible, inScenes, followScenes, lastId, heartbeat, reported) =>
      onPresenceUpdate(lastActive, visible, inScenes, followScenes, lastId, heartbeat, reported)
    case DeliverMessage(eventType, message)                                                       =>
      onDeliverMessage(eventType, message)
    case Terminated(actor)                                                                        =>
      onTerminated(actor)
  }

  /** SSE subscription handler. Terminates any currently-associated SSE actor, then forwards any outstanding SSE
    * messages to the new actor.
    *
    * @param lastId
    *   the last event identifier seen by the client
    * @param data
    *   the current actor state
    * @return
    *   the new actor state
    */
  private def onSseSubscribe(lastId: Option[Long])(data: StateData): Option[StateData] =
    // Poison any existing actor
    data.sseActor foreach { actor =>
      context unwatch actor
      actor ! PoisonPill
    }
    // Find the messages no seen by the client
    val messages = backlog(data.messages, lastId)
    messages foreach { msg =>
      sender() ! msg
    }
    // Return new state discarding any seen messages.
    Some(data.copy(sseActor = Some(sender()), messages = messages))
  end onSseSubscribe

  /** Presence information request. Returns current presence state.
    *
    * @param data
    *   the current state
    * @return
    *   the new state
    */
  private def onInfoRequest(data: StateData): Option[StateData] =
    sender() ! PresenceInfo(data)
    None

  /** Presence event request. Returns all events since `lastId`.
    *
    * Clears all events under the assumption that they are received by the caller.
    *
    * This is less principled than only dropping the seen events but it makes this endpoint easier to use for
    * performance tests without leaking events.
    *
    * @param lastId
    *   the last id seen, or `None` to return all events
    * @param data
    *   the current state
    * @return
    *   the current state, unchanged
    */
  private def onEventsRequest(lastId: Option[Long])(data: StateData): Option[StateData] =
    val unseen = backlog(data.messages, lastId)
    sender() ! PresenceEvents(unseen)
    Some(data.copy(messages = Vector.empty))

  /** Presence update notification. Updates the internal state.
    *
    * @param lastActive
    *   the user's last activity time
    * @param visible
    *   whether the user is visible
    * @param inScenes
    *   the scenes that the user is active in
    * @param followScenes
    *   the scenes that the user is following
    * @param lastEventId
    *   the last event id seen by the client
    * @param data
    *   the current state
    * @return
    *   the new state
    */
  private def onPresenceUpdate(
    lastActive: Option[Date],
    visible: Option[Boolean],
    inScenes: Option[Set[SceneId]],
    followScenes: Option[Set[SceneId]],
    lastEventId: Option[Long],
    heartbeat: Option[Heartbeat],
    reported: Date,
  )(data: StateData): Option[StateData] =
    // Filter out seen messages
    val messages = backlog(data.messages, lastEventId)
    Some(
      data.copy(
        lastActive = lastActive.getOrElse(data.lastActive),
        lastHeartbeat = heartbeat.getOrElse(data.lastHeartbeat),
        visible = visible.getOrElse(data.visible),
        inScenes = inScenes.getOrElse(data.inScenes),
        followScenes = followScenes.getOrElse(data.followScenes),
        lastReported = reported,
        messages = messages
      )
    )
  end onPresenceUpdate

  /** Add a new message to be delivered to the client. Forwards it to the associated SSE client and adds it to the
    * backlog.
    *
    * @param eventType
    *   the event type
    * @param msg
    *   the message
    * @param data
    *   the current state
    * @return
    *   the new state
    */
  private def onDeliverMessage(eventType: String, msg: Any)(data: StateData): Option[StateData] =
    val eventId  = System.currentTimeMillis max (1 + data.eventId)
    val message  = Event(eventId, eventType, msg)
    // Forward to the connected client
    data.sseActor foreach { sse =>
      logger.debug(s"PresenceActor $self deliver $message to $sse")
      sse ! message
    }
    val messages = (data.messages.filterNot(obe(message)) :+ message).transformWhen(_.size > MaxMessages) { msgs =>
      // TODO: Send a control message to the front-end
      logger.warn(s"Dropping message for $userId")
      msgs.drop(MaxMessages - msgs.size)
    }
    Some(data.copy(eventId = eventId, messages = messages))
  end onDeliverMessage

  /** Returns a function that tests whether an event has been overtaken by a more recent update. */
  private def obe(update: Event): Event => Boolean = update.event match
    case SceneActor.ScenePresence.Type =>
      overtakenByScenePresence(update.body.asInstanceOf[SceneActor.ScenePresence])

    case _ => Misc.falsely

  /** Returns whether a given event has been overtaken by a more recent scene presence update. */
  private def overtakenByScenePresence(update: SceneActor.ScenePresence)(event: Event): Boolean =
    (event.event == SceneActor.ScenePresence.Type)
      .option(event.body.asInstanceOf[SceneActor.ScenePresence])
      .exists(_.scene == update.scene)

  // Both string match and asInstanceOf above could be more elegantly generalized in some manner as
  // def eventMatcher[T : EventType](e: Event): Option[T]

  /** Actor termination. Handles death of the SSE actor.
    *
    * @param actor
    *   the dead actor
    * @param data
    *   the current state
    * @return
    *   the new state
    */
  private def onTerminated(actor: ActorRef)(data: StateData): Option[StateData] =
    Some(data.copy(sseActor = data.sseActor - actor))
end PresenceActor

/** Presence actor companion.
  */
object PresenceActor:

  /** The logger. */
  private val logger = org.log4s.getLogger

  /** Presence actor reference. */
  type Ref = ActorRef @@ PresenceActor

  /** Maximum number of messages we will queue. */
  val MaxMessages = 64

  /** State transitions type is a partial function from a message to a state transition function.
    */
  private type StateTransitions =
    PartialFunction[Any, StateData => Option[StateData]]

  /** Create a new presence actor.
    *
    * @param lastActive
    *   when the user was last active
    * @param visible
    *   the user's visibility
    * @param inScenes
    *   the scenes in which the actor is present
    * @param followScenes
    *   the scenes which the actor is following
    * @param userId
    *   the user id
    * @param userHandle
    *   the user handle
    * @param scenes
    *   the scenes actor
    * @param factory
    *   the actor factory
    * @return
    *   the new presence actor
    */
  def create(
    lastActive: Date,
    visible: Boolean,
    heartbeat: Heartbeat,
    inScenes: Set[SceneId],
    followScenes: Set[SceneId],
    userId: Long,
    userHandle: String,
    scenes: ScenesActor.Ref,
    timeOnTask: TimeOnTaskActor.Ref
  )(implicit factory: ActorRefFactory): Ref =
    Tag.of[PresenceActor] {
      factory.actorOf(
        Props(
          new PresenceActor(
            StateData(lastActive, visible, heartbeat, inScenes, followScenes),
            userId,
            userHandle,
            scenes,
            timeOnTask
          )
        )
      )
    }

  /** Filter out messages from the message queue that the client has already seen.
    *
    * @param messages
    *   the message queue
    * @param lastId
    *   the last event id seen by the client
    * @return
    *   the filtered messages
    */
  private def backlog(messages: Vector[Event], lastId: Option[Long]): Vector[Event] =
    lastId.fold(messages) { id =>
      messages dropWhile { msg =>
        msg.id <= id
      }
    }

  /** An SSE actor subscription request. The subscribed actor will receive a sequence of {Event} messages.
    *
    * @param lastId
    *   the last event id seen by the client
    */
  case class Subscribe(lastId: Option[Long])

  /** An event to stream to the client. This will be send to the subscribed SSE actor.
    *
    * @param id
    *   the event id
    * @param event
    *   the event type
    * @param body
    *   the event body
    */
  case class Event(id: Long, event: String, body: Any)

  /** A request to deliver a message to the client.
    *
    * @param eventType
    *   the event type
    * @param message
    *   the message
    */
  case class DeliverMessage(
    eventType: String,
    message: Any
  )

  /** Deliver message companion.
    */
  object DeliverMessage:

    /** Construct a deliver message request from a typed message.
      *
      * @param message
      *   the message
      * @tparam A
      *   the message type
      * @return
      *   the deliver message request
      */
    def apply[A: EventType](message: A): DeliverMessage =
      DeliverMessage(EventType[A].eventType(message), message)
  end DeliverMessage

  /** A presence update request. All fields are optional, only those present will be applied to the actor state.
    *
    * @param lastActive
    *   when the user was last active
    * @param visible
    *   the user's visibility
    * @param inScenes
    *   the scenes that the user is active in
    * @param followScenes
    *   the scenes that the user is following
    * @param lastEventId
    *   the laste event id seen by the client
    */
  case class PresenceUpdate(
    lastActive: Option[Date],
    visible: Option[Boolean],
    inScenes: Option[Set[SceneId]],
    followScenes: Option[Set[SceneId]],
    lastEventId: Option[Long], // last-seen event id
    heartbeat: Option[Heartbeat],
    reported: Date,
  )

  /** A request for events in this presence.
    *
    * @param lastId
    *   the event id after which to start returning events
    */
  case class EventsRequest(lastId: Option[Long] = None)

  /** A list of events in response to an `EventsRequest`. */
  case class PresenceEvents(events: Seq[Event])

  /** A presence information request.
    */
  case object InfoRequest

  /** A presence information response.
    *
    * @param lastActive
    *   the user's last active time
    * @param lastReported
    *   when the client last reported in
    * @param visible
    *   the user's visibility
    * @param inScenes
    *   the scenes that the user is active in
    */
  case class PresenceInfo(
    lastActive: Date,
    lastReported: Date,
    visible: Boolean,
    inScenes: Set[SceneId]
  )

  /** Presence info companion.
    */
  object PresenceInfo:

    /** Transform the actor state to presence information.
      *
      * @param data
      *   the actor state
      * @return
      *   the presence information
      */
    def apply(data: StateData): PresenceInfo =
      PresenceInfo(data.lastActive, data.lastReported, data.visible, data.inScenes)
  end PresenceInfo

  /** The idle timeout after which the presence actor will poison itself. */
  private final val IdleTimeout = 10.minutes

  /** The presence actor state data.
    *
    * @param lastActive
    *   when the user was last active as reported by the front end
    * @param visible
    *   whether the user is visible
    * @param lastHeartbeat
    *   the most recent heartbeat received by this presence actor
    * @param inScenes
    *   the scenes in which the user is active
    * @param followScenes
    *   the scenes which the user is following
    * @param lastReported
    *   when the front end sent this particular heartbeat
    * @param eventId
    *   the current event identifier
    * @param messages
    *   the backlog of client messages
    * @param sseActor
    *   the associated SSE actor
    */
  case class StateData(
    lastActive: Date,
    visible: Boolean,
    lastHeartbeat: Heartbeat,
    inScenes: Set[SceneId] = Set.empty,
    followScenes: Set[SceneId] = Set.empty,
    lastReported: Date = new Date(),
    eventId: Long = 0L,
    messages: Vector[Event] = Vector.empty,
    sseActor: Option[ActorRef] = None,
  )

  /** @param time
    *   server time of heartbeat receipt
    * @param activeMillis
    *   active milliseconds between the last heartbeat.time and this heartbeat.time
    */
  case class Heartbeat(
    time: Date,
    activeMillis: Long
  )
end PresenceActor
