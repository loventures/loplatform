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

package loi.cp.apikey

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty}
import com.learningobjects.cpxp.component.annotation.Schema
import loi.cp.integration.SystemComponent
import loi.cp.right.{Right, RightService}
import loi.cp.user.UserComponent

@Schema("apiKeyConnector")
trait ApiKeySystem extends SystemComponent[ApiKeySystem]:
  @JsonProperty
  def getRights: String

  @JsonIgnore
  def getRightClasses(implicit rs: RightService): Set[Class[? <: Right]]

  @JsonProperty
  def getWhiteList: String

  @JsonIgnore
  def getWhiteListIps: Set[String]

  @JsonIgnore
  def getSystemUser: UserComponent
end ApiKeySystem

final case class ApiKeyConfiguration(
  /** List of permitted IP addresses or subnets. Dangerously, if empty then assumes that all IPs are allowed.
    */
  whiteList: Set[String] = Set.empty
)
