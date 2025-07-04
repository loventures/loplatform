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

package loi.authoring.exchange.exprt

import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.de.task.TaskReport
import loi.authoring.edge.EdgeService
import loi.authoring.exchange.exprt.store.ExportReceiptDao
import loi.authoring.exchange.model.*
import loi.authoring.node.AssetNodeService
import loi.authoring.workspace.{ReadWorkspace, TraversalScope}
import org.hibernate.Session
import scalaz.{Success, Validation}

import java.util.UUID

class ExportContentTask(
  report: TaskReport,
  receipt: ExportReceipt,
  workspace: ReadWorkspace,
  scope: TraversalScope
)(
  exportReceiptDao: ExportReceiptDao,
  nodeService: AssetNodeService,
  edgeService: EdgeService,
  session: Session
) extends ExportTask[ExportableExchangeManifest](report, receipt)(exportReceiptDao):

  override protected def run(): Validation[Throwable, ExportableExchangeManifest] =
    Success(createManifest(scope))

  private def createManifest(scope: TraversalScope): ExportableExchangeManifest =

    val batches = scope.nodeIds.keys.toSeq.grouped(1000).toSeq
    batches.foldLeft(ExportableExchangeManifest.empty)({ case (acc, batch) =>
      val nodes = nodeService.load(workspace).byId(batch)

      val edges = edgeService
        .loadOutEdges(workspace, nodes)
        .map(edge =>
          edge.source.info.name -> EdgeExchangeData(
            edge.group,
            edge.target.info.name.toString,
            edge.position,
            edge.traverse,
            edge.edgeId,
            edge.data
          )
        )
        .groupMap(_._1)(_._2)

      val manifestSoFar = nodes.foldLeft(acc)({ case (acc2, node) =>
        val nodeExchangeData = ExportableNodeExchangeData(
          node.info.name.toString,
          node.info.typeId.entryName,
          JacksonUtils.getFinatraMapper.valueToTree(node.data),
          edges.getOrElse(node.info.name, Seq.empty),
        )

        report.markProgress()
        val useCompetencyPolicy = scope.nodeIds(node.info.id)

        if useCompetencyPolicy then
          acc2
            .copy(nodes = acc2.nodes :+ nodeExchangeData, competencyIds = acc2.competencyIds + nodeExchangeData.id)
        else acc2.copy(nodes = acc2.nodes :+ nodeExchangeData)
      })

      session.flush()
      session.clear()
      // what about L2?

      manifestSoFar
    })
  end createManifest
end ExportContentTask

object ExportContentTask:

  def apply(
    receipt: ExportReceipt,
    workspace: ReadWorkspace,
    startingNodeNames: Set[UUID]
  )(
    exportReceiptDao: ExportReceiptDao,
    nodeService: AssetNodeService,
    edgeService: EdgeService,
    session: Session
  ): ExportContentTask =

    val startingNodeIds = startingNodeNames.flatMap(workspace.getNodeId)
    val exportScope     = TraversalScope.build(workspace, startingNodeIds)
    val report          = receipt.report.addChild("Exporting Content", exportScope.nodeIds.size)

    new ExportContentTask(report, receipt, workspace, exportScope)(
      exportReceiptDao,
      nodeService,
      edgeService,
      session
    )
  end apply
end ExportContentTask
