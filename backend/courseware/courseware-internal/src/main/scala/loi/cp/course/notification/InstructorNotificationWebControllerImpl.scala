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

package loi.cp.course.notification
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.{ArgoBody, ErrorResponse}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.content.ContentAccessService
import loi.cp.course.CourseEnrollmentService
import loi.cp.notification.NotificationService
import scaloi.misc.TimeSource
import scaloi.syntax.BooleanOps.*
import scaloi.syntax.TryOps.*

import scalaz.\/

@Component
class InstructorNotificationWebControllerImpl(implicit
  val componentService: ComponentService,
  val componentInstance: ComponentInstance,
  notificationService: NotificationService,
  courseEnrollmentService: CourseEnrollmentService,
  contentAccessService: ContentAccessService,
  timeSource: TimeSource,
  currentUser: UserDTO
) extends ComponentImplementation
    with InstructorNotificationWebController:
  override def notify(request0: ArgoBody[InstructorNotificationRequest], context: Long): \/[ErrorResponse, Unit] =
    for
      request <- request0.decode_! \/>| ErrorResponse.badRequest
      course  <- contentAccessService.getCourseAsInstructor(context, currentUser) \/>| ErrorResponse.forbidden
      _       <- courseEnrollmentService.areAllStudentsEnrolled(
                   course.id,
                   request.notifiedUserIds.toSet
                 ) \/> ErrorResponse.badRequest
    yield
      val init: InstructorNotification.Init = InstructorNotification.Init(
        request.edgePath,
        course.contextId,
        timeSource.instant,
        currentUser.id,
        request.message,
        request.notifiedUserIds,
        request.urgency
      )
      notificationService.notify(course, classOf[InstructorNotification], init)
end InstructorNotificationWebControllerImpl
