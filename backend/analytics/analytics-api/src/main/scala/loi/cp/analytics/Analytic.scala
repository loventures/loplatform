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

import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.annotation.Schema
import com.learningobjects.cpxp.component.web.util.ExtendedISO8601DateFormat
import com.learningobjects.cpxp.service.component.misc.AnalyticConstants
import com.learningobjects.de.web.Queryable
import loi.cp.analytics.event.Event

@Schema("analytic")
trait Analytic extends Id:

  @Queryable(dataType = AnalyticConstants.DATA_TYPE_ANALYTIC_GUID)
  @JsonProperty
  def guid: UUID

  /** String because we want to force UTC formatting instead of default "Fri, May 13 HH:MM....etc."
    */
  @Queryable(dataType = AnalyticConstants.DATA_TYPE_ANALYTIC_TIME)
  @JsonProperty
  def time: String

  @JsonProperty
  def eventData: Event
end Analytic

object Analytic:
  val DateFormat = new ExtendedISO8601DateFormat()

  def unapply(arg: Analytic): Option[(UUID, String, Event)] =
    Some((arg.guid, arg.time, arg.eventData))
