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

package loi.cp.accesscode

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.de.group.GroupComponent
import loi.cp.context.accesscode.EnrollAccessCodeBatch
import loi.cp.role.{RoleComponent, RoleService}

@Service
class CourseAccessCodeServiceImpl(implicit
  facadeService: FacadeService,
  roleService: RoleService
) extends CourseAccessCodeService:
  import CourseAccessCodeServiceImpl.*

  override def getAccessCode(course: GroupComponent): Option[AccessCodeComponent] =
    course.facade[AccessCodeParentFacade].findAccessCode

  override def getOrCreateAccessCode(course: GroupComponent): AccessCodeComponent =
    val parent = course.facade[AccessCodeParentFacade]
    parent.lock(true)
    parent.pollute() // prohibit query cache
    parent.findAccessCode getOrElse {
      parent
        .addBatch(new EnrollAccessCodeBatch.Init(course, studentRole))
        .generateAccessCode(CourseAccessCodePrefix)
    }

  override def removeAccessCode(course: GroupComponent): Unit =
    for accessCode <- getAccessCode(course)
    do
      accessCode.getBatch.delete()
      accessCode.delete()

  private def studentRole: RoleComponent =
    roleService.getRoleByRoleId(EnrollmentWebService.STUDENT_ROLE_ID)
end CourseAccessCodeServiceImpl

object CourseAccessCodeServiceImpl:
  final val CourseAccessCodePrefix = "CAC"
