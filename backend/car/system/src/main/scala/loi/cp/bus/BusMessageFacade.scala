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

import com.fasterxml.jackson.databind.node.ObjectNode
import com.learningobjects.cpxp.dto.*
import com.learningobjects.cpxp.service.component.misc.MessageBusConstants

import java.util.Date

@FacadeItem(MessageBusConstants.ITEM_TYPE_BUS_MESSAGE)
trait BusMessageFacade extends Facade:
  @FacadeData
  def getScheduled: Date
  def setScheduled(scheduled: Date): Unit

  @FacadeData
  def getAttempts: Long
  def setAttempts(attempts: Long): Unit

  @FacadeData
  def getState: BusMessageState
  def setState(state: BusMessageState): Unit

  @FacadeData
  def getType: String
  def setType(`type`: String): Unit

  @FacadeData
  def getBody: ObjectNode
  def setBody(body: AnyRef): Unit

  @FacadeData
  def setBus(bus: MessageBusFacade): Unit
  def getBus: MessageBusFacade
end BusMessageFacade
