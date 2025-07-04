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

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.domain.DomainDTO
import loi.cp.content.{ContentDateUtils, ContentDates, CourseContentService, CourseContents}
import loi.cp.course.CourseAccessService.CourseRights
import loi.cp.customisation.Customisation
import loi.cp.gatedate.GateStartDate
import loi.cp.group.LightweightGroupService

@Service
class CourseSectionServiceImpl(
  groupService: LightweightGroupService,
  courseContentService: CourseContentService,
  domain: => DomainDTO
) extends CourseSectionService:
  override def getCourseSection(sectionId: Long, rights: Option[CourseRights]): Option[CourseSection] =
    groupService
      .fetchSectionGroup(sectionId)
      .map(lwc =>
        val contents: CourseContents = courseContentService.getCourseContents(lwc, rights).get
        val dates: ContentDates      =
          lwc.startDate
            .map(date => ContentDateUtils.contentDates(contents.tree, date.atZone(domain.timeZoneId)))
            .getOrElse(ContentDates.empty)

        CourseSection.fromOldCode(lwc, contents, dates.gateDates, dates.dueDates)
      )

  override def getCourseSectionInternal(sectionId: Long, customisation: Customisation): Option[CourseSection] =
    groupService
      .fetchSectionGroup(sectionId)
      .map(lwc =>
        val allContents: CourseContents = courseContentService.getCourseContentsInternal(lwc, customisation).get

        val gateStartDate: Option[GateStartDate] =
          GateStartDate.forCourse(lwc.rollingEnrollment, lwc.startDate, lwc.createDate, domain.timeZoneId)
        val dates: ContentDates                  =
          gateStartDate
            .flatMap(_.value)
            .map(value => ContentDateUtils.contentDates(allContents.tree, value))
            .getOrElse(ContentDates.empty)

        CourseSection.fromOldCode(lwc, allContents, dates.gateDates, dates.dueDates)
      )
end CourseSectionServiceImpl
