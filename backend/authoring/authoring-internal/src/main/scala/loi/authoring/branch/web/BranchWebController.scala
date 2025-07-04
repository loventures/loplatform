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

package loi.authoring.branch.web

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.ApiQueryResults
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{BaseComponent, ComponentInstance}
import com.learningobjects.cpxp.service.exception.HttpApiException.*
import com.learningobjects.de.authorization.Secured
import loi.authoring.asset.Asset
import loi.authoring.branch.Branch
import loi.authoring.index.{EsQuery, EsService}
import loi.authoring.info.AssetPathElement
import loi.authoring.node.AssetNodeService
import loi.authoring.project.web.ProjectsResponse
import loi.authoring.project.{AccessRestriction, ProjectDao2}
import loi.authoring.render.RenderService
import loi.authoring.search.web.exception.WebQueryParseException
import loi.authoring.search.web.{GraphWebQuery, WebQueryService}
import loi.authoring.security.right.*
import loi.authoring.web.AuthoringWebUtils
import loi.authoring.write.web.WriteCommit
import loi.cp.user.UserService
import scalaz.std.list.*
import scalaz.std.string.*
import scaloi.syntax.boolean.*
import scaloi.syntax.collection.*
import scaloi.syntax.option.*

import scala.jdk.CollectionConverters.*

@Component
@Controller(root = true, value = "branch-web-controller")
private[web] class BranchWebController(
  ci: ComponentInstance,
  webUtils: AuthoringWebUtils,
  projectDao: ProjectDao2,
  webQueryService: WebQueryService,
  esService: EsService,
  assetNodeService: AssetNodeService,
  renderService: RenderService,
)(implicit
  userService: UserService,
) extends BaseComponent(ci)
    with ApiRootComponent:

  @RequestMapping(path = "authoring/branches/{id}/linked", method = Method.GET)
  def getLinkedBranches(
    @PathVariable("id") branchId: Long,
  ): ProjectsResponse =
    val branch            =
      webUtils.branchOrFakeBranchOrThrow404(branchId, AccessRestriction.projectMemberOr[EditContentAnyProjectRight])
    val dependentProjects = projectDao.loadImmediateDependentProjects(branch.id)
    ProjectsResponse(dependentProjects.map(_.asBranch))
  end getLinkedBranches

  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/branches/{id}", method = Method.GET)
  def getBranch(@PathVariable("id") id: Long): Branch =
    webUtils.branchOrFakeBranchOrThrow404(id)

  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/branches/{id}/head", method = Method.GET)
  def getBranchHead(@PathVariable("id") id: Long): WriteCommit =
    WriteCommit(webUtils.branchOrFakeBranchOrThrow404(id).head)

  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/branches/{id}/blobs", method = Method.GET)
  def searchBranch(
    @PathVariable("id") id: Long,
    @QueryParam(value = "query") jsonQuery: ArgoBody[GraphWebQuery]
  ): ApiQueryResults[AssetWithHighlights] =

    val webQuery  = jsonQuery.decode_!.get // throws 422
    val workspace = webUtils.workspaceOrThrow404(id, cache = false)
    val query     = webQueryService
      .validate(webQuery, workspace)
      .recover({ case ex: WebQueryParseException =>
        throw unprocessableEntity(ex)
      })
      .get

    val excludeNames = query.groupExclusion foldZ { case (asset, group) =>
      workspace.outEdgeInfos(asset.info.name, group).map(_.targetId).map(workspace.nodeName).toList
    }
    val results      = esService.search(
      EsQuery(
        search = query.searchTerm.filterNZ,
        fields = query.fields.map(f => s"data.$f"),
        branch = Some(workspace.bronchId),
        offering = Some(0L),
        archived = query.includeArchived.noption(false),
        typeIds = query.typeIds.toList,
        excludeNames = excludeNames,
        sortBy =
          query.ordering.map(o => o.propertyName -> (o.direction.toLowerCase == "asc")).toList, // any keyword field
        from = query.offset.map(_.intValue),
        size = query.limit.map(_.intValue),
      )
    )

    val assets               = assetNodeService.load(workspace).byName(results.hits.map(_.name)).get
    val renderedAssets       = renderService.render(workspace, assets.toList).groupUniqBy(_.info.name)
    val assetsWithHighlights = results.hits map { hit =>
      AssetWithHighlights(renderedAssets(hit.name), hit.highlights, Seq.empty)
    }
    new ApiQueryResults(assetsWithHighlights.asJava, results.total, results.total)
  end searchBranch
end BranchWebController

final case class AssetWithHighlights(
  asset: Asset[?],
  highlights: Map[String, Seq[String]],
  paths: Seq[Seq[AssetPathElement]], // always empty
)
