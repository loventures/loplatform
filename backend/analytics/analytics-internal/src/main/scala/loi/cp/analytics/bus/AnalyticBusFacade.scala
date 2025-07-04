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

package loi.cp.analytics.bus

import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.dto.{Facade, FacadeData, FacadeItem}
import com.learningobjects.cpxp.service.component.misc.AnalyticBusFinder.DATA_TYPE_ANALYTIC_BUS_LAST_MVR_DATE
import com.learningobjects.cpxp.service.component.misc.AnalyticConstants.*
import loi.cp.analytics.AnalyticsSystem
import loi.cp.bus.BusStatistics

import java.util.Date

/** Analytics bus facade.
  */
@FacadeItem(ITEM_TYPE_ANALYTIC_BUS)
trait AnalyticBusFacade extends Facade:
  @FacadeData(DATA_TYPE_ANALYTIC_BUS_SCHEDULED)
  def getScheduled: Date
  def setScheduled(when: Date): Unit

  @FacadeData(DATA_TYPE_ANALYTIC_BUS_SYSTEM)
  def getSystem: AnalyticsSystem[? <: AnalyticsSystem[?]]
  def setSystem(id: Id): Unit

  @FacadeData(DATA_TYPE_ANALYTIC_BUS_SENDER_IDENTIFIER)
  def getSenderIdentifier: Option[String]
  def setSenderIdentifier(sender: Option[String]): Unit

  @FacadeData(DATA_TYPE_ANALYTIC_BUS_WINDOW_TIME)
  def getWindowTime: Long
  def setWindowTime(window: Long): Unit

  @FacadeData(DATA_TYPE_ANALYTIC_BUS_WINDOW_IDS)
  def getWindowIds: Seq[Long]
  def setWindowIds(ids: Seq[Long]): Unit

  @FacadeData(DATA_TYPE_ANALYTIC_BUS_STATISTICS)
  def getStatistics: BusStatistics
  def setStatistics(statistics: BusStatistics): Unit

  @FacadeData(DATA_TYPE_ANALYTIC_BUS_STATE)
  def getState: AnalyticBusState
  def setState(state: AnalyticBusState): Unit

  @FacadeData(DATA_TYPE_ANALYTIC_BUS_FAILURE_COUNT)
  def getFailureCount: Long
  def setFailureCount(count: Long): Unit

  @FacadeData(DATA_TYPE_ANALYTIC_BUS_CONFIGURATION)
  def getConfiguration: AnalyticBusConfiguration
  def setConfiguration(configuration: AnalyticBusConfiguration): Unit

  @FacadeData(DATA_TYPE_ANALYTIC_BUS_LAST_MVR_DATE)
  def getLastMaterializedViewRefreshDate: Option[Date]
  def setLastMaterializedViewRefreshDate(date: Option[Date]): Unit

  def lock(pessimistic: Boolean): Unit
  def refresh(timeout: Long): Boolean
end AnalyticBusFacade
