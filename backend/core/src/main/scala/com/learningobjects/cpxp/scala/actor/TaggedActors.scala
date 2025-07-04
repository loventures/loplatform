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

import org.apache.pekko.actor.{ActorContext, ActorRef, Actor as TheActor}
import org.apache.pekko.util.Timeout
import scalaz.syntax.tag.*
import scalaz.{@@, Tag}

import scala.concurrent.Future
import scala.reflect.ClassTag

/** Helpers for working with tagged actors. This supports using most actor reference and actor context methods directly
  * on tagged actors, avoiding the need for extensive use of {Tag.unwrap}.
  */
object TaggedActorOps:
  import org.apache.pekko.pattern.*

  /** Enriches tagged actor refs to support asking and telling.
    *
    * @param self
    *   the actor ref
    * @tparam T
    *   the tag type
    */
  final class TaggedActorRefOps[T](val self: ActorRef @@ T) extends AnyVal:

    /** Send the actor a message.
      *
      * @param message
      *   the message
      * @param sender
      *   the sender
      */
    @inline def !(message: Any)(implicit sender: ActorRef = TheActor.noSender): Unit = self.unwrap ! message

    /** Send the actor a message.
      *
      * @param message
      *   the message
      * @param sender
      *   the sender
      */
    @inline def tell(message: Any, sender: ActorRef): Unit =
      self.unwrap.tell(message, sender)

    /** Forward the actor a message.
      *
      * @param message
      *   the message
      * @param context
      *   the actor context
      */
    @inline def forward(message: Any)(implicit context: ActorContext): Unit =
      self.unwrap.tell(message, context.sender())

    /** Send the actor a message and expect a reply.
      *
      * @param message
      *   the message
      * @param timeout
      *   the timeout
      * @param sender
      *   the sender
      * @return
      *   the future reply
      */
    @inline def ask(message: Any)(implicit timeout: Timeout, sender: ActorRef = TheActor.noSender): Future[Any] =
      new AskableActorRef(self.unwrap).ask(message)

    /** An alias for ask. */
    @inline def ?(message: Any)(implicit timeout: Timeout, sender: ActorRef = TheActor.noSender): Future[Any] = ask(
      message
    )

    /** Send the actor a message and expect a reply of a particular type.
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
    @inline
    def askFor[A: ClassTag](message: Any)(implicit timeout: Timeout, sender: ActorRef = TheActor.noSender): Future[A] =
      new AskableActorRef(self.unwrap).ask(message).mapTo[A]
  end TaggedActorRefOps

  /** Enhancements on actor contexts to work with tagged actors.
    *
    * @param self
    *   the actor context
    */
  final class TaggedActorContextOps(val self: ActorContext) extends AnyVal:

    /** Watch an actor for death.
      *
      * @param a
      *   the actor
      * @tparam T
      *   the tag type
      * @return
      *   the actor
      */
    def watch[T](a: ActorRef @@ T): ActorRef @@ T =
      Tag.of[T](self.watch(a.unwrap))
  end TaggedActorContextOps
end TaggedActorOps

trait ToTaggedActorOps extends Any:
  import TaggedActorOps.*

  import language.implicitConversions

  implicit final def toTaggedActorRefOps[T](self: ActorRef @@ T): TaggedActorRefOps[T] =
    new TaggedActorRefOps[T](self)

  implicit final def toTaggedActorContextOps(self: ActorContext): TaggedActorContextOps =
    new TaggedActorContextOps(self)
end ToTaggedActorOps

/** Tagged actor helper companion.
  */
object TaggedActors extends ToTaggedActorOps
