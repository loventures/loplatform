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

package loi.cp.user

import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.user.{UserDTO, UserId}
import loi.cp.context.ContextId
import loi.cp.course.{CourseAccessService, CourseComponent}
import scalaz.\/
import scalaz.syntax.either.*

@Service
class ImpersonationServiceImpl(
  courseAccessService: CourseAccessService,
  currentUser: => UserDTO,
)(implicit env: ComponentService)
    extends ImpersonationService:

  override def checkImpersonation(
    courseId: ContextId,
    targetUser: UserId,
  ): ImpersonationError \/ Unit =
    val course = courseId.component[CourseComponent]

    val isSuperUser: Boolean = (
      courseAccessService.hasAdvisorAccess(course.contextId)
        || courseAccessService.hasInstructorAccess(course.contextId)
    )

    if targetUser.id == currentUser.id then
      // Asking for yourself (i.e. not impersonating)
      ().right
    else if isSuperUser then
      // A super user is viewing as another user
      ().right
    else
      // A non-super user is attempting to view as another user
      ImpersonationError.NotSuperUser().left
  end checkImpersonation
end ImpersonationServiceImpl
