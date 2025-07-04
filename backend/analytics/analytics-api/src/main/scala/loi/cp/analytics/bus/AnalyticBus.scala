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

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.ComponentInterface
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.service.component.misc.AnalyticConstants
import loi.cp.analytics.{AnalyticsSender, AnalyticsSystem}
import loi.cp.bus.BusStatistics

import java.util.Date

/** Analytic bus component.
  */
@ItemMapping(value = AnalyticConstants.ITEM_TYPE_ANALYTIC_BUS, singleton = true)
@Schema("analyticBus")
trait AnalyticBus extends ComponentInterface with Id:

  /** Get the integration system associated with this analytic bus.
    * @return
    *   the integration system
    */
  @JsonProperty
  def getSystem: Option[AnalyticsSystem[? <: AnalyticsSystem[?]]]

  def getAnalyticsSender: Option[AnalyticsSender]

  @JsonProperty
  def getName: Option[String]

  /** Get when this bus is next scheduled for processing.
    * @return
    *   when this bus is next scheduled for processing
    */
  @JsonProperty
  def getScheduled: Date

  /** Get the delivery statistics for this bus.
    * @return
    *   the delivery statistics for this bus
    */
  @JsonProperty
  def getStatistics: BusStatistics

  /** Get the state of this bus.
    * @return
    *   the state of this bus
    */
  @JsonProperty
  def getState: AnalyticBusState
  def setState(state: AnalyticBusState): Unit

  /** Get the lookback window start date.
    * @return
    *   the lookback window start date
    */
  @JsonProperty
  def getWindowStart: Date

  /** Get the lookback window ID list size.
    * @return
    *   the lookback window ID list size
    */
  @JsonProperty
  def getWindowSize: Int

  /** Get the number of events still to be delivered by this bus.
    * @return
    *   the number of events still to be delivered by this bus
    */
  @JsonProperty
  def getQueueSize: Long

  /** Get the number of times the most recent window has failed to be delivered.
    * @return
    *   the number of recent failures
    */
  @JsonProperty
  def getFailureCount: Long
  def setFailureCount(count: Long): Unit

  @JsonProperty
  def getLastMaterializedViewRefreshDate: Option[Date]

  def pump(): Unit

  def lock(pessimistic: Boolean): Unit
  def refresh(timeout: Long): Boolean
end AnalyticBus
