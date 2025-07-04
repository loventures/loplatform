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

package loi.cp.bus

import com.learningobjects.cpxp.dto.*
import com.learningobjects.cpxp.service.component.misc.MessageBusConstants
import com.learningobjects.cpxp.service.folder.FolderConstants
import com.learningobjects.cpxp.service.query.QueryBuilder
import loi.cp.integration.SystemComponent

import java.util.Date

@FacadeItem(FolderConstants.ITEM_TYPE_FOLDER)
trait MessageBusParentFacade extends Facade:
  @FacadeChild
  def queryBuses: QueryBuilder
  def getOrCreateBusBySystem(
    @FacadeCondition(MessageBusConstants.DATA_TYPE_MESSAGE_BUS_SYSTEM)
    system: SystemComponent[?]
  )(init: MessageBusFacade => Unit): MessageBusFacade

// TODO: should the bus be coupled with the system s/t if the system
// is disabled, the bus is, or are they orthogonal...

@FacadeItem(MessageBusConstants.ITEM_TYPE_MESSAGE_BUS)
trait MessageBusFacade extends Facade:

  /** The integration system. */
  @FacadeData
  def getSystem: SystemComponent[?]
  def setSystem(system: SystemComponent[?]): Unit

  /** Bus state. */
  @FacadeData
  def getState: MessageBusState
  def setState(state: MessageBusState): Unit

  /** When next to consider events for this bus. */
  @FacadeData
  def getScheduled: Date
  def setScheduled(date: Date): Unit

  /** Bus statistics. */
  @FacadeData
  def getStatistics: BusStatistics
  def setStatistics(statistics: BusStatistics): Unit

  /** Lock and refresh. */
  def refresh(pessimistic: Boolean): Unit
end MessageBusFacade
