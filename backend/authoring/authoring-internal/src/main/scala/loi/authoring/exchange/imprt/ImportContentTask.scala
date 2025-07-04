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

package loi.authoring.exchange.imprt

import cats.syntax.option.*
import com.learningobjects.de.task.TaskReport
import loi.asset.blob.SourceProperty
import loi.asset.competency.model.CompetencySet
import loi.authoring.AssetType
import loi.authoring.edge.Group
import loi.authoring.exchange.imprt.store.ImportReceiptDao
import loi.authoring.exchange.model.*
import loi.authoring.node.AssetNodeService
import loi.authoring.project.AccessRestriction
import loi.authoring.workspace.{AttachedReadWorkspace, WorkspaceService}
import loi.authoring.write.*
import loi.cp.i18n.AuthoringBundle
import scaloi.syntax.collection.*

import java.util.UUID
import scala.collection.immutable.ListMap

/** This is a sub task of an overall import operation. See ImportOperation for the overall process
  */
class ImportContentTask private (
  report: TaskReport,
  receipt: ImportReceipt,
  manifest: ImportableExchangeManifest,
  expectedAssets: Map[String, UUID],
  targetWs: AttachedReadWorkspace,
)(implicit
  importReceiptDao: ImportReceiptDao,
  nodeService: AssetNodeService,
  workspaceService: WorkspaceService,
  writeService: WriteService,
) extends ImportTask[Seq[ImportedRoot]](report, receipt)(importReceiptDao):

  override def run(): Option[Seq[ImportedRoot]] =

    // nodesOnlyState means we have only imported nodes thus the root-y values on
    // nodesOnlyState are not accurate yet (since we haven't imported edges yet)
    val nodesOnlyState     = importNodes(manifest.createNodes)
    val nodesAndEdgesState = importEdges(manifest.createNodes, nodesOnlyState, targetWs)
    val importState        = connectImportedRoots(nodesAndEdgesState, targetWs)

    val wws       = workspaceService.requireWriteWorkspace(targetWs.bronchId, AccessRestriction.none)
    val result    = writeService.commit(wws, importState.ops.toList).get
    val rootNames = importState.allImportedRoots.values.map(_.name)
    val rootNodes = nodeService.load(result.ws).byName(rootNames).get.groupUniqBy(_.info.name)

    val rootDtos = for
      (importId, importedRoot) <- importState.allImportedRoots.view
      root                     <- rootNodes.get(importedRoot.name)
    yield ImportedRoot(importId, root)

    rootDtos.toSeq.some
  end run

  private def importNodes(nodes: Seq[ImportableNodeExchangeData[?]]): ImportState =

    // capture the _ <: Asset of ImportableNodeExchangeData in A
    def helper[A](node: ImportableNodeExchangeData[A]): AddNode[A] =
      val data = SourceProperty.putSource(node.data, node.file.flatMap(_.blobRef))
      AddNode(data, UUID.randomUUID)(using node.assetType)

    nodes.foldLeft(ImportState.empty)({ case (state, node) =>
      val addNodeOp = helper(node)
      report.markProgress()
      state.withNode(addNodeOp, node.id, node.synthetic)
    })
  end importNodes

  private def importEdges(
    nodes: Seq[ImportableNodeExchangeData[?]],
    importState: ImportState,
    targetWs: AttachedReadWorkspace,
  ): ImportState =
    nodes.foldLeft(importState)({ case (state, source) =>
      val sourceName = importState.requireSource(source).name
      source.createableEdges
        .groupBy(_.group)
        .foldLeft(state)({ case (state2, (group, edges)) =>
          val (nextState2, ordering) =
            edges.foldLeft((state2, Vector.empty[(Long, UUID)]))({ case ((state3, positions), edge) =>
              report.markProgress()
              getTarget(state3, source, edge, targetWs) match
                case Some(importedTgt) =>
                  val addEdgeOp = AddEdge(
                    sourceName = sourceName,
                    targetName = importedTgt,
                    group = edge.group,
                    name = UUID.randomUUID(),
                    traverse = edge.traverse,
                    data = edge.edgeData,
                    edgeId = edge.edgeId,
                  )
                  (
                    state3.withEdge(addEdgeOp, edge.target, edge.synthetic),
                    positions :+ (edge.position -> addEdgeOp.name)
                  )
                case None              => (state3, positions)
              end match
            })
          nextState2.withWriteOp(SetEdgeOrder(sourceName, group, ordering.sortBy(_._1).map(_._2)))
        })

    })

  private def getTarget(
    importState: ImportState,
    source: ImportableNodeExchangeData[?],
    edge: ValidEdgeExchangeData,
    targetWs: AttachedReadWorkspace
  ): Option[UUID] =
    if edge.targetInWorkspace then
      val competency = UUID.fromString(edge.target)
      if targetWs.containsNode(competency) then competency.some
      else
        report.addWarning(AuthoringBundle.importEdgeSkipped(source.id, edge.target, edge.group))
        None
    else if !edge.traverse && manifest.expectedAssets.contains(edge.target) then
      val target = expectedAssets.get(edge.target)
      if target.isEmpty then report.addWarning(AuthoringBundle.importEdgeSkipped(source.id, edge.target, edge.group))
      target
    else importState.requireTarget(source, edge).name.some

  private def connectImportedRoots(
    importState0: ImportState,
    targetWs: AttachedReadWorkspace,
  ): ImportState =

    val importState1 = manifest.setRootAndHome.foldLeft(importState0) { case (acc, (rootImportId, homeImportId)) =>
      // wishing for scala 3 Tuple.map
      val rootName = acc.addNodeOps(rootImportId).name
      val homeName = acc.addNodeOps(homeImportId).name
      acc.withWriteOp(SetRootName(rootName)).withWriteOp(SetHomeName(homeName))
    }

    // if I weren't both angry and lazy I would move this to ValidateImportTask where the root.1 synthesis is
    val rootName = importState1.rootName.getOrElse(targetWs.rootName)
    importState1.importedRoots[CompetencySet].foldLeft(importState1) { case (acc, compSet) =>
      acc.withWriteOp(AddEdge(rootName, compSet.name, Group.CompetencySets, position = Position.End.some))
    }
  end connectImportedRoots
end ImportContentTask

object ImportContentTask:

  def apply(
    receipt: ImportReceipt,
    manifest: ImportableExchangeManifest,
    expectedAssets: Map[String, UUID],
    targetWs: AttachedReadWorkspace,
  )(
    importReceiptDao: ImportReceiptDao,
    nodeService: AssetNodeService,
    workspaceService: WorkspaceService,
    writeService: WriteService,
  ): ImportContentTask =
    val report = receipt.report
      .addChild("Importing Content", manifest.numCreateNodesAndEdges)
    new ImportContentTask(report, receipt, manifest, expectedAssets, targetWs)(using
      importReceiptDao,
      nodeService,
      workspaceService,
      writeService
    )
  end apply
end ImportContentTask

// using ListMap to maintain insertion order because QTI import (there is a selenide test)
// Turns our our import is LOAF/QTI node-order-stable and it _must_ remain so.
private[authoring] case class ImportState(
  addNodeOps: ListMap[String, AddNode[?]],
  otherOps: Vector[WriteOp],
  rootImportIds: Set[String],
  rootName: Option[UUID]
):

  lazy val ops: Vector[WriteOp] = addNodeOps.values.toVector ++ otherOps

  lazy val allImportedRoots: ListMap[String, AddNode[?]] =
    ListMap.from(addNodeOps.view.filterKeys(rootImportIds.contains))

  def importedRoots[A](implicit assetType: AssetType[A]): Iterable[AddNode[?]] =
    allImportedRoots.filter(_._2.assetType == assetType).values

  def withNode(op: AddNode[?], importNodeId: String, synthetic: Boolean): ImportState = copy(
    addNodeOps = addNodeOps.updated(importNodeId, op),
    rootImportIds = if synthetic then rootImportIds else rootImportIds + importNodeId,
  )

  def withEdge(op: AddEdge, targetImportNodeId: String, synthetic: Boolean): ImportState = copy(
    otherOps = otherOps :+ op,
    rootImportIds = if synthetic then rootImportIds else rootImportIds - targetImportNodeId,
  )

  def withWriteOp(op: WriteOp): ImportState = op match
    case SetRootName(rootName) => copy(rootName = rootName.some, otherOps = otherOps :+ op)
    case _                     => copy(otherOps = otherOps :+ op)

  def requireSource(
    source: ImportableNodeExchangeData[?],
  ): AddNode[?] =
    addNodeOps.getOrElse(
      source.id,
      throw new RuntimeException(
        s"${source.createableEdges.size} edge(s) cannot be imported b/c the source (${source.id}) was not imported"
      )
    )

  def requireTarget(
    source: ImportableNodeExchangeData[?],
    edge: ValidEdgeExchangeData
  ): AddNode[?] =
    addNodeOps.getOrElse(
      edge.target,
      throw new RuntimeException(
        s"Edge from ${source.id} to ${edge.target} in " +
          s"group ${edge.group} cannot be imported b/c the target was not imported"
      )
    )
end ImportState

private object ImportState:
  val empty: ImportState = ImportState(ListMap.empty, Vector.empty, Set.empty, Option.empty)
