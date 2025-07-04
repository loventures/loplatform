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

package loi.cp.startup

import java.io.PrintWriter

import org.apache.pekko.actor.{Actor, ActorRef, ActorRefFactory, PoisonPill, Props, ReceiveTimeout}
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.scala.actor.TaggedActors.*
import loi.cp.sse.{SseEvent, SseWriter}

import scala.concurrent.duration.*
import scalaz.{@@, Tag}

// TODO: with presence merged this should register with the http session actor so that it
// dies with your http session.

/** An actor responsible for streaming startup tasks to an SSE client.
  *
  * @param startupTaskActor
  *   the central startup task actor
  * @param out
  *   the stream to which to write events
  */
class StartupSseActor(
  startupTaskActor: StartupTaskActor.Ref,
  out: PrintWriter
) extends Actor:
  import StartupSseActor.*

  /** Before startup initialization. Registers with the startup task actor.
    */
  override def preStart(): Unit =
    context setReceiveTimeout HeartbeatInterval
    startupTaskActor ! StartupTaskActor.Subscribe
    logger info s"Started SSE actor"

  /** Get the actor handler function.
    *
    * @return
    *   the actor handler function
    */
  override def receive: Receive = {
    case status: StartupTaskActor.UpdateStatus => onUpdateStatus(status)
    case ReceiveTimeout                        => onReceiveTimeout()
  }

  /** Handle a status update from the startup task actor. Streams this to the SSE client.
    *
    * @param status
    *   the status update
    */
  private def onUpdateStatus(status: StartupTaskActor.UpdateStatus): Unit =
    val json = mapper.writeValueAsString(status)
    stream(SseEvent(System.currentTimeMillis.toString, "startup", json))

  /** Handle a receive timeout. Sends a heartbeat to the SSE client.
    */
  private def onReceiveTimeout(): Unit =
    stream(HeartbeatMessage)

  /** After the actor stops, close the SSE connection.
    */
  override def postStop(): Unit =
    stream(StopMessage)
    logger info s"Stopped SSE actor"

  /** Stream an SSE event to the client. If the connection fails, shut down.
    *
    * @param event
    *   the SSE event
    */
  private def stream(event: SseEvent): Unit =
    out `stream` event
    if out.checkError then self ! PoisonPill
end StartupSseActor

/** Startup SSE actor companion.
  */
object StartupSseActor:

  /** The logger. */
  private final val logger = org.log4s.getLogger

  /** A startup SSE actor reference. */
  type Ref = ActorRef @@ StartupSseActor

  /** Create a new startup SSE actor.
    *
    * @param startupTaskActor
    *   the central startup task actor
    * @param out
    *   the SSE stream
    * @param factory
    *   the actor factory
    * @return
    *   a new startup SSE actor
    */
  def create(startupTaskActor: StartupTaskActor.Ref, out: PrintWriter)(implicit factory: ActorRefFactory): Ref =
    Tag.of[StartupSseActor] {
      factory.actorOf(Props(new StartupSseActor(startupTaskActor, out)))
    }

  /** A heartbeat message. */
  private final val HeartbeatMessage = SseEvent("control", "heartbeat")

  /** A stream stop message. */
  private final val StopMessage = SseEvent(SseEvent.CloseId, "control", "stop")

  /** The heartbeat interval. */
  private final val HeartbeatInterval = 45.seconds

  /** The JSON mapper. */
  private final val mapper = JacksonUtils.getMapper
end StartupSseActor
