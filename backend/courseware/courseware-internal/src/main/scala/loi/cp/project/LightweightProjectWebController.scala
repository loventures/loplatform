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

package loi.cp.project

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.{ApiFilter, ApiQuery, ApiQueryResults, ApiQueryUtils}
import com.learningobjects.cpxp.component.web.ErrorResponse.notFound
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ErrorResponse, Method}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.de.web.Queryable
import com.learningobjects.de.web.Queryable.Trait
import loi.asset.course.model.Course
import loi.authoring.asset.Asset
import loi.authoring.branch.Branch
import loi.authoring.node.AssetNodeService
import loi.authoring.project.{AccessRestriction, Project, ProjectService, ProjectType}
import loi.authoring.web.AuthoringApiWebUtils
import loi.authoring.workspace.service.ReadWorkspaceService
import loi.cp.offering.ProjectOfferingService
import scalaz.\/
import scalaz.std.option.*
import scalaz.syntax.either.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scaloi.syntax.boolean.*

import java.util.UUID
import scala.annotation.meta.getter
import scala.compat.java8.OptionConverters.*

/** Project web API for admin pages. */
@Controller(value = "lwc-prj", root = true)
trait LightweightProjectWebController extends ApiRootComponent:
  import LightweightProjectWebController.*

  @RequestMapping(method = Method.GET, path = "lwc/projects")
  def getProjects(query: ApiQuery): ErrorResponse \/ ApiQueryResults[ProjectInfo]

  @RequestMapping(method = Method.GET, path = "lwc/projects/{id}")
  def getProject(@PathVariable("id") id: Long): ErrorResponse \/ ProjectInfo

  @RequestMapping(method = Method.GET, path = "lwc/projects/{projectId}/course")
  def getProjectCourse(@PathVariable("projectId") projectId: Long): CourseInfo
end LightweightProjectWebController

@Component
class LightweightProjectWebControllerImpl(
  val componentInstance: ComponentInstance,
  authoringWebUtils: AuthoringApiWebUtils
)(implicit
  projectService: ProjectService,
  nodeService: AssetNodeService,
  workspaceService: ReadWorkspaceService,
  projectOfferingService: ProjectOfferingService,
) extends LightweightProjectWebController
    with ComponentImplementation:

  import LightweightProjectWebController.*

  override def getProjects(aq0: ApiQuery): ErrorResponse \/ ApiQueryResults[ProjectInfo] =
    def toProjectInfo(branch: Branch) = ProjectInfo(branch.requireProject, branch)
    for (aq1, offeredOnly) <- extractFilter(aq0, "offered")
    yield
      // Well this is terrible..
      val allBranches = projectService.loadProjectsAsMasterBranches(notArchived = true)
      val branches    = if offeredOnly then offeredBranches(allBranches) else allBranches
      ApiQueryUtils.query(branches.map(toProjectInfo), ApiQueryUtils.propertyMap[ProjectInfo](aq1))

  override def getProject(id: Long): ErrorResponse \/ ProjectInfo = for branch <-
      projectService.loadProjectAsMasterBranch(id, AccessRestriction.none) \/> notFound(s"project $id")
  yield ProjectInfo(branch.requireProject, branch)

  override def getProjectCourse(projectId: Long): CourseInfo =
    val branch = authoringWebUtils.masterOrFakeBranchOrThrow404(projectId, AccessRestriction.none)
    val ws     = workspaceService.requireReadWorkspace(branch.id, AccessRestriction.none)
    val course = nodeService.loadA[Course](ws).byName(ws.homeName).get // let it 500

    CourseInfo(course)

  private def offeredBranches(branches: Seq[Branch]): Seq[Branch] =
    val offered = projectOfferingService.offeredBranches(branches)
    branches.filter(branch => offered.contains(branch.id))

  private def extractFilter(query: ApiQuery, name: String): ErrorResponse \/ (ApiQuery, Boolean) =
    for
      builder <- new ApiQuery.Builder(query).right
      filter  <- builder.removePrefilter(name).asScala.traverseU(validateEmpty)
    yield (builder.build, filter.isDefined)

  private def validateEmpty(filter: ApiFilter): ErrorResponse \/ Unit =
    ((filter.getOperator eq null) && (filter.getValue == "")) \/> ErrorResponse
      .validationError("prefilter", filter)("Invalid filter")
end LightweightProjectWebControllerImpl

object LightweightProjectWebController:
  type Q = Queryable @getter

  final case class ProjectInfo(
    @Q id: Long,
    @Q(traits = Array(Trait.CASE_INSENSITIVE)) name: String,
    @Q `type`: String,
    @Q(traits = Array(Trait.CASE_INSENSITIVE)) displayString: String,
    branchId: Long,
  )

  object ProjectInfo:
    def apply(p: Project, b: Branch): ProjectInfo =
      ProjectInfo(p.id, p.name, ProjectType.Course.entryName, enhancedProjectName(p), b.id)

  /** Adds code/type prefix as appropriate. */
  def enhancedProjectName(p: Project): String =
    val prefix = (p.code, p.productType) match
      case (Some(code), Some(productType)) if !p.name.contains(code) && !p.name.contains(productType) =>
        s"$code $productType: "
      case (Some(code), _) if !p.name.contains(code)                                                  =>
        s"$code: "
      case (_, Some(productType)) if !p.name.contains(productType)                                    =>
        s"$productType: "
      case _                                                                                          =>
        ""

    s"$prefix${p.name}"
  end enhancedProjectName

  final case class CourseInfo(
    @Q id: Long,
    assetName: UUID,
    @Q(traits = Array(Trait.CASE_INSENSITIVE)) title: String,
    @Q(traits = Array(Trait.CASE_INSENSITIVE)) subtitle: String,
  )

  object CourseInfo:
    def apply(c: Asset[Course]): CourseInfo =
      CourseInfo(c.info.id, c.info.name, c.data.title, c.data.subtitle)
end LightweightProjectWebController
