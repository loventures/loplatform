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

import java.time.Instant
import java.util.{Date, Objects}

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.component.annotation.{Component, PostCreate}
import com.learningobjects.cpxp.component.{AbstractComponent, ComponentService}
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService
import com.learningobjects.cpxp.service.group.{GroupConstants, GroupFacade}
import com.learningobjects.cpxp.util.Ids
import com.learningobjects.de.enrollment.EnrollmentOwnerImpl
import loi.cp.right.RightService
import loi.cp.role.SupportedRoleService

import scala.jdk.CollectionConverters.*

@Component
class Group(
  override val componentService: ComponentService,
  override val enrollmentWebService: EnrollmentWebService,
  rightService: RightService,
  self: GroupFacade,
  override val supportedRoleService: SupportedRoleService,
) extends AbstractComponent
    with GroupComponent
    with EnrollmentOwnerImpl:

  @SuppressWarnings(Array("unused")) // lifecycle
  @PostCreate
  // the GroupComponent.Init is needed due to something like scala/scala#1409,
  // but not quite the same... and I can't track it down... but TODO
  def postCreate(init: GroupComponent.Init, prototype: GroupComponent): Unit =

    // TODO: Don't allow init to be null, get rid of prototype.
    if init != null then
      this.setExternalId(init.externalId)
      this.setName(init.name)
      this.setGroupId(init.groupId)
      this.setGroupType(init.groupType)
      this.setDisabled(init.disabled)
    else if prototype != null then
      this.setExternalId(prototype.getExternalId)
      this.setName(prototype.getName)
      this.setGroupId(prototype.getGroupId)
      this.setGroupType(prototype.getGroupType)
      this.setDisabled(false)
    else // I'm not sure whether this statement is reachable, but I want to make sure there
      //  is a default value for 'disabled'
      this.setGroupType(GroupConstants.GroupType.COURSE)
      this.setDisabled(false)
    end if
    self.bindUrl(this.getGroupId)
    this.setInDirectory(false)
    this.setUnavailable(false)
    this.setCreateTime(Instant.now)
  end postCreate

  override def getId: java.lang.Long = self.getId

  override def getExternalId: java.util.Optional[String] = self.getGroupExternalId

  override def setExternalId(externalId: java.util.Optional[String]): Unit = self.setGroupExternalId(externalId)

  override def getUrl: String = self.getUrl

  override def setUrl(url: String): Unit = if url != null then self.bindUrl(url)

  override def getName: String = self.getName

  override def setName(name: String): Unit = self.setName(name)

  override def getGroupId: String = self.getGroupId

  override def setGroupId(groupId: String): Unit = self.setGroupId(groupId)

  override def getGroupType: GroupConstants.GroupType = self.getGroupType

  override def setGroupType(groupType: GroupConstants.GroupType): Unit = self.setGroupType(groupType)

  override def getInDirectory: java.lang.Boolean = self.getInDirectory

  override def setInDirectory(inDirectory: java.lang.Boolean): Unit = self.setInDirectory(inDirectory)

  override def getDisabled: java.lang.Boolean = self.getDisabled

  override def setDisabled(disabled: java.lang.Boolean): Unit =
    val isDifferent = !Objects.equals(self.getDisabled, disabled)
    if isDifferent then self.setDisabled(disabled)

  override def setProject(projectId: java.lang.Long): Unit = self.setProjectId(projectId)

  override def getUnavailable: java.lang.Boolean = self.getUnavailable

  override def setUnavailable(unavailable: java.lang.Boolean): Unit = self.setUnavailable(unavailable)

  override def getBranchId: java.util.Optional[java.lang.Long] = java.util.Optional.ofNullable(self.getBranchId)

  override def getProjectId: java.util.Optional[java.lang.Long] = self.getProjectId

  override def getCommitId: java.util.Optional[java.lang.Long] = self.getCommitId

  override def setCommitId(commitId: java.util.Optional[java.lang.Long]): Unit = self.setCommitId(commitId)

  override def getCreateTime: Instant = Option(self.getCreateTime).map(_.toInstant).orNull

  override def setCreateTime(createTime: Instant): Unit = self.setCreateTime(Date.from(createTime))

  override def getRights: java.util.Set[String] =
    val context = Ids.of(self.getId)
    rightService
      .getUserRightsInPedigree(context)
      .asScala
      .map(clazz => if clazz == null then null else clazz.getName)
      .asJava

  override def getProperties: JsonNode = self.getProperties

  override def setProperty(propertyName: String, value: Object): Unit = self.setProperty(propertyName, value)

  override def getProperty[T](propertyName: String, `type`: Class[T]): T = self.getProperty(propertyName, `type`)
end Group
