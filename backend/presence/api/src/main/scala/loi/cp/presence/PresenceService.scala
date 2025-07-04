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

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.presence.EventType
import loi.cp.presence.SceneActor.SceneId

import scala.jdk.CollectionConverters.*
import scaloi.syntax.boxes.*

/** Service for delivering messages to active users.
  */
@Service
trait PresenceService:

  /** Schedules delivery of a message to a user's active sessions upon completion of the current transaction. Any of the
    * users that lack active sessions are ignored.
    */
  def deliverToUsers[A <: AnyRef: EventType](message: A)(userIds: Long*): Unit

  final def deliverToUsersJava[A <: AnyRef](
    message: A,
    eventType: EventType[A],
    userIds: java.util.List[java.lang.Long]
  ): Unit = deliverToUsers(message)(userIds.asScala.toSeq.unboxInside()*)(using eventType)

  /** Schedules delivery of a message to a scene's active sessions upon completion of the current transaction. If the
    * scene doesn't exist or has no active sessions then no action is taken.
    */
  def deliverToScene[A <: AnyRef: EventType](message: A)(sceneId: SceneId): Unit
end PresenceService
