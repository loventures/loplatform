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

package loi.cp.analytics

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.analytics.entity.{CourseId, UserData}
import loi.cp.analytics.event.Event

@Service
trait AnalyticsService:

  /** Queues events up for insertion at TX commit. */
  def emitEvent(event: Event): Unit

  /** Inserts the event synchronously in the given domain. Does not do triplets */
  def insertEvent(event: Event, domain: Long): Unit

  def courseId(courseId: Long): Option[CourseId]

  def userData(user: UserDTO): UserData

  /** Pump the analytics poller. */
  private[analytics] def pumpPoller(): Unit = {}
end AnalyticsService
