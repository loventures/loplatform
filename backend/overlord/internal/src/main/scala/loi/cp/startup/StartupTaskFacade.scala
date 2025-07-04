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

import com.learningobjects.cpxp.dto.{Facade, FacadeData, FacadeItem}
import com.learningobjects.cpxp.service.startup.StartupTaskConstants.*
import com.learningobjects.cpxp.startup.TaskState

/** Facade for representing startup task state persisted to the database.
  */
@FacadeItem(ITEM_TYPE_STARTUP_TASK)
trait StartupTaskFacade extends Facade:
  @FacadeData(DATA_TYPE_STARTUP_TASK_IDENTIFIER)
  def getIdentifier: String
  def setIdentifier(identifier: String): Unit
  @FacadeData(DATA_TYPE_STARTUP_TASK_VERSION)
  def getVersion: Long
  def setVersion(version: Long): Unit
  @FacadeData(DATA_TYPE_STARTUP_TASK_TIMESTAMP)
  def getTimestamp: Date
  def setTimestamp(timestamp: Date): Unit
  @FacadeData(DATA_TYPE_STARTUP_TASK_STATE)
  def getState: TaskState
  def setState(state: TaskState): Unit
  @FacadeData(DATA_TYPE_STARTUP_TASK_LOGS)
  def getLogs: String
  def setLogs(logs: String): Unit
end StartupTaskFacade
