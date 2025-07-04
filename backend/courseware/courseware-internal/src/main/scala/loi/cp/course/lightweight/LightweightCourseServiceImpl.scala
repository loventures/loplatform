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
package lightweight

import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.group.GroupConstants.GroupType
import loi.cp.analytics.CoursewareAnalyticsService
import loi.cp.customisation.CourseCustomisationService
import loi.cp.gatedate.GateDateSchedulingService

@Service
class LightweightCourseServiceImpl(
  courseCustomisationService: CourseCustomisationService,
  coursewareAnalyticsService: CoursewareAnalyticsService,
  gateDateService: GateDateSchedulingService,
)(implicit
  componentService: ComponentService,
) extends LightweightCourseService:
  import LightweightCourseServiceImpl.*

  override def initializeSection(course: LightweightCourse, origin0: Option[LightweightCourse]): Unit =
    origin0 foreach { origin =>
      courseCustomisationService.copyCustomisation(course, origin)
      incrementGeneration(course)
    }
    gateDateService.scheduleGateDateEvents(course)

    if course.getGroupType == GroupType.CourseSection then
      if origin0.isEmpty then
        throw new RuntimeException(s"for ${course.id} to be a CourseSection it must use an offering")
      coursewareAnalyticsService.emitSectionCreateEvent(course, origin0.get)
  end initializeSection

  override def updateSection(course: LightweightCourse): Unit =
    incrementGeneration(course)

  override def incrementGeneration(lwc: Lwc): Unit =
    val course = lwc.component[LightweightCourse]
    course.getGeneration foreach { generation =>
      val newGeneration = generation + 1L
      logger info s"generation for course ${lwc.id} is now $newGeneration"
      course.setGeneration(newGeneration)
    }
end LightweightCourseServiceImpl

object LightweightCourseServiceImpl:
  private val logger = org.log4s.getLogger
