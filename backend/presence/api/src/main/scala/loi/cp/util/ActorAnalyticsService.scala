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

package loi.cp.util

import com.learningobjects.cpxp.service.domain.DomainDTO
import loi.cp.analytics.entity.{ExternallyIdentifiableEntity, UserData}
import loi.cp.analytics.event.Event

/** Helper class to contain the analytics event details and ManagedUtils junk
  */
trait ActorAnalyticsService:

  /** An EventBuilder takes a domain id and builds an event
    */
  type EventBuilder = (DomainDTO, UserData, Option[ExternallyIdentifiableEntity], Option[Long]) => Event

  def emitEventWithBuilder(userId: Long, courseId: Option[Long])(builder: EventBuilder): Unit
