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

package loi.authoring.write

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.domain.DomainDTO
import loi.authoring.*
import loi.authoring.project.*
import loi.authoring.workspace.*
import loi.authoring.write.LayeredWriteService.InsertState

import java.sql.Timestamp
import java.util.UUID
import scala.annotation.tailrec
import scala.util.Try

/** Handles writing changes to nodes and edges.
  */
@Service
class BaseWriteService(
  commitTime: => Timestamp,
  domainDto: => DomainDTO,
  layeredWriteService: LayeredWriteService,
  layeredWriteReversalService: LayeredWriteReversalService,
) extends WriteService:

  override def commit(
    ws: WriteWorkspace,
    ops: List[WriteOp],
    squash: Boolean = false
  ): Try[CommitResult[AttachedReadWorkspace]] = ws match
    case lws: LayeredWriteWorkspace =>
      layeredWriteService.commit(lws, ops, squash).left.map(_.exception).toTry

  /** A special, authoring-internal only variant of `commit` intended for committing an entire copy of `ws` to a new
    * domain. The special-ness is to avoid the possibility that, if the regular `commit` variant included a domain
    * parameter, a stranger would mistakenly pass the not-current domain. Then the graph would be smeared across
    * multiple domains. This is harmless until we delete a domain in the extreme sense which we never do. But still, I
    * would prefer to avoid the mess.
    */
  def commitForWorkspaceCopy(
    ws: WriteWorkspace,
    domain: DomainDTO,
    roots: Set[UUID],
    addNodeOps: List[ValidatedAddNode[?]],
    addEdgeOps: List[ValidatedAddEdge]
  ): CommitResult[AttachedReadWorkspace] =
    ws match
      case lws: LayeredWriteWorkspace =>
        val s = InsertState.forWsCopy(lws, roots, addNodeOps, addEdgeOps, commitTime.toLocalDateTime)
        layeredWriteService.insert(s, domain)

  def commitForWorkspaceCopy(
    ws: WriteWorkspace,
    roots: Set[UUID],
    addNodeOps: List[ValidatedAddNode[?]],
    addEdgeOps: List[ValidatedAddEdge]
  ): CommitResult[AttachedReadWorkspace] = commitForWorkspaceCopy(ws, domainDto, roots, addNodeOps, addEdgeOps)

  def loadAncestorGraph(
    ws: ReadWorkspace,
    startingNodeNames: Set[UUID],
    deletedEdges: Set[UUID]
  ): GraphData2 =

    val startingNodes = startingNodeNames
      .flatMap(n => ws.getNodeId(n).map(id => NodeInfo2(n, id)))

    @tailrec
    def loop(tgtNames: Set[UUID], acc0: Set[NodeInfo2]): Set[NodeInfo2] =
      val srcs = for
        tgtName <- tgtNames.view
        inEdge  <- ws.inEdgeAttrs(tgtName)
        if !deletedEdges.contains(inEdge.name)
        srcId   <- ws.getNodeId(inEdge.srcName)
      yield NodeInfo2(inEdge.srcName, srcId)

      val newSrcs = srcs.toSet -- acc0 // important to handle cycles
      if newSrcs.isEmpty then acc0 else loop(newSrcs.map(_.name), acc0 ++ newSrcs)
    end loop

    val nodes = loop(startingNodes.map(_.name), startingNodes)
    val edges = nodes.flatMap(n => ws.outEdgeElems(n.name))
    GraphData2(nodes, edges)
  end loadAncestorGraph

  override def reverseWriteOps(ws: WriteWorkspace, commit: BigCommit): Either[ReversingError, List[WriteOp]] =
    ws match
      case lws: LayeredWriteWorkspace => layeredWriteReversalService.reverseWriteOps(lws, commit)

end BaseWriteService
