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

package loi.cp.analytics.event

import com.fasterxml.jackson.annotation.JsonIgnore
import loi.cp.analytics.AnalyticsConstants.EventActionType

import java.util.{Date, UUID}

/** Obfuscate a user.
  * @param userId
  *   the user to obfuscate
  */
case class UserObfuscateEvent1(
  id: UUID,
  time: Date,
  source: String,
  userId: Long,
) extends Event:

  override val actionType: EventActionType         = EventActionType.END
  override val eventType: String                   = this.getClass.getSimpleName
  @JsonIgnore override val sessionId: Option[Long] = None
end UserObfuscateEvent1
