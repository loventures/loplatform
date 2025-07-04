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

package loi.cp.role

import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService.*
import com.learningobjects.cpxp.service.relationship.RelationshipWebService
import com.learningobjects.cpxp.startup.StartupTaskScope.Domain
import com.learningobjects.cpxp.startup.{StartupTask, StartupTaskBinding}
import loi.cp.admin.right.*
import loi.cp.announcement.AnnouncementAdminRight
import loi.cp.course.right.ManageLibrariesReadRight
import loi.cp.lti.right.ConfigureLtiCourseRight
import loi.cp.overlord.{OverlordRight, SupportRight, UnderlordRight}

import scala.jdk.CollectionConverters.*

@StartupTaskBinding(version = 20181025, taskScope = Domain)
class RoleStartupTask(
  override val rs: RoleService,
  override val domain: DomainDTO,
  override val rws: RelationshipWebService
) extends StartupTask
    with RoleStartupOps:

  override def run(): Unit =

    putRoles(ROLE_IDS.asScala.toSeq*)

    putSupportedRolesInDomain(
      SupportedRoleInfo(ROLE_ADMINISTRATOR_NAME, List(classOf[AdminRight], classOf[ConfigureLtiCourseRight])),
      SupportedRoleInfo(ROLE_FACULTY_NAME, List(classOf[ConfigureLtiCourseRight])),
      SupportedRoleInfo(ROLE_STUDENT_NAME),
      SupportedRoleInfo(ROLE_STAFF_NAME),
      SupportedRoleInfo(
        ROLE_HOSTING_ADMIN_NAME,
        List(classOf[HostingAdminRight], classOf[OverlordRight], classOf[ConfigureLtiCourseRight])
      ),
      SupportedRoleInfo(
        ROLE_HOSTING_STAFF_NAME,
        List(classOf[AdminRight], classOf[UnderlordRight], classOf[ConfigureLtiCourseRight])
      ),
      SupportedRoleInfo(
        ROLE_HOSTING_SUPPORT_NAME,
        List(
          classOf[AdminRight],
          classOf[SupportRight],
          classOf[ConfigureLtiCourseRight],
          classOf[AnnouncementAdminRight],
          classOf[ConfigurationAdminRight]
        )
      ),
      SupportedRoleInfo(
        ROLE_PROVISIONING_ADMIN_NAME,
        List(classOf[CourseAdminRight], classOf[ProjectAdminRight], classOf[ConfigureLtiCourseRight]),
        List(classOf[ManageLibrariesReadRight])
      ),
    )

    // delete cruft that someone left behind..
    rs.getSupportedRoles(domain).asScala.filter(_.getRoleId eq null) foreach { sr =>
      sr.delete()
    }
  end run
end RoleStartupTask
