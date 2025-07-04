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

package com.learningobjects.cpxp.scala.actor

import org.apache.pekko.actor.{ActorRef, Actor as TheActor}
import org.apache.pekko.pattern.*
import org.apache.pekko.serialization.Serialization
import org.apache.pekko.util.Timeout
import scalaz.Equal

import scala.concurrent.Future
import scala.language.implicitConversions
import scala.reflect.ClassTag

/** Enhancements on actor refs.
  * @param self
  *   the actor reference.
  */
final class ActorRefOps(val self: ActorRef) extends AnyVal:

  /** Send the actor a message and expect a reply of a particular type.
    *
    * {{{
    *   actor.askFor[T](question)
    * }}}
    *
    * is equivalent to the standard pekko pattern with mapTo:
    *
    * {{{
    *   actor.ask(question).mapTo[T]
    * }}}
    *
    * @param message
    *   the message
    * @param timeout
    *   the timeout
    * @param sender
    *   the sender
    * @tparam A
    *   the reply type
    * @return
    *   the future reply
    */
  def askFor[A: ClassTag](message: Any)(implicit timeout: Timeout, sender: ActorRef = TheActor.noSender): Future[A] =
    self.ask(message).mapTo[A]

  /** Sends a message to an actor, optionally first verifying that it is serializable.
    * @param message
    *   the message to send
    * @param sender
    *   the sender
    * @param serialization
    *   an optional serializer to use for verifying serializability
    */
  def !!!(
    message: AnyRef
  )(implicit sender: ActorRef = TheActor.noSender, serialization: Option[Serialization]): Unit =
    serialization.map(_.findSerializerFor(message)) foreach { serializer =>
      try serializer.fromBinary(serializer.toBinary(message), manifest = None)
      catch case e: Throwable => throw new IllegalArgumentException(s"Message is not serializable: $message", e)
    }
    self ! message
end ActorRefOps

/** Actor ref operations companion.
  */
object ActorRefOps extends ToActorRefOps

/** Implicit conversion for actor ref operations.
  */
trait ToActorRefOps:

  /** Implicit conversion from an actor ref to enhancements.
    * @param value
    *   the actor ref
    * @return
    *   the enhanced actor ref
    */
  implicit def toActorRefOps(value: ActorRef): ActorRefOps = new ActorRefOps(value)

  /** Equal by reference. */
  implicit val actorEqual: Equal[ActorRef] = Equal.equal(_ equals _)
end ToActorRefOps
