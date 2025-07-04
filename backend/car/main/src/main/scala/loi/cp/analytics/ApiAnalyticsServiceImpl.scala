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

package loi.cp.analytics

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.session.SessionDTO
import loi.cp.analytics.event.{EnrollmentCreateEvent2, EnrollmentDeleteEvent1, EnrollmentUpdateEvent2}
import loi.cp.enrollment.EnrollmentComponent
import scaloi.misc.TimeSource

import java.util.UUID

@Service
class ApiAnalyticsServiceImpl(
  analyticsService: AnalyticsService,
  domainDto: => DomainDTO,
  sessionDto: => SessionDTO,
  timeSource: TimeSource,
) extends ApiAnalyticsService:

  override def emitEnrollmentCreateEvent(e: EnrollmentComponent): Unit =
    analyticsService.emitEvent(
      EnrollmentCreateEvent2(
        id = UUID.randomUUID(),
        session = sessionDto.id,
        source = domainDto.hostName,
        time = timeSource.date,
        enrollmentId = e.getId,
        user = analyticsService.userData(e.getUser.toDTO),
        sectionId = e.getContextId,
        role = e.getRole.getRoleId,
        disabled = e.isDisabled,
        startTime = Option(e.getStartTime).map(_.toInstant),
        endTime = Option(e.getStopTime).map(_.toInstant),
        dataSource = e.getDataSource,
      )
    )

  override def emitEnrollmentUpdateEvent(e: EnrollmentComponent): Unit =
    analyticsService.emitEvent(
      EnrollmentUpdateEvent2(
        id = UUID.randomUUID(),
        session = sessionDto.id,
        source = domainDto.hostName,
        time = timeSource.date,
        enrollmentId = e.getId,
        user = analyticsService.userData(e.getUser.toDTO),
        sectionId = e.getContextId,
        role = e.getRole.getRoleId,
        disabled = e.isDisabled,
        startTime = Option(e.getStartTime).map(_.toInstant),
        endTime = Option(e.getStopTime).map(_.toInstant),
        dataSource = e.getDataSource,
      )
    )

  override def emitEnrollmentDeleteEvent(enrollmentId: Long): Unit =
    analyticsService.emitEvent(
      EnrollmentDeleteEvent1(
        id = UUID.randomUUID(),
        session = sessionDto.id,
        source = domainDto.hostName,
        time = timeSource.date,
        enrollmentId = enrollmentId,
      )
    )
end ApiAnalyticsServiceImpl
