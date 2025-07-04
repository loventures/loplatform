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

package com.learningobjects.de.group

import com.fasterxml.jackson.annotation.{JsonProperty, JsonView}
import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.ComponentInterface
import com.learningobjects.cpxp.component.annotation.{Controller, ItemMapping, RequestMapping, Schema}
import com.learningobjects.cpxp.component.web.Method
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.group.GroupConstants
import com.learningobjects.de.enrollment.EnrollmentOwner
import com.learningobjects.de.web.Queryable
import com.learningobjects.de.web.Queryable.Trait.{CASE_INSENSITIVE, NO_STEMMING}

import java.time.Instant
import javax.validation.constraints.{NotNull, Size}
import javax.validation.groups.Default

/** A group.
  */
@Controller(value = "group", category = Controller.Category.CONTEXTS)
@Schema("group")
@ItemMapping(value = GroupConstants.ITEM_TYPE_GROUP)
trait GroupComponent extends ComponentInterface with EnrollmentOwner with Id:

  @JsonView(Array(classOf[Default]))
  def getUrl: String

  def setUrl(url: String): Unit

  // TODO these constraints should be implementation-specific, e.g. a carnegie school can be 50 chars,
  // but a carnegie district maxes at 30
  @Size(min = 1, max = 50)
  @NotNull
  @JsonProperty
  @Queryable(dataType = GroupConstants.DATA_TYPE_GROUP_NAME, traits = Array(CASE_INSENSITIVE, NO_STEMMING))
  def getName: String

  def setName(name: String): Unit

  // TODO make optional for CLI only
  @JsonProperty
  @Queryable(dataType = GroupConstants.DATA_TYPE_GROUP_ID, traits = Array(CASE_INSENSITIVE))
  def getGroupId: String

  def setGroupId(groupId: String): Unit

  @JsonProperty
  @Queryable(dataType = GroupConstants.DATA_TYPE_GROUP_EXTERNAL_IDENTIFIER, traits = Array(CASE_INSENSITIVE))
  def getExternalId: java.util.Optional[String]

  def setExternalId(externalId: java.util.Optional[String]): Unit

  @JsonProperty
  @Queryable(dataType = DataTypes.DATA_TYPE_CREATE_TIME)
  def getCreateTime: Instant

  def setCreateTime(createTime: Instant): Unit

  /** Represents the type of group */
  @JsonProperty("groupType")
  @JsonView(Array(classOf[Default]))
  @Queryable(dataType = GroupConstants.DATA_TYPE_GROUP_TYPE, traits = Array(CASE_INSENSITIVE))
  def getGroupType: GroupConstants.GroupType

  def setGroupType(groupType: GroupConstants.GroupType): Unit

  def getInDirectory: java.lang.Boolean

  def setInDirectory(inDirectory: java.lang.Boolean): Unit

  @JsonProperty
  @Queryable(dataType = DataTypes.DATA_TYPE_DISABLED)
  def getDisabled: java.lang.Boolean

  def setDisabled(disabled: java.lang.Boolean): Unit

  def getUnavailable: java.lang.Boolean

  def setUnavailable(unavailable: java.lang.Boolean): Unit

  @JsonProperty("branch_id")
  @Queryable(dataType = GroupConstants.DATA_TYPE_GROUP_BRANCH)
  def getBranchId: java.util.Optional[java.lang.Long]

  @JsonProperty("project_id")
  @Queryable(dataType = GroupConstants.DATA_TYPE_GROUP_PROJECT)
  def getProjectId: java.util.Optional[java.lang.Long]

  // Jackson doesn't like that this has a differing parameter type than what getProjectId returns, thus setProject
  def setProject(id: java.lang.Long): Unit

  @JsonProperty def // ???
  getCommitId: java.util.Optional[java.lang.Long]

  def setCommitId(commitId: java.util.Optional[java.lang.Long]): Unit

  /** Get the current user's rights in the group. The group's parents are also inspected for rights.
    *
    * @return
    *   the current user's rights in the group
    */
  @RequestMapping(path = "rights", method = Method.GET)
  def getRights: java.util.Set[String]

  /** Group property collection. Provides an extension point for different group types.
    */
  @RequestMapping(path = "properties", method = Method.GET)
  def getProperties: JsonNode

  /** Set a group property. Provides an extension point for different group types.
    */
  def setProperty(propertyName: String, value: Object): Unit

  /** Fetch a group property. Provides an extension point for different group types.
    */
  def getProperty[T](propertyName: String, `type`: Class[T]): T
end GroupComponent

object GroupComponent:

  class Init(
    val name: String,
    val groupId: String,
    val groupType: GroupConstants.GroupType,
    val disabled: Boolean = false,
    val externalId: java.util.Optional[String] = java.util.Optional.empty()
  )
