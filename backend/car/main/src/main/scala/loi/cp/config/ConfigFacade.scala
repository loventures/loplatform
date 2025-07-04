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

package loi.cp.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.learningobjects.cpxp.dto.*
import com.learningobjects.cpxp.service.config.ConfigurationConstants

@FacadeItem("*")
private[config] trait ConfigFacade extends Facade:

  @FacadeData(ConfigurationConstants.DATA_TYPE_CONFIGURATION_BLOB)
  def getConfig: ObjectNode
  def setConfig(cfg: ObjectNode): Unit

  @FacadeJson(ConfigurationConstants.DATA_TYPE_CONFIGURATION_BLOB)
  def getAttr(name: String): JsonNode
  def setAttr[T](name: String, value: T): Unit
  def removeAttr(name: String): Unit

  def refresh(pessi: Boolean = true): Unit
end ConfigFacade
