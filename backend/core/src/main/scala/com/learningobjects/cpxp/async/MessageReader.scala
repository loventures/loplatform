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

package com.learningobjects.cpxp.async

import org.apache.pekko.actor.*
import org.apache.pekko.cluster.Cluster
import org.apache.pekko.event.LoggingReceive
import com.fasterxml.jackson.databind.ObjectMapper
import com.learningobjects.cpxp.async.events.ErrorEvent
import com.learningobjects.cpxp.async.messages.router.*
import com.learningobjects.cpxp.async.messages.subscription.*
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.scala.json.JsonCodec.*
import jakarta.servlet.AsyncContext
import scaloi.misc.TimeSource

import java.io.PrintWriter
import java.util.Date
import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*
import scala.language.postfixOps
import scala.util.Try

object MessageReader:
  object ReadQueues
  case class ConnectedEvent(host: String, routers: Set[String], channel: ChannelId, id: Long, timestamp: Date)
      extends Event
  case class QueueAddedEvent(queuePath: ActorPath, channel: ChannelId, id: Long, timestamp: Date) extends Event

/** An actor that writes EventMessages to client output streams.
  *
  * On init this actor will query a set of given Router actors for the addresses of message queues for each given
  * channel the request is subscribing to.
  *
  * MessageReader 'reads' the state of event queues by pinging them with ReadMessages or ResumeFrom. If a queue contains
  * any relevant messages it will respond directly with them as a collection.
  *
  * @param routers
  *
  * @param channels
  * @param defaultChannel
  *   A defualt channel used to report error, or warning information to.
  */
class MessageReader(
  replay: Boolean,
  lastId: Option[Long],
  out: PrintWriter,
  routers: Iterable[ActorPath],
  channels: Iterable[ChannelId],
  defaultChannel: ChannelId,
  asyncContext: AsyncContext,
)(implicit
  ec: ExecutionContext,
  now: TimeSource,
) extends Actor:
  import MessageReader.*

  val log = org.apache.pekko.event.Logging(context.system, this)

  implicit val mapper: ObjectMapper = JacksonUtils.getMapper

  val queues: mutable.Set[ActorPath] = mutable.Set()
  val messages: mutable.Set[Event]   = mutable.Set()
  val connectTime                    = now.date

  // Schedule to kill self
  val suicideNote = context.system.scheduler.scheduleOnce(5 minutes, self, PoisonPill)
  log debug s"[${self.path}]Starting new MessageReader for $defaultChannel"

  // Check for new queues
  for
    routerPath <- routers
    channel    <- channels
    router      = context.actorSelection(routerPath.toStringWithoutAddress)
  yield
    log debug s"[${self.path}]Querying Router for channel: $channel"
    router ! GetOrCreateChannel(channel)

  val connectedMessage = ConnectedEvent(
    host = Cluster(context.system).selfAddress.toString,
    routers = routers.map(_.toString).toSet,
    channel = "/meta/connection",
    id = 0L,
    timestamp = now.date,
  )
  self ! connectedMessage

  def receive = LoggingReceive {
    case error: ErrorEvent => writeError(error)
    case mail: Event       =>
      if isValid(mail) && messages.add(mail) then writeMessage(mail)
    case set: Iterable[?]  =>
      val mails   = set collect { case e: Event => e }
      val newMail = (mails.toSet filter isValid) -- messages
      messages ++= mails // XXX:In the future a more sophisticated system may be needed to remember messages
      newMail foreach { mail =>
        writeMessage(mail)
      }
    case QueuePath(path)   =>
      val queueMessage = QueueAddedEvent(path, "/meta/queue", 0L, now.date)
      val queueSel     = context.actorSelection(path)

      queues.add(path)
      self ! queueMessage

      queueSel ! lastId.fold[Any](Subscribe(self))(id => ResumeSub(id, self))
  }

  def isValid(message: Event) = replay || message.timestamp.after(connectTime)

  override def postStop(): Unit =
    suicideNote.cancel()
    // if i am killed because the context failed this is reduntant..
    // but if my suicide note killed me then this is good
    Try(asyncContext.complete())
    ()

  def writeMessage(message: Event): Unit =
    writeData(message)

  def writeError(error: ErrorEvent): Unit =
    writeData(error)

  def writeData(data: Event) =
    // we do not guarantee that after async context fails us,
    // that we immediately stop writing. in particular, messages
    // are queue so the poison pill may arrive after messages.
    try
      val json = data.encode[String]
      out `print` s"event: ${data.channel}\n"
      out `print` s"id: ${data.id}\n"
      json `split` "\\r?\\n" foreach { line =>
        out `println` s"data:$line"
      }
      out `print` "\n\n"
      out.flush()
      log debug s"Sent Event Message: $json"
    catch
      case ex: Exception =>
        log.error(ex, s"Error writing Message: $data")
        self ! PoisonPill // shutdown on error
end MessageReader
