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

package loi.authoring.copy

import com.fasterxml.jackson.databind.node.ObjectNode
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.cpxp.util.GuidUtil
import com.learningobjects.de.task.{BaseTaskReport, TaskReport}
import de.tomcat.juli.LogMeta
import loi.asset.blob.SourceProperty
import loi.authoring.asset.Asset
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.copy.store.{CopyReceiptDao, CopyReceiptEntity}
import loi.authoring.copy.web.{BronchAsset, BronchAssets}
import loi.authoring.edge.{EdgeService, Group}
import loi.authoring.node.AssetNodeService
import loi.authoring.workspace.{AttachedReadWorkspace, EdgeInfo, WorkspaceService, WriteWorkspace}
import loi.authoring.write.*
import loi.cp.asset.edge.EdgeData
import org.log4s.Logger
import scaloi.syntax.finiteDuration.*

import java.util.{Date, UUID}
import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}
import scala.util.Try

@Service
private[copy] class ContentCopyService(
  assets: AssetNodeService,
  domain: => DomainDTO,
  receiptDao: CopyReceiptDao,
  user: => UserDTO,
  workspaces: WorkspaceService,
  writes: WriteService,
  edges: EdgeService,
):
  import ContentCopyService.*

  def copy(src: BronchAssets, group: Group, dst: BronchAsset, beforeEdge: Option[UUID]): Try[List[UUID]] =
    val start = new Date()
    val srcWs = workspaces.requireReadWorkspace(src.branch)
    val dstWs = workspaces.requireWriteWorkspace(dst.branch)

    for
      srcAssets <- assets.load(srcWs).byName(src.nodes)
      dstAsset  <- assets.load(dstWs).byName(dst.node)

      opNodes = opsAndSrcNodes(srcWs, srcAssets.toList, dstWs, dstAsset, group, beforeEdge)

      commitRes <- writes.commit(dstWs, opNodes.ops)
    yield
      logCopy(srcWs, srcAssets, opNodes.newNodeNameSrcAssets, commitRes, start, new Date())
      opNodes.resultNames
    end for
  end copy

  private def logCopy(
    src: AttachedReadWorkspace,
    srcAssets: Seq[Asset[?]],
    nodes: Seq[(UUID, Asset[?])],
    res: CommitResult[AttachedReadWorkspace],
    start: Date,
    end: Date
  ): Unit =
    val report = CopyReport(
      info = SrcDst(
        source = BronchRef(src.bronchId, src.commitId),
        target = BronchRef(res.ws.bronchId, res.ws.commitId)
      ),
      nodes = nodes
        .groupBy { case (_, srcNode) => srcNode.info.typeId }
        .view
        .mapValues(_.map { case (newName, srcNode) =>
          SrcDst(srcNode.info.name, newName)
        })
        .toMap,
    )
    report.markStart(start)
    report.markProgress(nodes.size)
    report.markComplete(end)

    LogMeta.put("cc_src_branch", report.info.source.branch)
    LogMeta.put("cc_src_commit", report.info.source.commit)
    LogMeta.put("cc_dst_branch", report.info.target.branch)
    LogMeta.put("cc_dst_commit", report.info.target.commit)
    log.info(
      s"content-copied following nodes: ${report.nodes} " +
        s"in ${FiniteDuration(end.getTime - start.getTime, MILLISECONDS).toHumanString}"
    )

    // only receipt the first node.
    for
      srcAsset  <- srcAssets.headOption
      dstName   <- nodes.find(_._2.info.name == srcAsset.info.name).map(_._1)
      copyAsset <- assets.load(res.ws).byName(dstName).toOption
    yield
      val json: ObjectNode = JacksonUtils.getFinatraMapper.valueToTree(report)
      // ContentCopyService.CopyReport is only used for this, I don't want to make
      // it a public sub-type of TaskReport just to avoid jackson errors. So I
      // mark it as unbounded so the de-serialization works.
      json.put("type", "unbounded")

      receiptDao.save(
        new CopyReceiptEntity(
          null,
          srcAsset.info.id,
          copyAsset.info.id,
          json,
          CopyReceiptStatus.SUCCESS.name(),
          new Date(),
          start,
          end,
          user.id,
          domain.id
        )
      )
    end for
  end logCopy

  private def opsAndSrcNodes(
    srcWs: AttachedReadWorkspace,
    srcAssets: List[Asset[?]],
    dstWs: WriteWorkspace,
    dstAsset: Asset[?],
    group: Group,
    beforeEdge: Option[UUID]
  ) =
    val existingEdges = dstWs.outEdgeInfos(dstAsset.info.name, group).toSeq.sortBy(_.position).map(_.name)

    val (nodeIds, edgeInfos) = filterGraph(srcWs, srcAssets)
    val nodes                = assets.load(srcWs).byId(nodeIds)
    val addOpsByNodeId       = nodes.map(n => addOp(n)).toMap[Long, AddNode[?]]
    val finalEdges           = srcAssets.map(srcAsset =>
      AddEdge(
        sourceName = dstAsset.info.name,
        targetName = addOpsByNodeId(srcAsset.info.id).name,
        group = group
      )
    )
    val finalEdgeNames       = finalEdges.map(_.name)
    val ordering             = beforeEdge match
      case Some(name) =>
        val before = existingEdges.takeWhile(_ != name)
        before :++ finalEdgeNames :++ existingEdges.drop(before.length)
      case None       => existingEdges :++ finalEdgeNames
    val edgeOps              = this.edgeOps(srcWs, addOpsByNodeId, edgeInfos) :++ finalEdges :+
      SetEdgeOrder(
        sourceName = dstAsset.info.name,
        group = group,
        ordering = ordering
      )

    OpNodes(
      finalEdges.map(_.targetName),
      addOpsByNodeId.values.toList ++ edgeOps,
      nodes.map(n => addOpsByNodeId(n.info.id).name -> n)
    )
  end opsAndSrcNodes

  private def filterGraph(ws: AttachedReadWorkspace, srcs: List[Asset[?]]) =
    @annotation.tailrec
    def go0(ids: Set[Long], acc: Set[Long]): Set[Long] =
      val eis = ids
        .flatMap(ws.outEdgeInfos)
        .filter(i => i.traverse)
      if eis.nonEmpty then go0(eis.map(_.targetId), acc ++ ids)
      else acc ++ ids

    val start = srcs.map(_.info.id).toSet
    val nodes = go0(start, Set.empty)

    @annotation.tailrec
    def go1(ns: Set[Long], eis: Set[EdgeInfo]): Set[EdgeInfo] =
      val eis1 = ns
        .flatMap(ws.outEdgeInfos)
        .filter(i => nodes(i.targetId) && !eis(i))
      if eis1.nonEmpty then go1(eis1.map(_.targetId), eis ++ eis1)
      else eis

    val eis = go1(start, Set.empty)
    (nodes, eis)
  end filterGraph

  private def addOp[A](a: Asset[A]) =
    a.info.id -> AddNode(a.data, exemptBlobName = SourceProperty.fromNode(a).map(_.name))(using a.assetType)

  private def edgeOps(srcWs: AttachedReadWorkspace, addOpsByNodeId: Map[Long, AddNode[?]], infos: Set[EdgeInfo]) =
    val dataByEdgeId = edges.loadEdgesAnyTypeById(srcWs, infos.map(_.id)).map(e => e.id -> e.data).toMap

    val filteredEdges = infos
      .groupBy(info => (info.sourceId, info.group))
      .view
      .mapValues(_.toSeq.sortBy(_.position))
      .toMap
    filteredEdges.flatMap { case ((srcNodeId, group), edgeInfos) =>
      val sourceName = addOpsByNodeId(srcNodeId).name
      val addOps     = edgeInfos.map { info =>
        AddEdge(
          sourceName = sourceName,
          targetName = addOpsByNodeId(info.targetId).name,
          group = info.group,
          traverse = info.traverse,
          data = dataByEdgeId.getOrElse(info.id, EdgeData.empty),
          edgeId = info.edgeId
        )
      }
      addOps :+ SetEdgeOrder(
        sourceName = sourceName,
        group = group,
        ordering = addOps.map(_.name)
      )
    }.toSeq
  end edgeOps
end ContentCopyService

object ContentCopyService:
  private val log: Logger = org.log4s.getLogger

  private case class OpNodes(resultNames: List[UUID], ops: List[WriteOp], newNodeNameSrcAssets: Seq[(UUID, Asset[?])])
  private case class BronchRef(branch: Long, commit: Long)
  private case class SrcDst[T](source: T, target: T):
    override def toString: String = s"$source -> $target"

  private case class CopyReport(
    info: SrcDst[BronchRef],
    nodes: Map[AssetTypeId, Seq[SrcDst[UUID]]]
    // extending this class so there are no deserialization errors
  ) extends BaseTaskReport(null, s"content-copy ${GuidUtil.guid}", null, null, null, null, false, 0, null):
    override def copy(): TaskReport = ???
end ContentCopyService
