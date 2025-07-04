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

package loi.authoring.asset.web

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{BaseComponent, ComponentInstance}
import com.learningobjects.cpxp.service.exception.HttpApiException.*
import com.learningobjects.de.authorization.Secured
import loi.authoring.asset.service.exception.*
import loi.authoring.edge.EdgeService
import loi.authoring.edge.service.exception.EdgeException.{GroupDisallowsType, NoSuchGroup, UnreachableTargetType}
import loi.authoring.node.AssetNodeService
import loi.authoring.render.RenderService
import loi.authoring.security.right.AccessAuthoringAppRight
import loi.authoring.web.AuthoringWebUtils
import loi.authoring.workspace.exception.NoSuchNodeInWorkspaceException

import java.util.UUID
import scala.jdk.CollectionConverters.*

/** Web routes for basic asset operations.
  */
@Component
@Controller(root = true, value = "node-web-controller")
@Secured(Array(classOf[AccessAuthoringAppRight]))
private[web] class NodeWebController(
  ci: ComponentInstance,
  webUtils: AuthoringWebUtils,
  nodeService: AssetNodeService,
  renderService: RenderService,
  edgeService: EdgeService,
) extends BaseComponent(ci)
    with ApiRootComponent:

  /** Gets an asset by name or id.
    *
    * @param nameOrId
    *   the name or id of the node
    * @param includeGroup
    *   an optional array edge groups that should be retrieved and added to the response
    * @return
    *   the asset
    */
  @RequestMapping(path = "authoring/{branch}/nodes/{nameOrId}", method = Method.GET)
  def getAsset(
    @PathVariable("branch") branchId: Long,
    @PathVariable("nameOrId") nameOrId: String,
    @QueryParam(required = false) includeGroup: java.util.List[String],
  ): AssetWebDto =

    val workspace = webUtils.workspaceOrThrow404(branchId, cache = false)
    val node      = webUtils.nodeOrThrow404(workspace, nameOrId)
    val groups    = includeGroup.asScala.view.map(grp => webUtils.groupOrThrow400(grp)).toSet
    val outEdges  = edgeService.loadOutEdges(workspace, Seq(node), groups)

    val (List(renderedNode), renderedEdges) = renderService.render(workspace, List(node), outEdges)
    AssetWebDto(renderedNode, groups.map(g => (g, renderedEdges.filter(_.group == g))).toMap)
  end getAsset

  @RequestMapping(path = "authoring/{branch}/nodes/{nameOrId}/slim", method = Method.GET)
  def getSlimAsset(
    @PathVariable("branch") branchId: Long,
    @PathVariable("nameOrId") nameOrId: String,
    @QueryParam(required = false) includeGroup: java.util.List[String],
  ): SlimAssetWebDto =

    val workspace = webUtils.workspaceOrThrow404(branchId, cache = false)
    val node      = webUtils.nodeOrThrow404(workspace, nameOrId)
    val groups    = includeGroup.asScala.view.map(grp => webUtils.groupOrThrow400(grp)).toSet
    val outEdges  = edgeService
      .loadOutEdges(workspace, Seq(node), groups)
      .map(edge =>
        NewEdge(edge.name, edge.source.info.name, edge.target.info.name, edge.data, edge.group, edge.traverse)
      )

    SlimAssetWebDto(node, groups.map(g => (g, outEdges.filter(_.group == g))).toMap)
  end getSlimAsset

  /** bulk asset retrieval The name is sad, but its the only way to differentiate between the routes
    *
    * @param dto
    *   list of names
    * @return
    *   map of name -> asset
    */
  @RequestMapping(path = "authoring/{branch}/nodesInBulk", method = Method.POST)
  def getAssets(
    @PathVariable("branch") branchId: Long,
    @RequestBody dto: BulkFetchByNameDto
  ): Map[UUID, AssetWebDto] =

    val workspace = webUtils.workspaceOrThrow404(branchId, cache = false)

    val loadAttempt =
      for assets <- nodeService.load(workspace).byName(dto.names)
      yield

        val renderedAssets = renderService.render(workspace, assets.toList)
        val edges          = edgeService.loadOutEdges(workspace, assets)
        val embedsMap      = edges.groupBy(_.source.info.name)

        renderedAssets
          .map(renderedAsset =>
            val edgesByGroupMap = embedsMap.getOrElse(renderedAsset.info.name, Nil).groupBy(_.group)
            (renderedAsset.info.name, AssetWebDto(renderedAsset, edgesByGroupMap))
          )
          .toMap

    loadAttempt
      .recover({
        case ex: NoSuchNodeInWorkspaceException => throw notFound(ex)
        case ex: NoSuchAssetException           => throw notFound(ex)
        case ex: UnreachableTargetType          => throw notFound(ex)
        case ex: GroupDisallowsType             => throw notFound(ex)
        case ex: NoSuchGroup                    => throw notFound(ex)
      })
      .get
  end getAssets
end NodeWebController
