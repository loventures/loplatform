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

import org.apache.pekko.actor.{Actor, ActorRef, PoisonPill, Terminated}
import com.learningobjects.cpxp.async.messages.subscription.*

import scala.annotation.nowarn
import scala.collection.mutable
import scala.concurrent.duration.*
import scala.language.postfixOps

/** A mixin trait to make an Actor subscribable for [[Event]] s.
  */
trait Subscribable:
  self: Actor =>
  protected val subscribers     = mutable.Set[ActorRef]()
  protected var subscriberClock = countDown

  def subscriberTimeout: FiniteDuration = 2 minutes

  /** Compose this method with your receive using orElse, ie:
    *
    * def receive = receiveSubscription orElse { case MyMessage => .... }
    */
  def receiveSubscription: Receive = {
    case Subscribe(sub)       =>
      subscriberClock.cancel()
      context watch sub
      subscribers += sub
      onSubscribe(sub)
    case ResumeSub(id, sub)   =>
      subscriberClock.cancel()
      context watch sub
      subscribers += sub
      onResume(sub, id)
    case Terminated(listener) =>
      subscribers remove listener
      if subscribers.isEmpty then subscriberClock = countDown
    case UnSubscribe(unSub)   => // TODO: Send UnSub Event
      subscribers remove unSub
      if subscribers.isEmpty then subscriberClock = countDown
  }

  // Override these methods to implement callbacks
  @nowarn // parameter is for subclasses
  def onSubscribe(newSub: ActorRef): Unit = ()
  @nowarn // parameter is for subclasses
  def onResume(newSub: ActorRef, id: Long): Unit = ()
  @nowarn // parameter is for subclasses
  def onSubscriberTimeout(): Unit =
    context.self ! PoisonPill

  /** Creates a timer which will execute onSubscriberTimeout unless cancelled by a subscription.
    */
  protected def countDown =
    context.system.scheduler
      .scheduleOnce(subscriberTimeout)(onSubscriberTimeout())(using context.dispatcher)
end Subscribable
