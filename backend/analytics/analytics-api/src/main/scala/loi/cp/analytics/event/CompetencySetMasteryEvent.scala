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

import java.util.{Date, UUID}

import com.fasterxml.jackson.annotation.JsonInclude
import loi.cp.analytics.AnalyticsConstants.*
import loi.cp.analytics.entity.ExternallyIdentifiableEntity

@JsonInclude(JsonInclude.Include.NON_EMPTY)
final case class CompetencySetMasteryEvent(
  id: UUID,
  time: Date,
  source: String,
  subject: ExternallyIdentifiableEntity,
  competencySetId: Long,
  competencySetGuid: UUID, // TODO: these are String's everywhere else but actually UUIDs underneath. assets might lose guids though, so revisit when that happens.
  competencySetTitle: String
) extends Event:

  override val eventType: String       = this.getClass.getSimpleName
  override val sessionId: Option[Long] = None // mastery is async
  override val actionType              = EventActionType.MASTER

end CompetencySetMasteryEvent
