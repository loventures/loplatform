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

package loi.cp.course.preview

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.cpxp.PK
import com.learningobjects.cpxp.scala.cpxp.PK.ops.*
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.user.{UserDTO, UserType}
import loi.cp.course.lightweight.Lwc
import loi.cp.role.RoleService
import loi.cp.user.{UserComponent, UserParentFacade}
import scaloi.syntax.any.*

import scala.jdk.OptionConverters.*

@Service
class PreviewService(
  user: => UserDTO
)(implicit
  fs: FacadeService,
  rs: RoleService,
  ews: EnrollmentWebService,
):

  def findPreviewer(course: Lwc, role: PreviewRole): Option[UserComponent] =
    user.facade[UserParentFacade].findUserByUsername(previewName(course, role)).toScala

  def getOrCreatePreviewer(course: Lwc, role: PreviewRole): UserComponent =
    user.facade[UserParentFacade].getOrCreateUserByUsername(previewName(course, role), userInit(course, role)) always {
      enrolPreviewer(course, role, _)
    }

  def enrolPreviewer[User: PK](course: Lwc, role: PreviewRole, previewer: User): Unit =
    val enrolmentRole = rs.getRoleByRoleId(role.roleId).getId
    ews.setSingleEnrollment(course.id, enrolmentRole, previewer.pk, PreviewService.DataSource)

  private def userInit(course: Lwc, role: PreviewRole): UserComponent.Init = new UserComponent.Init <| { init =>
    init.userName = previewName(course, role)
    init.givenName = user.givenName
    init.middleName = user.middleName
    init.familyName = role.entryName
    init.emailAddress = user.emailAddress
    init.userType = UserType.Preview
    init.url = null
  }

  private def previewName(course: Lwc, role: PreviewRole): String =
    s"${course.groupId}-${role.entryName}"
end PreviewService

object PreviewService:
  final val DataSource = "Preview"
