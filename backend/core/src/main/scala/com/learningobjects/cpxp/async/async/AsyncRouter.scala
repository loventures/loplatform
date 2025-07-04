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

package com.learningobjects.cpxp.async.async

import org.apache.pekko.actor.SupervisorStrategy.{Restart, Stop}
import org.apache.pekko.actor.{Actor, OneForOneStrategy, Props, ReceiveTimeout}
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.learningobjects.cpxp.async.*
import com.learningobjects.cpxp.async.async.AsyncOperationActor.{InProgress, Warning}
import com.learningobjects.cpxp.async.events.HeartbeatMessage
import com.learningobjects.cpxp.async.messages.router.{GetOrCreateChannel, QueuePath}
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.scala.actor.{CpxpActorSystem, Actor as Actors}
import com.learningobjects.cpxp.scala.json.JsonCodec.*
import com.learningobjects.cpxp.scala.json.{Encode, JacksonCodec}
import jakarta.persistence.OptimisticLockException
import scaloi.misc.TimeSource

import scala.concurrent.duration.*

object AsyncQueueStrat extends QueueStrategy:
  def queueType: Props = Props(classOf[SimpleQueue], AsyncMessageHandler)

object AsyncMessageHandler extends MessageHandlerStrategy:
  // Expire Messages older than one minute
  def preAdd: PartialFunction[(Event, Long), Event] = { case (message: AsyncEvent, id) =>
    message.copy(id = id)
  }

object AsyncRouter:
  final val HeartbeatTimeout = 20.seconds

  def op2ChannelId[R](op: AsyncSSEOperation[R]): String =
    guid2channel(op.guid)

  def guid2channel(guid: String) =
    s"/async/$guid"

  lazy val localActor =
    CpxpActorSystem.system.actorOf(Props(new AsyncRouter()(using TimeSource.realtime)), "async")

  lazy val routerBroadcastGroup =
    Actors.createRouterBroadcastGroup(localActor, CpxpActorSystem.system)
end AsyncRouter

class AsyncRouter(
  queueStrat: QueueStrategy = AsyncQueueStrat,
)(implicit
  now: TimeSource
) extends Actor
    with Router:
  import AsyncRouter.*

  implicit val mapper: ObjectMapper = JacksonUtils.getMapper
  implicit def anyEncoder[T]: Encode[T] {
    type JsonRepr = JsonNode
  } = JacksonCodec.jacksonNodeEncode[T]

  override def preStart(): Unit =
    context setReceiveTimeout HeartbeatTimeout

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 3, withinTimeRange = Duration.Inf, loggingEnabled = true) {
      case e: OptimisticLockException => Restart
      case e                          => Stop
    }

  def receive = {
    case operation: AsyncSSEOperation[?] =>
      context.actorOf(queueStrat.queueType, operation.guid) // create queue now
      val worker = context.actorOf(Props(new AsyncOperationActor(operation)))
      worker ! AsyncOperationActor.Perform

    case asyncMessage: AsyncEvent =>
      context.child(asyncMessage.guid).foreach(child => child.forward(asyncMessage))

    case GetOrCreateChannel(channelId) =>
      context.child(channelId.substring(1)).foreach(child => sender() ! QueuePath(child.path))

    case update: InProgress[?] =>
      val asyncMessage = AsyncEvent(
        guid = update.operation.guid,
        origin = update.operation.origin,
        status = "progress",
        body = update.asInstanceOf[InProgress[Any]].encode[JsonNode],
        channel = op2ChannelId(update.operation),
        id = 0L,
        timestamp = now.date,
      )
      self ! asyncMessage

    case warning: Warning[?] =>
      val asyncMessage = AsyncEvent(
        guid = warning.operation.guid,
        origin = warning.operation.origin,
        status = "warning",
        body = warning.asInstanceOf[Warning[Any]].encode[JsonNode],
        channel = op2ChannelId(warning.operation),
        id = 0L,
        timestamp = now.date,
      )
      self ! asyncMessage

    case ReceiveTimeout =>
      context.children foreach (_ ! HeartbeatMessage())
  }
end AsyncRouter
