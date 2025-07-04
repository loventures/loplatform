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

package loi.authoring.write.web

import cats.syntax.either.*
import com.fasterxml.jackson.databind.node.ArrayNode
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.util.JacksonUtils.getFinatraMapper
import com.learningobjects.cpxp.component.web.util.ObjectMapperOps.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.cpxp.component.{BaseComponent, ComponentInstance}
import com.learningobjects.cpxp.service.exception.HttpApiException.{conflict, unprocessableEntity}
import com.learningobjects.cpxp.service.presence.EventType
import com.learningobjects.cpxp.service.user.{UserDTO, UserType}
import com.learningobjects.de.authorization.Secured
import com.learningobjects.de.web.UncheckedMessageException
import loi.authoring.configuration.AuthoringConfigurationService
import loi.authoring.edge.EdgeService
import loi.authoring.node.AssetNodeService
import loi.authoring.project.CommitDao2
import loi.authoring.render.RenderService
import loi.authoring.security.right.AccessAuthoringAppRight
import loi.authoring.web.AuthoringWebUtils
import loi.authoring.write.exception.*
import loi.authoring.write.{BaseWriteService, LayeredWriteWorkspace, ReversingError}
import loi.cp.i18n.AuthoringBundle
import loi.cp.i18n.syntax.bundleMessage.*
import loi.cp.presence.PresenceService
import loi.cp.presence.SceneActor.InBranch
import scaloi.misc.TimeSource
import scaloi.syntax.boolean.*
import scaloi.syntax.date.*
import scaloi.syntax.disjunction.*
import scaloi.syntax.localDateTime.*

import scala.concurrent.duration.*

@Component
@Controller(root = true, value = "authoring-write-web-controller")
private[web] class WriteWebController(
  ci: ComponentInstance,
  commitDao2: CommitDao2,
  edgeService: EdgeService,
  nodeService: AssetNodeService,
  renderService: RenderService,
  webUtils: AuthoringWebUtils,
  writeRequestValidationService: WriteRequestValidationService,
  writeService: BaseWriteService,
  configurationService: AuthoringConfigurationService,
  presenceService: PresenceService,
  now: TimeSource,
  user: UserDTO,
) extends BaseComponent(ci)
    with ApiRootComponent:

  private val log = org.log4s.getLogger

  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/{branchId}/write", method = Method.POST)
  def write(
    @RequestBody writeJson: ArrayNode,
    @PathVariable("branchId") branchId: Long,
    @QueryParam(value = "commit", decodeAs = classOf[Long]) commitOpt: Option[Long],
  ): WriteResponse =

    val ws = webUtils.writeWorkspaceOrThrow404(branchId)

    val requests: Seq[WriteRequest] = getFinatraMapper.tree2Value[List[WriteRequest]](writeJson)
    val maintenance                 = ws.projectInfo.maintenance && user.userType != UserType.Overlord

    val attempt = for
      _            <- maintenance.thenFailure(ProjectUndergoingMaintenance())
      _            <- commitOpt.forall(_ == ws.commitId) <@~* CommitConflict(commitOpt.get, ws.commitId)
      bundle       <- writeRequestValidationService
                        .validate(requests, ws)
                        .toTry(errList => WriteRequestValidationException(errList))
      sameAuthor    = ws.createdBy == user.id
      recentCommit  = now.date - ws.created.asDate < 5.minutes
      result       <- writeService.commit(ws, bundle.writeOps, squash = sameAuthor && recentCommit)
      updatedNodes <- nodeService.load(result.ws).byName(result.modifiedNodes.keySet)
      updatedEdges  = edgeService.load(result.ws).byName(result.modifiedEdges.keySet)
    yield

      val (renderedNodes, renderedEdges) = renderService.render(result.ws, updatedNodes.toList, updatedEdges)

      // wrvs selected names, wvs may have selected better ones (recovered durable edge names)
      val requestEdgeNames = bundle.newRequestNamesByUuid.edges.map({ case (wrvsName, requestName) =>
        result.recoveredEdges.getOrElse(wrvsName, wrvsName) -> requestName
      })

      // what remote assets are now customized...
      val customizedAssets = result.ws match
        case lws: LayeredWriteWorkspace =>
          val modifiedRemoteSources = for
            edge       <- renderedEdges.view
            sourceElem <- lws.getNodeElem(edge.source.info.name) if !sourceElem.isLocal
          yield edge.source.info.name

          val modifiedRemoteNodes = for
            node <- renderedNodes.view
            elem <- lws.getNodeElem(node.info.name) if !elem.isLocal
          yield node.info.name

          modifiedRemoteSources ++ modifiedRemoteNodes

        case _ => throw new IllegalStateException(s"Unexpected workspace ${result.ws.getClass.getName}")

      // delays until commit
      if configurationService.getConfig.realTime then
        presenceService
          .deliverToScene(BranchHeadChanged(result.ws.commitId, result.ws.created.asDate.getTime))(
            InBranch(branchId, None)
          )

      WriteResponse(
        renderedNodes,
        renderedEdges,
        result.deletedEdges,
        bundle.newRequestNamesByUuid.nodes,
        requestEdgeNames,
        customizedAssets.iterator.to(Set),
        WriteCommit(ws),
        WriteCommit(result.ws),
        result.squashed,
      )

    attempt.recover(writeErr2HttpErr).get
  end write

  private val writeErr2HttpErr: PartialFunction[Throwable, Nothing] = {
    case ex: OutdatedData              => log.warn(ex)(ex.getMessage); throw unprocessableEntity(ex)
    case ex: Logical                   => log.warn(ex)(ex.getMessage); throw unprocessableEntity(WebMessage(ex.messageException))
    case ex: CommitConflict            => log.warn(ex)(ex.getMessage); throw conflict(ex.getErrorMessage)
    case ex: UncheckedMessageException => log.warn(ex)(ex.getMessage); throw unprocessableEntity(ex)
  }

  /** Writes a new commit whose ops are the reverse of `commitId`.
    */
  @RequestMapping(path = "authoring/{projectId}/commits/{commitId}/reverse", method = Method.POST)
  def reverseCommit(
    @PathVariable("projectId") projectId: Long,
    @PathVariable("commitId") commitId: Long
  ): WriteResponse =

    val commit = commitDao2.loadBigCommit(commitId).getOrElse(AuthoringBundle.noSuchCommit(commitId).throw422)
    val ws     = webUtils.writeWorkspaceOrThrow404(projectId)

    val reverseOps = writeService.reverseWriteOps(ws, commit).valueOr {
      case e: ReversingError.NoParentCommit.type     => throw new RuntimeException(e.msg)
      case e: ReversingError.NoSuchNode              => throw new RuntimeException(e.msg)
      case e: ReversingError.NoSuchEdge              => throw new RuntimeException(e.msg)
      case e: ReversingError.IrreversibleCommit.type => AuthoringBundle.irreversibleCommit.throw422
      case e: ReversingError.IrreversibleOpType      => AuthoringBundle.irreversibleOpType(e.opClassName).throw422
      case e: ReversingError.NodeConflict            => AuthoringBundle.nodeConflict(e.name).throw422
      case e: ReversingError.EdgeConflict            => AuthoringBundle.edgeConflict(e.name).throw422
    }

    val attempt = for
      result       <- writeService.commit(ws, reverseOps)
      updatedNodes <- nodeService.load(result.ws).byName(result.modifiedNodes.keySet)
      updatedEdges  = edgeService.load(result.ws).byName(result.modifiedEdges.keySet)
    yield
      val (renderedNodes, renderedEdges) = renderService.render(result.ws, updatedNodes.toList, updatedEdges)

      // delays until commit
      if configurationService.getConfig.realTime then
        presenceService
          .deliverToScene(BranchHeadChanged(result.ws.commitId, result.ws.created.asDate.getTime))(
            InBranch(projectId, None)
          )

      WriteResponse(
        renderedNodes,
        renderedEdges,
        result.deletedEdges,
        Map.empty,
        Map.empty,
        Set.empty, // customized assets.. this may be incorrect
        WriteCommit(ws),
        WriteCommit(result.ws),
        result.squashed,
      )

    attempt.recover(writeErr2HttpErr).get
  end reverseCommit
end WriteWebController

final case class BranchHeadChanged(commit: Long, created: Long)

object BranchHeadChanged:
  implicit val BranchCommitType: EventType[BranchHeadChanged] = EventType("BranchHeadChanged")
