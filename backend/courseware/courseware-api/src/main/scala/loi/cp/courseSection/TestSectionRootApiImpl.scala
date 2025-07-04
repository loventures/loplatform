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

import cats.syntax.option.*
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.ErrorResponse
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.integration.IntegrationWebService
import com.learningobjects.cpxp.service.user.UserDTO
import loi.authoring.node.AssetNodeService
import loi.authoring.project.{CreateProjectDto, ProjectService, ProjectType}
import loi.authoring.workspace.service.ReadWorkspaceService
import loi.cp.analytics.CoursewareAnalyticsService
import loi.cp.course.lightweight.LightweightCourseService
import loi.cp.courseSection.CourseSectionRootApi.ITSection
import loi.cp.courseSection.SectionRootApi.{SectionDTO, SectionInit}
import loi.cp.gatedate.GateDateSchedulingService
import loi.cp.offering.ProjectOfferingService
import scalaz.\/

/** Test section root API implementation.
  */
@Component
class TestSectionRootApiImpl(val componentInstance: ComponentInstance)(implicit
  coursewareAnalyticsService: CoursewareAnalyticsService,
  fs: FacadeService,
  iws: IntegrationWebService,
  lwcs: LightweightCourseService,
  projectService: ProjectService,
  gateDateService: GateDateSchedulingService,
  nodeService: AssetNodeService,
  workspaceService: ReadWorkspaceService,
  projectOfferingService: ProjectOfferingService,
  user: () => UserDTO,
) extends SectionRootApiImpl("folder-testSections")
    with TestSectionRootApi
    with ComponentImplementation:

  // this creates a preview section (no linked offering), not a test section..
  override def emptySection(it: ITSection): ErrorResponse \/ SectionDTO =
    val dto = CreateProjectDto(projectName = it.name, projectType = ProjectType.Course, createdBy = user.id)
    for
      (branch, _) <- projectService.createProject(dto).toDisjunction.leftMap(_ => ErrorResponse.serverError)
      rws          = workspaceService.requireReadWorkspace(branch.id)
      course       = nodeService.load(rws).byName(rws.homeName).get
      ini          = SectionInit(
                       name = it.name,
                       groupId = it.groupId,
                       fjœr = Some(true),
                       project_id = long2Long(rws.projectInfo.id).some,
                       course_id = long2Long(course.info.id).some,
                       version_id = Some(branch.id),
                       externalId = Some(it.groupId),
                     )
      result      <- add(ini)
    yield result
    end for
  end emptySection
end TestSectionRootApiImpl
