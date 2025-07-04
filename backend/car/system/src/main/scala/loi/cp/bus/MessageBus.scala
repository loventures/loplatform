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

import com.fasterxml.jackson.annotation.{JsonProperty, JsonView}
import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.ComponentInterface
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.service.component.misc.MessageBusConstants
import com.learningobjects.de.web.Queryable
import loi.cp.integration.SystemComponent

import java.util.Date
import javax.validation.groups.Default

@ItemMapping(value = MessageBusConstants.ITEM_TYPE_MESSAGE_BUS, singleton = true)
@Schema("messageBus")
trait MessageBus extends ComponentInterface with Id:
  @JsonProperty
  def getName: String

  @JsonProperty
  def getState: MessageBusState
  def setState(state: MessageBusState): Unit

  @JsonProperty
  def getScheduled: Date

  @JsonProperty
  def getStatistics: BusStatistics

  @JsonProperty
  def getQueueSize: Long

  @Queryable(dataType = MessageBusConstants.DATA_TYPE_MESSAGE_BUS_SYSTEM)
  @JsonView(Array(classOf[Default]))
  def getSystem: SystemComponent[?]
end MessageBus
