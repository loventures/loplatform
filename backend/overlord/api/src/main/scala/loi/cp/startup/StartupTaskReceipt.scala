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

package loi.cp.startup

import java.util.Date

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.component.ComponentInterface
import com.learningobjects.cpxp.component.annotation.{RequestMapping, Schema}
import com.learningobjects.cpxp.component.web.{Method, WebResponse}
import com.learningobjects.cpxp.startup.TaskState
import com.learningobjects.de.web.{Queryable, QueryableId}

/** Web API model for startup task receipts.
  */
@Schema("startupTaskReceipt")
trait StartupTaskReceipt extends ComponentInterface with QueryableId:
  import StartupTaskReceipt.*

  @JsonProperty(rootIdProperty)
  @Queryable
  def getRootId: Long

  @JsonProperty(identifierProperty)
  @Queryable
  def getIdentifier: String

  @JsonProperty(versionProperty)
  @Queryable
  def getVersion: Long

  @JsonProperty(timestampProperty)
  @Queryable
  def getTimestamp: Date

  @JsonProperty(stateProperty)
  @Queryable
  def getState: TaskState
  def setState(state: TaskState): Unit

  @RequestMapping(path = "logs", method = Method.GET)
  def getLogs: String

  @RequestMapping(path = "logs/download", method = Method.GET)
  def downloadLogs: WebResponse
end StartupTaskReceipt

object StartupTaskReceipt:
  final val identifierProperty = "identifier"
  final val rootIdProperty     = "root_id"
  final val stateProperty      = "state"
  final val timestampProperty  = "timestamp"
  final val versionProperty    = "version"
