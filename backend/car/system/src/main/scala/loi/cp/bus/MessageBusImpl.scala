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

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.component.misc.MessageBusConstants.*
import com.learningobjects.cpxp.service.query.*
import loi.cp.integration.SystemComponent

import java.util.Date

@Component
class MessageBusImpl(
  val componentInstance: ComponentInstance,
  self: MessageBusFacade,
  qs: QueryService
) extends MessageBus
    with ComponentImplementation:
  override def getId = self.getId

  override def getName: String =
    Option(self.getSystem: SystemComponent[?]).fold("Unknown")(_.getName)

  override def getState: MessageBusState = self.getState

  override def setState(state: MessageBusState): Unit = self.setState(state)

  override def getScheduled: Date = self.getScheduled

  override def getSystem: SystemComponent[?] = self.getSystem

  override def getStatistics: BusStatistics = self.getStatistics

  override def getQueueSize: Long =
    qs.queryRoot(self.getRootId, ITEM_TYPE_BUS_MESSAGE)
      .addCondition(DATA_TYPE_BUS_MESSAGE_BUS, "eq", self)
      .addCondition(
        BaseCondition.inIterable(DATA_TYPE_BUS_MESSAGE_STATE, List(BusMessageState.Ready, BusMessageState.Queued))
      )
      .getAggregateResult(Function.COUNT)
      .longValue
end MessageBusImpl
