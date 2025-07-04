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

import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.annotation.{Component, DefaultImplementation}
import com.learningobjects.cpxp.component.site.ItemSiteComponent
import com.learningobjects.cpxp.component.web.{HtmlResponse, WebResponse}
import com.learningobjects.cpxp.service.attachment.ImageFacade
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.de.group.Group
import loi.asset.course.model.Course
import loi.authoring.asset.Asset
import loi.authoring.branch.Branch
import loi.authoring.workspace.AttachedReadWorkspace
import loi.cp.config.ConfigurationService
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.right.{Right, RightService}
import loi.cp.role.SupportedRoleService

import java.{lang, util}
import scala.jdk.CollectionConverters.*

@DefaultImplementation
@Component
class UnknownCourse(
  val self: CourseFacade,
  componentService: ComponentService,
  val configurationService: ConfigurationService,
  enrollmentWebService: EnrollmentWebService,
  rightService: RightService,
  supportedRoleService: SupportedRoleService
) extends Group(componentService, enrollmentWebService, rightService, self, supportedRoleService)
    with CourseComponent
    with CourseDatesImpl
    with ItemSiteComponent:
  override def id: Long = self.getId

  override def getLogo: ImageFacade = self.getLogo

  override def getSubtenantId: lang.Long = self.getSubtenant

  override def setSubtenant(subtenantId: lang.Long): Unit = ???

  override def getPreferences: CoursePreferences = ???

  override def isArchived: lang.Boolean = false

  override def setArchived(archived: lang.Boolean): Unit = ???

  override def getUserRights: util.Set[Class[? <: Right]] = Set.empty[Class[? <: Right]].asJava

  override def isRestricted: Boolean = false

  override def renderSite(view: String): WebResponse = HtmlResponse(this, "unknownCourse.html")

  override def delete(): Unit = self.delete()

  override def loadCourse(): Asset[Course]            = ???
  override def setCourseId(id: Long): Unit            = ???
  override def getGeneration: Option[Long]            = ???
  override def setGeneration(generation: Long): Unit  = ???
  override def getOffering: LightweightCourse         = ???
  override def getOfferingId: Option[Long]            = ???
  override def externalId: Option[String]             = ???
  override def groupId: String                        = ???
  override def getCreatedBy: Option[UserDTO]          = ???
  override def getWorkspace: AttachedReadWorkspace    = ???
  override def isSelfStudy: Boolean                   = ???
  override def loadBranch(): Branch                   = ???
  override def commitId: Long                         = ???
  override def setCommitId(id: Long): Unit            = ???
  override def setSelfStudy(selfStudy: Boolean): Unit = ???
end UnknownCourse
