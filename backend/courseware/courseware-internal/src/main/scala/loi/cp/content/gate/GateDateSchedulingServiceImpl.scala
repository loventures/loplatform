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

package loi.cp.content
package gate

import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.enrollment.EnrollmentFacade
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService.EnrollmentType
import com.learningobjects.cpxp.service.user.UserId
import com.learningobjects.cpxp.util.Ids
import loi.cp.appevent.AppEventService
import loi.cp.course.lightweight.Lwc
import loi.cp.course.{CourseSection, CourseSectionService}
import loi.cp.enrollment.EnrollmentService
import loi.cp.gatedate.*
import scalaz.syntax.std.option.*
import scaloi.misc.TimeSource

import java.time.ZonedDateTime
import java.util.Date

@Service
class GateDateSchedulingServiceImpl(
  appEventService: AppEventService,
  courseSectionService: CourseSectionService,
  domain: () => DomainDTO,
  enrollmentService: EnrollmentService,
  ts: TimeSource
) extends GateDateSchedulingService:
  import GateDateSchedulingServiceImpl.*

  override def scheduleGateDateEvents(course: Lwc): Unit =
    // TODO: Get rid of this horrible duplicative content loading
    courseSectionService
      .getCourseSection(course.id)
      .foreach(section =>
        section.startDate
          .foreach(startDate => scheduleGateDateEvent(section, startDate.atZone(domain.timeZoneId), None)) // @<<@
      )

  override def scheduleGateDateEvents(course: Lwc, user: UserId): Unit =
    // TODO: Get rid of this horrible duplicative content loading
    courseSectionService
      .getCourseSection(course.id)
      .foreach(section =>
        if course.rollingEnrollment && isStudent(course, user) then
          section
            .startDateForUser(userEnrolments(course, user))
            .fold(deleteGateDateEvent(Ids.of(user.id))) { start =>
              scheduleGateDateEvent(section, start.atZone(domain.timeZoneId), Some(user)) // @<<@
            }
      )

  private def isStudent(course: Lwc, user: UserId): Boolean =
    userEnrolments(course, user).exists(e => studentLikeRoles.contains(e.getRole.getRoleId))

  private def scheduleGateDateEvent(section: CourseSection, start: ZonedDateTime, user: Option[UserId]): Unit =
    val gateDates = ContentDateUtils.contentDates(section.contents.tree, start).gateDates
    val parent    = user.cata(u => Ids.of(u.id), section)
    // TODO: it would be nicer to reschedule the event than delete it
    deleteGateDateEvent(parent)
    ContentDateUtils.nextDate(gateDates, ts.instant) foreach { instant =>
      appEventService.scheduleEvent(Date.from(instant), parent, null, GateDateEvent(section.id, None))
    }

  private def deleteGateDateEvent(parent: Id): Unit =
    appEventService.deleteEvents(parent, null, classOf[GateDateEvent])

  // A user start date could be in the future so we have to look at all enrollments.
  private def userEnrolments(course: Lwc, user: UserId): List[EnrollmentFacade] =
    enrollmentService.loadEnrollments(user.id, course.id, EnrollmentType.ALL).filterNot(_.getDisabled)
end GateDateSchedulingServiceImpl

object GateDateSchedulingServiceImpl:

  /** Student like role identifiers. TODO: Dedup with LtiEnrolmentService. */
  private final val studentLikeRoles = Set("student", "trialLearner")
