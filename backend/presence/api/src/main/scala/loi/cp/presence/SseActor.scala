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

import java.io.PrintWriter

import org.apache.pekko.actor.{Actor, ActorRef, ActorRefFactory, PoisonPill, Props, ReceiveTimeout, Terminated}
import com.fasterxml.jackson.databind.ObjectMapper
import com.learningobjects.cpxp.scala.actor.TaggedActors.*
import com.learningobjects.cpxp.service.presence.EventType
import loi.cp.presence.PresenceActor.Subscribe
import loi.cp.sse.{SseEvent, SseWriter}

import scala.concurrent.duration.*
import scalaz.{@@, Tag}

// TODO: This is basically the same as the StartupSseActor. Unify them.

/** The SSE actor is responsible for streaming presence messages to a client over an SSE stream.
  *
  * @param presence
  *   the presence actor
  * @param lastEventId
  *   the last event id seen by the client
  * @param out
  *   the SSE stream
  */
class SseActor(
  presence: PresenceActor.Ref,
  lastEventId: Option[Long],
  out: PrintWriter,
  mapper: ObjectMapper
) extends Actor:
  import SseActor.*

  /** Before startup, subscribe to the presence actor, schedule the heartbeat and send a start message.
    */
  override def preStart(): Unit =
    context setReceiveTimeout HeartbeatInterval
    context `watch` presence
    presence ! Subscribe(lastEventId)
    stream(StartEvent)

  /** After stop, send a close event.
    */
  override def postStop(): Unit =
    stream(SseEvent.Close)

  /** The actor message handler.
    */
  override val receive: Receive = {
    // TODO: refactor to accept any message that implements an SSE message trait?
    case PresenceActor.Event(id, event, body) => onEvent(id, event, body)
    case ReceiveTimeout                       => onReceiveTimeout()
    case Terminated(_)                        => onTerminated()
  }

  /** Stream an event to the SSE client.
    *
    * @param id
    *   the event identifier
    * @param event
    *   the event type
    * @param body
    *   the event body
    */
  private def onEvent(id: Long, event: String, body: Any): Unit =
    logger.debug(s"SseActor $self stream event $id / $event / $body")
    stream(SseEvent(id.toString, event, mapper.writeValueAsString(body)))

  /** Send a periodic heartbeat message.
    */
  private def onReceiveTimeout(): Unit =
    stream(HeartbeatEvent)

  /** Send a session-end message and terminate this actor if the associated presence actor dies.
    */
  private def onTerminated(): Unit =
    stream(SessionEndedEvent)
    self ! PoisonPill

  /** Send a control channel event.
    *
    * @param event
    *   the event
    * @tparam A
    *   the event type
    */
  private def stream[A: EventType](event: A): Unit =
    stream(SseEvent(EventType[A].eventType(event), mapper.writeValueAsString(event)))

  /** Stream an event to the client and poison this actor if the connection has died.
    *
    * @param event
    *   the SSE event
    */
  private def stream(event: SseEvent): Unit =
    if !out.checkError then out `stream` event
    if out.checkError then self ! PoisonPill
end SseActor

/** The SSE actor companion.
  */
object SseActor:

  /** The logger. */
  private val logger = org.log4s.getLogger

  /** A reference to an SSE actor. */
  type Ref = ActorRef @@ SseActor

  /** Create a new SSE actor.
    *
    * @param presence
    *   the associated presence actor
    * @param lastEventId
    *   the last seen event ID
    * @param out
    *   the SSE stream
    * @param factory
    *   the actor factory
    * @param mapper
    *   the object mapper
    * @return
    *   the new SSE actor reference
    */
  def create(presence: PresenceActor.Ref, lastEventId: Option[Long], out: PrintWriter)(implicit
    factory: ActorRefFactory,
    mapper: ObjectMapper
  ): Ref =
    Tag.of[SseActor] {
      factory.actorOf(Props(new SseActor(presence, lastEventId, out, mapper)))
    }

  /** The interval at which to send heartbeats. */
  private final val HeartbeatInterval = 45.seconds
end SseActor
