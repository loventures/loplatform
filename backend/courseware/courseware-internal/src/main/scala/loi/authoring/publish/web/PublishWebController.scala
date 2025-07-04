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

package loi.authoring.publish.web

import com.google.common.net.MediaType
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, FileResponse, Method, WebRequest}
import com.learningobjects.cpxp.component.{BaseComponent, ComponentInstance}
import com.learningobjects.cpxp.web.ExportFile
import com.learningobjects.de.authorization.Secured
import loi.authoring.project.{AccessRestriction, ProjectService}
import loi.authoring.security.right.{AccessAuthoringAppRight, DeleteAnyProjectRight, PublishOfferingRight}
import loi.authoring.web.AuthoringApiWebUtils
import loi.cp.offering.ProjectOfferingService
import scaloi.syntax.localDateTime.*

import java.util.Date
import scala.util.Try

@Component
@Controller(root = true, value = "publish-web-controller")
class PublishWebController(
  ci: ComponentInstance,
  projectService: ProjectService,
  projectOfferingService: ProjectOfferingService,
  webUtils: AuthoringApiWebUtils,
) extends BaseComponent(ci)
    with ApiRootComponent:

  @Secured(Array(classOf[PublishOfferingRight]))
  @RequestMapping(path = "authoring/projects/{projectId}/publishAnalysis", method = Method.GET)
  def getPublishAnalysis(@PathVariable("projectId") projectId: Long): PublishAnalysisWeb =
    val project  = webUtils.projectOrThrow404(projectId, AccessRestriction.projectMemberOr[PublishOfferingRight])
    val analysis = projectOfferingService.analyzePublish(project)
    PublishAnalysisWeb.from(analysis)

  @Secured(Array(classOf[PublishOfferingRight]))
  @RequestMapping(path = "authoring/projects/{projectId}/publishAnalysis/detail", method = Method.GET)
  def getPublishAnalysisCsv(@PathVariable("projectId") projectId: Long, request: WebRequest): FileResponse[?] =
    val project  = webUtils.projectOrThrow404(projectId, AccessRestriction.projectMemberOr[PublishOfferingRight])
    val analysis = projectOfferingService.analyzePublish(project)

    val csvFile = ExportFile.create(
      s"${project.code.getOrElse(project.name)} - LIS Result Line Item Changes.csv",
      MediaType.CSV_UTF_8,
      request
    )

    analysis.writeCsv(csvFile.file)
    FileResponse(csvFile.toFileInfo)
  end getPublishAnalysisCsv

  @Secured(Array(classOf[PublishOfferingRight]))
  @RequestMapping(path = "authoring/projects/{projectId}/publish", method = Method.POST)
  def publishProject(
    @PathVariable("projectId") projectId: Long,
  ): Unit =
    val project = webUtils.projectOrThrow404(projectId, AccessRestriction.projectMemberOr[PublishOfferingRight])
    projectOfferingService.publishProject(project)

  /** Sets the commits of all the offerings for the branch to the head of the branch.
    * @return
    *   the number of updated sections (sections of the offerings)
    */
  @Secured(Array(classOf[PublishOfferingRight]))
  @RequestMapping(path = "authoring/projects/{projectId}/update", method = Method.POST)
  def updateSections(
    @PathVariable("projectId") projectId: Long
  ): Int =
    val project = webUtils.projectOrThrow404(projectId, AccessRestriction.projectMemberOr[PublishOfferingRight])

    val updated = projectOfferingService.updateProject(project, None)

    updated
  end updateSections

  @RequestMapping(path = "authoring/branches/{branchId}/offering", method = Method.GET)
  def getOffering(@PathVariable("branchId") branchId: Long): Option[OfferingDto] =
    val branch = webUtils.branchOrFakeBranchOrThrow404(branchId)
    webUtils.throw403ForNonProjectUserWithout[PublishOfferingRight](branch.requireProject)
    for
      course <- projectOfferingService.getOfferingComponentForBranch(branch)
      ws      = webUtils.workspaceAtCommitOrThrow404(branch.id, course.commitId)
    yield OfferingDto(course.id, branch.id, ws.commitId, ws.created.asDate)

  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/projects/{id}", method = Method.DELETE)
  def deleteProject(@PathVariable("id") id: Long): Try[String] =
    val project = webUtils.projectOrThrow404(id, AccessRestriction.projectOwnerOr[DeleteAnyProjectRight])
    projectOfferingService.deleteProject(project)
    projectService.deleteProject(project)
end PublishWebController

final case class OfferingDto(
  id: Long,
  branchId: Long,
  commitId: Long,
  commitTime: Date,
)
