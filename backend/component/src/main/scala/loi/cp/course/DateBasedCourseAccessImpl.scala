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

import java.time.Instant
import java.util.Date
import java.util as ju

import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.UserException
import com.learningobjects.cpxp.component.util.ComponentUtils
import com.learningobjects.cpxp.component.web.{ErrorResponse, WebResponse}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.util.InternationalizationUtils as i18n
import loi.cp.course.right.InteractCourseRight
import loi.cp.right.*
import scalaz.syntax.std.option.*

import scala.jdk.CollectionConverters.*
import scala.util.control.NoStackTrace

private[course] trait DateBasedCourseAccessImpl extends RoleRightsProvider:
  course: CourseComponent =>

  import DateBasedCourseAccessImpl.*

  /** A horrid horrid hack.
    *
    * We use `@Secured` everywhere to restrict access based on a user's rights in a course, which is fine. However, once
    * a course has ended, we need to remove a student's ability to interact with the course. We do this by magically
    * removing their [[InteractCourseRight]], because that's totally what you'd expect to happen.
    *
    * The correct way to go about this would be to use [[CourseAccessService]] or something similar and check explicitly
    * in the body of the various web controller methods, but why...
    */
  override def getRoleRights(context: Id, role: Id): ju.Set[Class[? <: Right]] =
    val baseRights = component[DefaultRoleRightsProvider]
      .getRoleRights(context, role)
      .asScala
    (baseRights &~ {
      if hasCourseEnded then Set(classOf[InteractCourseRight]) else Set.empty
    }).asJava

  /** Check if dates would preclude access to this course.
    *
    * Access is precluded if the course has not started or has shut down.
    *
    * This method has no idea about roles; all are equal in preclusability.
    */
  protected def checkDateBasedAccess: Option[WebResponse] =
    def cd                          = getComponentInstance.getComponent
    def onDateAtTime(when: Instant) =
      ComponentUtils.formatMessage(OnDateAtTimeMessage, cd, Date `from` when)

    val notStarted = getStartDate match
      case Some(sd) if !hasCourseStarted =>
        val title = i18n.formatMessage(CourseNotStartedTitle)
        val msg   = i18n.formatMessage(CourseNotStartedMessage, onDateAtTime(sd))
        ErrorResponse.forbidden(new UserException(title, msg) with NoStackTrace).some
      case _                             => None
    val shutdown   = getShutdownDate match
      case Some(sd) if hasCourseShutdown =>
        val title = i18n.formatMessage(CourseShutdownTitle)
        val msg   = i18n.formatMessage(CourseShutdownMessage, onDateAtTime(sd))
        ErrorResponse.forbidden(new UserException(title, msg) with NoStackTrace).some
      case _                             => None

    notStarted orElse shutdown
  end checkDateBasedAccess
end DateBasedCourseAccessImpl

private[course] object DateBasedCourseAccessImpl:
  final val CourseNotStartedTitle   = "COURSE_NOT_STARTED_TITLE"
  final val CourseNotStartedMessage = "COURSE_NOT_STARTED_MESSAGE"
  final val CourseShutdownTitle     = "COURSE_EXPIRED_TITLE"
  final val CourseShutdownMessage   = "COURSE_EXPIRED_MESSAGE"
  final val OnDateAtTimeMessage     = "msg_onDateAtTime"
