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

package loi.authoring.edge.web

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ArgoBody, Method}
import com.learningobjects.cpxp.component.{BaseComponent, ComponentInstance}
import com.learningobjects.de.authorization.Secured
import loi.authoring.edge.*
import loi.authoring.security.right.AccessAuthoringAppRight
import loi.authoring.web.AuthoringWebUtils

import java.util.UUID

@Component
@Controller(root = true, value = "edge-web-controller")
@Secured(Array(classOf[AccessAuthoringAppRight]))
private[web] class EdgeWebController(
  ci: ComponentInstance,
  edgeService: EdgeService,
  webUtils: AuthoringWebUtils,
) extends BaseComponent(ci)
    with ApiRootComponent:

  @RequestMapping(path = "authoring/{branch}/edges", method = Method.GET)
  def loadEdges(
    @PathVariable("branch") branchId: Long,
    @QueryParam(value = "options") jsonDto: ArgoBody[LoadEdgesWebDto]
  ): Seq[AssetEdge.Any] =

    val dto       = jsonDto.decode_!.get
    val workspace = webUtils.workspaceOrThrow404(branchId, cache = false)
    val node      = webUtils.nodeOrThrow422(workspace, dto.node)

    dto match
      case outgoing: OutgoingEdges =>
        edgeService.loadOutEdges(workspace, Seq(node))
      case incoming: IncomingEdges =>
        edgeService.loadInEdges(workspace, Seq(node), incoming.group.toSet)
      case all: AllEdges           =>
        val ins  = edgeService.loadInEdges(workspace, Seq(node), all.group.toSet)
        val outs = edgeService.loadOutEdges(workspace, Seq(node), all.group.toSet)
        ins ++ outs
  end loadEdges
end EdgeWebController

private[web] sealed trait LoadEdgesWebDto:
  def node: UUID
  def group: Option[Group]

// no time to investigate type tags
private[web] case class OutgoingEdges(
  node: UUID,
  group: Option[Group]
) extends LoadEdgesWebDto

private[web] case class IncomingEdges(
  node: UUID,
  group: Option[Group]
) extends LoadEdgesWebDto

private[web] case class AllEdges(
  node: UUID,
  group: Option[Group]
) extends LoadEdgesWebDto

private[web] object LoadEdgesWebDto:
  import argonaut.*
  import Argonaut.*

  implicit val codec: CodecJson[LoadEdgesWebDto] = CodecJson(
    le =>
      ("direction"                              := (le match
        case _: OutgoingEdges => "outgoing"
        case _: IncomingEdges => "incoming"
        case _: AllEdges => "all")) ->: ("node" := le.node) ->: ("group" := le.group) ->: jEmptyObject,
    c =>
      for
        direction <- (c --\ "direction").as[String]
        node      <- (c --\ "node").as[UUID]
        group     <- (c --\ "group").as[Option[Group]]
        le        <- direction match
                       case "outgoing" => DecodeResult.ok(OutgoingEdges(node, group))
                       case "incoming" => DecodeResult.ok(IncomingEdges(node, group))
                       case "all"      => DecodeResult.ok(AllEdges(node, group))
                       case unknown    => DecodeResult.fail(s"Unknown direction: $unknown", c.history)
      yield le
  )
end LoadEdgesWebDto
