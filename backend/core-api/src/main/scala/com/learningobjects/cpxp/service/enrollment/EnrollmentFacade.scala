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

package com.learningobjects.cpxp.service.enrollment

import java.time.Instant
import java.util.Date

import java.lang as jl
import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.dto.{Facade, FacadeData, FacadeItem, FacadeParent}
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.enrollment.EnrollmentConstants.*
import com.learningobjects.cpxp.service.group.GroupFacade
import com.learningobjects.cpxp.service.integration.IntegrationConstants
import com.learningobjects.cpxp.service.relationship.RoleFacade
import com.learningobjects.cpxp.service.user.UserFacade

@FacadeItem(ITEM_TYPE_ENROLLMENT)
trait EnrollmentFacade extends Facade:

  @FacadeData(DATA_TYPE_ENROLLMENT_GROUP)
  def getGroup: GroupFacade
  def setGroup(group: Id): Unit

  @FacadeData(DATA_TYPE_ENROLLMENT_GROUP)
  def getGroupId: jl.Long
  def setGroupId(group: Long): Unit

  @FacadeData(DATA_TYPE_ENROLLMENT_ROLE)
  def getRole: RoleFacade
  def setRole(role: String): Unit

  @FacadeData(DATA_TYPE_ENROLLMENT_ROLE)
  def getRoleId: jl.Long
  def setRoleId(role: Long): Unit

  @FacadeData(IntegrationConstants.DATA_TYPE_DATA_SOURCE)
  def getDataSource: String
  def setDataSource(source: String): Unit

  @FacadeData(DataTypes.DATA_TYPE_START_TIME)
  def getStartTime: Date
  def setStartTime(date: Date): Unit

  @FacadeData(DataTypes.DATA_TYPE_STOP_TIME)
  def getStopTime: Date
  def setStopTime(date: Date): Unit

  @FacadeData(DATA_TYPE_ENROLLMENT_CREATED_ON)
  def getCreatedOn: Instant
  def setCreatedOn(date: Instant): Unit

  @FacadeData(DataTypes.DATA_TYPE_DISABLED)
  def getDisabled: jl.Boolean
  def setDisabled(disabled: Boolean): Unit

  @FacadeParent
  def getUser: UserFacade
end EnrollmentFacade
