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

package loi.cp.discussion.user

import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService.EnrollmentType
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.user.{UserDTO, UserId}
import com.learningobjects.cpxp.util.Ids
import loi.cp.admin.right.CourseAdminRight
import loi.cp.context.ContextId
import loi.cp.course.right.TeachCourseRight
import loi.cp.right.{Right, RightService}
import scaloi.syntax.set.*

import scala.jdk.CollectionConverters.*

/** Service intended to grab users and rights about a user associated with a discussion board.
  */
@Service
trait DiscussionUserRightsService:

  def userHasModeratorRight(userId: UserId, course: ContextId): Boolean

  def courseReviewers(course: ContextId): Seq[UserDTO]

@Service
class DiscussionUserRightsServiceComponentBacked(
  rightService: RightService,
  enrollmentWebService: EnrollmentWebService
)(implicit fs: FacadeService, cs: ComponentService)
    extends DiscussionUserRightsService:

  override def userHasModeratorRight(userId: UserId, course: ContextId): Boolean =
    val moderatorRights: Set[Class[? <: Right]] = Set(classOf[TeachCourseRight], classOf[CourseAdminRight])
    moderatorRights `intersects` rightService.getUserRightsInPedigree(course).asScala

  override def courseReviewers(course: ContextId): Seq[UserDTO] =
    val enrollments = enrollmentWebService
      .getGroupEnrollments(course.id, EnrollmentType.ACTIVE_ONLY)
      .asScala
      .toSeq

    // Number of roles per course is small, so identifying reviewers based on this is cheaper
    val courseRoles   = enrollments.map(_.getRole).distinct
    val reviewerRoles = courseRoles
      .filter { role =>
        val roleRights = rightService.getRoleRights(Ids.of(course.id), role)
        roleRights.contains(classOf[TeachCourseRight])
      }
      .map(_.getId)

    enrollments
      .filter(e => reviewerRoles.contains(e.getRoleId))
      .map(enrollment => UserDTO(enrollment.getUser))
  end courseReviewers
end DiscussionUserRightsServiceComponentBacked
