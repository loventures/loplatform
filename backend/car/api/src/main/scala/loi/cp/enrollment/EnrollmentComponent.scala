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

package loi.cp.enrollment

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.component.ComponentInterface
import com.learningobjects.cpxp.component.annotation.{Controller, ItemMapping, RequestMapping, Schema}
import com.learningobjects.cpxp.component.web.Method
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.enrollment.EnrollmentConstants
import com.learningobjects.de.web.{DeletableEntity, Queryable, QueryableId, QueryableProperties}
import loi.cp.context.ContextComponent
import loi.cp.role.RoleComponent
import loi.cp.user.UserComponent

import java.lang as jl
import java.time.Instant
import java.util.Date

// dataSource is deprecated and not supported by this API

@Controller(value = "enrollment", category = Controller.Category.USERS) // REALLY??
@Schema("enrollment")
@ItemMapping(value = EnrollmentConstants.ITEM_TYPE_ENROLLMENT, singleton = true)
@QueryableProperties(
  value = Array(
    new Queryable(name = "active", handler = classOf[ActiveGroupQueryHandler]),
  )
)
trait EnrollmentComponent extends ComponentInterface with QueryableId with DeletableEntity:

  @JsonProperty("user_id")
  @Queryable(dataType = DataTypes.META_DATA_TYPE_PARENT_ID)
  def getUserId: jl.Long

  @RequestMapping(path = "user", method = Method.GET)
  def getUser: UserComponent

  @JsonProperty("context_id")
  @Queryable(dataType = EnrollmentConstants.DATA_TYPE_ENROLLMENT_GROUP)
  def getContextId: jl.Long

  @RequestMapping(path = "context", method = Method.GET)
  def getContext: ContextComponent // but why?!

  @JsonProperty("role_id")
  @Queryable(dataType = EnrollmentConstants.DATA_TYPE_ENROLLMENT_ROLE)
  def getRoleId: jl.Long
  def setRoleId(role: jl.Long): Unit

  @JsonProperty("role_name")
  def getRoleName: String

  @RequestMapping(path = "role", method = Method.GET)
  @Queryable(
    name = "role",
    dataType = EnrollmentConstants.DATA_TYPE_ENROLLMENT_ROLE,
    joinComponent = classOf[RoleComponent]
  )
  def getRole: RoleComponent

  @JsonProperty
  @Queryable(dataType = DataTypes.DATA_TYPE_START_TIME)
  def getStartTime: Date
  def setStartTime(startTime: Date): Unit

  @JsonProperty
  @Queryable(dataType = DataTypes.DATA_TYPE_STOP_TIME)
  def getStopTime: Date
  def setStopTime(stopTime: Date): Unit

  @JsonProperty
  @Queryable(dataType = DataTypes.DATA_TYPE_DISABLED)
  def isDisabled: Boolean

  @JsonProperty
  @Queryable(dataType = EnrollmentConstants.DATA_TYPE_ENROLLMENT_CREATED_ON)
  def getCreatedOn: Instant

  @JsonProperty
  def getDataSource: Option[String]
  def setDataSource(dataSource: Option[String]): Unit
end EnrollmentComponent
