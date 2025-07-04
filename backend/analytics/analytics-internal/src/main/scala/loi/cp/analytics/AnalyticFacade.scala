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

import java.util.{Date, UUID}

import com.learningobjects.cpxp.dto.{Facade, FacadeData, FacadeItem}
import com.learningobjects.cpxp.service.component.misc.AnalyticConstants.*
import loi.cp.analytics.event.Event

/** DEAN Analytics Event
  *
  * Temporarily stored in Postgres in case the Message Queue system is unreachable, we only really care about a UUID for
  * deduplication, and the EventJson.
  */
@FacadeItem(ITEM_TYPE_ANALYTIC)
trait AnalyticFacade extends Facade:

  @FacadeData(DATA_TYPE_ANALYTIC_GUID)
  def getGuid: UUID
  def setGuid(guid: UUID): Unit

  @FacadeData(DATA_TYPE_ANALYTIC_TIME)
  def getTime: Date
  def setTime(time: Date): Unit

  @FacadeData(DATA_TYPE_ANALYTIC_DATA_JSON)
  def getDataJson: Event
  def setDataJson(e: Event): Unit
end AnalyticFacade
