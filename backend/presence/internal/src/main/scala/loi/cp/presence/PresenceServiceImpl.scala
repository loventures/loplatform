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

import org.apache.pekko.actor.{ActorRef, ActorSystem}
import org.apache.pekko.serialization.{Serialization, SerializationExtension}
import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.actor.ActorRefOps.*
import com.learningobjects.cpxp.service.presence.EventType
import com.learningobjects.cpxp.util.EntityContext
import com.learningobjects.cpxp.util.EntityContextOps.*
import loi.cp.presence.SceneActor.SceneId
import scalaz.syntax.tag.*
import scaloi.syntax.boolean.*

// The users actor should be a DI field but that is not currently practical.

/** Service class for delivering messages to active users.
  */
@Service
class PresenceServiceImpl(
  ec: => EntityContext,
  sm: ServiceMeta,
  system: ActorSystem,
) extends PresenceService:
  import PresenceServiceImpl.*

  override def deliverToUsers[A <: AnyRef: EventType](message: A)(userIds: Long*): Unit =
    if userIds.nonEmpty then
      logger debug s"Scheduling $message to users $userIds"
      deliver(
        UsersActor.clusterActor.unwrap,
        UsersActor.DeliverMessage(EventType[A].eventType(message), message, userIds.toArray)
      )

  override def deliverToScene[A <: AnyRef: EventType](message: A)(sceneId: SceneId): Unit =
    logger debug s"Scheduling $message to scene $sceneId"
    deliver(
      ScenesActor.clusterActor.unwrap,
      ScenesActor.DeliverMessage(EventType[A].eventType(message), message, sceneId)
    )

  /** Deliver a message to an actor upon completion of this transaction.
    */
  private def deliver(actor: ActorRef, message: AnyRef): Unit =
    ec afterCommit {
      logger debug s"Delivering $message"
      actor !!! message
    }

  /** Returns a serializer to validate message serializability in local environments. */
  private implicit def serialization: Option[Serialization] =
    sm.isProdLike.noption(SerializationExtension(system))
end PresenceServiceImpl

object PresenceServiceImpl:
  private final val logger = org.log4s.getLogger
