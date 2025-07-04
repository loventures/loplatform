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

package loi.cp.lwgrade

import argonaut.Json
import com.learningobjects.cpxp.dto.*
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.grade.LWGradeFinder
import com.learningobjects.cpxp.service.query.QueryBuilder

@FacadeItem(LWGradeFinder.ITEM_TYPE_LIGHTWEIGHT_GRADE)
trait LWGradeFacade extends Facade:
  @FacadeData(value = LWGradeFinder.DATA_TYPE_LIGHTWEIGHT_USER)
  def getUser: Long
  def setUser(id: Long): Unit

  @FacadeData(value = LWGradeFinder.DATA_TYPE_LIGHTWEIGHT_COURSE)
  def getCourse: Long
  def setCourse(id: Long): Unit

  @FacadeData(value = LWGradeFinder.DATA_TYPE_LIGHTWEIGHT_GRAPH_JSON)
  def getGradeGraph: Json
  def setGradeGraph(json: Json): Unit
end LWGradeFacade

@FacadeItem("*")
trait LWGradeParentFacade extends Facade:
  @FacadeChild(LWGradeFinder.ITEM_TYPE_LIGHTWEIGHT_GRADE)
  def getLWGrade(
    @FacadeCondition(LWGradeFinder.DATA_TYPE_LIGHTWEIGHT_USER) user: Long,
    @FacadeCondition(LWGradeFinder.DATA_TYPE_LIGHTWEIGHT_COURSE) course: Long,
  ): Option[LWGradeFacade]
  def getOrCreateLWGrade(
    @FacadeCondition(LWGradeFinder.DATA_TYPE_LIGHTWEIGHT_USER) user: Long,
    @FacadeCondition(LWGradeFinder.DATA_TYPE_LIGHTWEIGHT_COURSE) course: Long,
  )(init: LWGradeFacade => Unit): LWGradeFacade
  def queryLWGrades: QueryBuilder

  @FacadeData(DataTypes.DATA_TYPE_JSON)
  def getConfigJson: Json
  def setConfigJson(json: Json): Unit
end LWGradeParentFacade
