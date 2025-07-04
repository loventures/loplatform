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

package loi.cp.course

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.util.ConfigUtils
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.group.GroupConstants.GroupType
import loi.authoring.project.ProjectService
import loi.cp.config.ConfigurationService.{ConfigDetail, SetResult}
import loi.cp.config.{ConfigurationKey, ConfigurationService}
import loi.cp.context.ContextId
import scalaz.syntax.either.*
import scaloi.syntax.disjunction.*

@Service
class CourseConfigurationServiceImpl(
  configurationService: ConfigurationService,
  projectService: ProjectService
)(implicit componentService: ComponentService)
    extends CourseConfigurationService:

  override def getGroupDetail[A](key: ConfigurationKey[A], group: CourseComponent): ConfigDetail[A] =
    lazy val offeringId = group.getOfferingId.get
    lazy val projectId  = group.getProjectId.get
    getGroupDetail(key, group.getId, group.getGroupType, offeringId, projectId)

  override def getGroupDetail[A](key: ConfigurationKey[A], group: CourseSection): ConfigDetail[A] =
    lazy val offeringId = group.offeringId
    lazy val projectId  = group.branch.requireProject.id
    getGroupDetail(key, group.id, group.groupType, offeringId, projectId)

  override def getGroupDetail[A](key: ConfigurationKey[A], group: ContextId): ConfigDetail[A] =
    val groupAgain = group.component[CourseComponent]
    getGroupDetail(key, groupAgain)

  private def getGroupDetail[A](
    key: ConfigurationKey[A],
    groupId: Long,
    groupType: GroupType,
    offeringId: => Long,
    projectId: => Long
  ): ConfigDetail[A] =

    lazy val domainJson   = configurationService.getDomainJson(key) // is overlord + domain
    lazy val offeringJson = configurationService.getRawItemJson(key)(() => offeringId)
    lazy val projectJson  = projectService.getRawConfigJson(key, projectId)
    lazy val groupJson    = configurationService.getRawItemJson(key)(() => groupId)

    groupType match

      case GroupType.CourseSection =>
        // overlord + domain + offering + group
        val parentJson = ConfigUtils.applyDefaults(offeringJson, domainJson)
        val valueJson  = ConfigUtils.applyDefaults(groupJson, parentJson)
        ConfigDetail(key, valueJson, parentJson, groupJson)

      case GroupType.PreviewSection =>
        // overlord + domain + project + group
        val parentJson = ConfigUtils.applyDefaults(projectJson, domainJson)
        val valueJson  = ConfigUtils.applyDefaults(groupJson, parentJson)
        ConfigDetail(key, valueJson, parentJson, groupJson)

      case _ => // CourseOffering and TestSection importantly and all others un-importantly
        // overlord + domain + section (the ordinary chain of config)
        configurationService.getItemDetail(key)(() => groupId)
    end match
  end getGroupDetail

  override def getProjectDetail[A](key: ConfigurationKey[A], projectId: Long): ConfigDetail[A] =
    val parentJson  = configurationService.getDomainJson(key) // is overlord + domain
    val projectJson = projectService.getRawConfigJson(key, projectId)
    val valueJson   = ConfigUtils.applyDefaults(projectJson, parentJson)
    ConfigDetail(key, valueJson, parentJson, projectJson)

  override def setProjectConfig[A](
    key: ConfigurationKey[A],
    projectId: Long,
    config: Option[JsonNode]
  ): SetResult[A] =

    val parentJson = configurationService.getDomainJson(key)

    config match
      case None              =>
        projectService.removeRawConfigJson(key, projectId)
        configurationService.validate(key, JsonNodeFactory.instance.objectNode(), parentJson)
      case Some(projectJson) =>
        configurationService
          .validate(key, projectJson, parentJson)
          .rightTap(_ => projectService.setRawConfigJson(key, projectId, projectJson))
  end setProjectConfig

  override def setGroupConfig[A](
    key: ConfigurationKey[A],
    group: CourseComponent,
    config: Option[JsonNode]
  ): SetResult[A] =

    config match
      case None            =>
        configurationService.clearItem(key)(group)
        getGroupDetail(key, group).value.right
      case Some(groupJson) =>
        val parentJson = group.getGroupType match
          case GroupType.CourseSection  =>
            val domainJson   = configurationService.getDomainJson(key)
            val offeringJson = configurationService.getRawItemJson(key)(group.getOffering)
            ConfigUtils.applyDefaults(offeringJson, domainJson)
          case GroupType.PreviewSection =>
            getProjectDetail(key, group.getProjectId.get).valueJson
          case _                        =>
            configurationService.getDomainJson(key)
        configurationService.setJson(key, group, config, parentJson)

  override def patchGroupConfig[A](key: ConfigurationKey[A], group: CourseComponent, patch: JsonNode): SetResult[A] =
    val current = getGroupDetail(key, group)
    val patched = ConfigUtils.applyDefaults(patch, current.overrides)
    setGroupConfig(key, group, Some(patched))

  override def copyConfig[A](key: ConfigurationKey[A], srcProjectId: Long, tgtGroup: CourseComponent): SetResult[A] =
    val projectJson = projectService.getRawConfigJson(key, srcProjectId)
    setGroupConfig(key, tgtGroup, Option(projectJson))
end CourseConfigurationServiceImpl
