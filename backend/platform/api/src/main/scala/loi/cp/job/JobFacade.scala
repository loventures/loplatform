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

package loi.cp.job

import com.learningobjects.cpxp.component.DataModel
import com.learningobjects.cpxp.dto.*
import com.learningobjects.cpxp.service.component.ComponentConstants
import com.learningobjects.cpxp.service.job.JobFinder.*
import com.learningobjects.cpxp.service.query.QueryBuilder

@FacadeItem(ITEM_TYPE_JOB)
trait JobFacade extends Facade:
  @FacadeData(DATA_TYPE_JOB_NAME)
  def getName: String
  def setName(name: String): Unit

  @FacadeData(DATA_TYPE_JOB_DISABLED)
  def getDisabled: Boolean
  def setDisabled(disabled: Boolean): Unit

  @FacadeData(DATA_TYPE_JOB_SCHEDULE)
  def getSchedule: String
  def setSchedule(schedule: String): Unit

  @FacadeJson(DATA_TYPE_JOB_JSON)
  def setAttribute[T](attribute: String, t: T): Unit
  def getAttribute[T](attribute: String, t: Class[T]): T

  @FacadeData(ComponentConstants.DATA_TYPE_COMPONENT_IDENTIFIER)
  def getIdentifier: String
  def setIdentifier(identifier: String): Unit

  @FacadeComponent
  def getRun(id: Long)(implicit dataModel: DataModel[Run]): Option[Run]
  def queryRuns(implicit dataModel: DataModel[Run]): QueryBuilder
end JobFacade
