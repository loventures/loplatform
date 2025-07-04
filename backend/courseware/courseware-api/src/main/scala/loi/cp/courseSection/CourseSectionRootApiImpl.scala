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

package loi.cp.courseSection

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.integration.IntegrationWebService
import com.learningobjects.cpxp.service.user.UserDTO
import loi.authoring.node.AssetNodeService
import loi.authoring.project.ProjectService
import loi.authoring.workspace.service.ReadWorkspaceService
import loi.cp.analytics.CoursewareAnalyticsService
import loi.cp.config.ConfigurationService
import loi.cp.course.CoursePreferences
import loi.cp.course.lightweight.LightweightCourseService
import loi.cp.gatedate.GateDateSchedulingService
import loi.cp.offering.ProjectOfferingService

/** Course section root API implementation.
  */
@Component
class CourseSectionRootApiImpl(val componentInstance: ComponentInstance)(implicit
  coursewareAnalyticsService: CoursewareAnalyticsService,
  cs: ConfigurationService,
  fs: FacadeService,
  iws: IntegrationWebService,
  lwcs: LightweightCourseService,
  projectService: ProjectService,
  gateDateService: GateDateSchedulingService,
  nodeService: AssetNodeService,
  workspaceService: ReadWorkspaceService,
  projectOfferingService: ProjectOfferingService,
  user: () => UserDTO,
) extends SectionRootApiImpl("folder-courses")
    with CourseSectionRootApi
    with ComponentImplementation:
  import loi.cp.courseSection.CourseSectionRootApi.*

  override def configuration: CourseConfiguration =
    val prefs = CoursePreferences.getDomain
    CourseConfiguration(prefs.reviewPeriodOffset, prefs.betaSelfStudyCourses)
end CourseSectionRootApiImpl
