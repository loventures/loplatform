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

package loi.cp.courseSection

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.group.GroupConstants.GroupType
import com.learningobjects.cpxp.service.user.{UserDTO, UserWebService}
import loi.authoring.project.ProjectService
import loi.cp.bootstrap.Bootstrap
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.course.{CourseComponent, CourseFolderFacade}
import loi.cp.offering.ProjectOfferingService
import loi.cp.role.RoleService
import scalaz.syntax.either.*

import java.util.Optional

@Component
class SectionBootstrap(implicit
  fs: FacadeService,
  projectService: ProjectService,
  projectOfferingService: ProjectOfferingService,
  uws: UserWebService,
  ews: EnrollmentWebService,
  rs: RoleService,
  user: UserDTO,
):

  @Bootstrap("section.create")
  def createSection(dto: SectionBootstrapDto): CourseComponent =
    val folder   = sectionFolder
    val project  = projectService.loadProjectByName(dto.projectName, false).get
    val branch   = projectService.loadMasterBranch(project)
    val offering = projectOfferingService.getOfferingComponentForBranch(branch).get.asInstanceOf[LightweightCourse]
    val init     = new CourseComponent.Init(
      name = dto.name,
      groupId = dto.groupId,
      groupType = GroupType.CourseSection,
      createdBy = user,
      source = offering.left,
      startDate = Optional.empty,
      endDate = Optional.empty,
      shutdownDate = Optional.empty,
    )
    folder.addCourse(LightweightCourse.Identifier, init)
  end createSection

  @Bootstrap("user.enrol")
  def enrolUser(dto: EnrolBootstrapDto): Long =
    val folder  = sectionFolder
    val section = folder.findCourseByGroupId(dto.groupId).get
    val user    = uws.getUserByUserName(dto.userName)
    val role    = rs.getRoleByRoleId(dto.role)
    ews.setSingleEnrollment(section.id, role.getId, user.getId, "Bootstrap")

  private def sectionFolder = "folder-courses".facade[CourseFolderFacade]
end SectionBootstrap

final case class SectionBootstrapDto(
  groupId: String,
  name: String,
  projectName: String,
)

final case class EnrolBootstrapDto(
  userName: String,
  groupId: String,
  role: String,
)
