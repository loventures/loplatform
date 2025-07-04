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
import org.apache.pekko.event.LoggingReceive
import com.learningobjects.cpxp.async.messages.queue.*
import com.learningobjects.cpxp.scala.util.Misc.*
import org.apache.commons.lang3.time.DateUtils

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.language.postfixOps

class SimpleQueue(messageStrat: MessageHandlerStrategy)
    extends Actor
    with Queue
    // with ActorLogging
    with Subscribable:
  val messages: mutable.Set[Event] = mutable.Set()
  var idCounter                    = 0L

  context.system.scheduler.scheduleWithFixedDelay(1 minute, 1 minute, self, CleanUp)
  context.setReceiveTimeout(messageStrat.EXPIRE_INTERVAL minutes)

  val log = org.apache.pekko.event.Logging(context.system, this)
  log.debug(s"Created new SimpleQueue for ${self.path}")

  override def receive =
    LoggingReceive(receiveSubscription orElse {
      case message: Event =>
        idCounter += 1L
        val newMessage = addMessage(message)
        subscribers foreach { _ ! newMessage }
      case Remove(event)  =>
        messages.find(e => e.id == event).foreach(e => messages.remove(e))
      case Clear          =>
        messages.clear()
      case CleanUp        =>
        val expireDate      =
          DateUtils.addMinutes(now, -messageStrat.EXPIRE_INTERVAL)
        val expiredMessages = messages filter { m =>
          m.timestamp.before(expireDate)
        }
        log debug s"Expired Messages: $expiredMessages"
        messages --= expiredMessages
        ()
    })

  def addMessage(message: Event): Event =
    val handledMessage =
      (messageStrat.preAdd orElse DefaultMessageHandlerStrategy.preAdd)((message, idCounter))
    messages add handledMessage
    log debug s"Added message $handledMessage"
    handledMessage

  override def onSubscribe(newSub: ActorRef): Unit = messages foreach {
    newSub ! _
  }

  override def onResume(newSub: ActorRef, id: Long): Unit =
    val (_, messagesAfter) = messages partition { event =>
      event.id <= id
    }
    messagesAfter foreach { newSub ! _ }

  override def onSubscriberTimeout(): Unit =
    log info "Queue Timeout, killing self"
    self ! PoisonPill
end SimpleQueue

trait MessageHandlerStrategy:
  val EXPIRE_INTERVAL = 1 // Expire Messages older than one minute
  def preAdd: PartialFunction[(Event, Long), Event]

object DefaultMessageHandlerStrategy extends MessageHandlerStrategy:
  // Expire Messages older than one minute
  def preAdd: PartialFunction[(Event, Long), Event] = { case (message, id) =>
    message
  }
