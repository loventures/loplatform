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

package loi.authoring.exchange.model

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.de.web.MediaType
import loi.authoring.AssetType
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.blob.BlobRef
import loi.authoring.edge.Group
import loi.cp.asset.edge.EdgeData
import scaloi.syntax.CollectionOps.*

import java.io.File
import java.util.UUID

// This file contains data structure that represent asset data as it moves through the
// phases of import. Primarily the phases are 1. validate, 2. import attachments
// 3. import content

sealed trait ProcessedExchangeManifest[A <: ProcessedNodeExchangeData[?, C], C <: ProcessedFileData]:
  def nodes: Seq[A]
  def competencyIds: Set[String]

  lazy val expectedAssets: Map[String, A] = nodes
    .groupUniqBy(_.id)
    .view
    .filterKeys(competencyIds.contains)
    .toMap

case class ValidatedExchangeManifest(
  nodes: Seq[ValidatedNodeExchangeData[?]],
  setRootAndHome: Option[(String, String)],
  competencyIds: Set[String]
) extends ProcessedExchangeManifest[ValidatedNodeExchangeData[?], ValidatedFileData]:

  lazy val numFiles: Int = nodes.count(_.file.isDefined)

  lazy val numNodesAndEdges: Int =
    nodes.size + nodes.flatMap(_.edges).size
end ValidatedExchangeManifest

case class ImportableExchangeManifest(
  nodes: Seq[ImportableNodeExchangeData[?]],
  setRootAndHome: Option[(String, String)],
  competencyIds: Set[String]
) extends ProcessedExchangeManifest[ImportableNodeExchangeData[?], ImportedFileData]:

  lazy val createNodes: Seq[ImportableNodeExchangeData[?]] =
    nodes.filterNot(n => competencyIds.contains(n.id))

  lazy val numCreateNodesAndEdges: Int =
    createNodes.size + createNodes.flatMap(_.edges).size
end ImportableExchangeManifest

case class ExportableExchangeManifest(
  version: String,
  nodes: Seq[ExportableNodeExchangeData],
  competencyIds: Set[String]
):

  lazy val jsonManifest: ExchangeManifest = ExchangeManifest(
    version,
    nodes.map(_.jsonNodeExchangeData),
    competencyIds
  )
end ExportableExchangeManifest

object ExportableExchangeManifest:
  val empty = ExportableExchangeManifest(ExchangeManifest.currentVersion, Seq.empty, Set.empty)

sealed trait ProcessedNodeExchangeData[A, B <: ProcessedFileData]:
  def id: String
  def assetType: AssetType[A]
  def data: A
  def edges: Seq[ValidEdgeExchangeData]
  def file: Option[B]

  lazy val isExpected: Boolean =
    AssetTypeId.CompetencyAndSetTypes.contains(assetType.id)

/** @param synthetic
  *   true disqualifies the node as imported root on the import receipt
  */
case class ValidatedNodeExchangeData[A](
  id: String,
  assetType: AssetType[A],
  data: A,
  edges: Seq[ValidEdgeExchangeData],
  file: Option[ValidatedFileData],
  synthetic: Boolean
) extends ProcessedNodeExchangeData[A, ValidatedFileData]:

  def toImportableData(blobData: Option[ImportedFileData]): ImportableNodeExchangeData[A] =
    ImportableNodeExchangeData(id, assetType, data, edges, blobData, synthetic)
end ValidatedNodeExchangeData

/** @param synthetic
  *   true disqualifies the node as imported root on the import receipt
  */
case class ImportableNodeExchangeData[A](
  id: String,
  assetType: AssetType[A],
  data: A,
  edges: Seq[ValidEdgeExchangeData],
  file: Option[ImportedFileData],
  synthetic: Boolean
) extends ProcessedNodeExchangeData[A, ImportedFileData]:
  lazy val createableEdges: Seq[ValidEdgeExchangeData] = edges.filter(_.targetExists)

case class ExportableNodeExchangeData(
  id: String,
  typeId: String,
  data: JsonNode,
  edges: Seq[EdgeExchangeData],
):
  lazy val jsonNodeExchangeData = NodeExchangeData(
    id,
    typeId,
    data,
    edges,
    None,
  )

  lazy val hasAttachment: Boolean = data.has("source")
end ExportableNodeExchangeData

/** @param targetExists
  *   a workaround for an issue caused by export from our system. When a competency is exported because it is the target
  *   of some traverse=false edge, the NodeExchangeData generated for the competency will contain the competency's
  *   outgoing edges (as it does for all nodes). However, the traverse=false of that incoming edges means we do not
  *   process those out-edges so those edges' targets are not in the exchange. This is valid and targetExists will be
  *   false in that case.
  * @param targetInWorkspace
  *   the target is actually the name of a node that is already in the workspace. this is typically used when an import
  *   has matched QTI question competency alignment to known competencies in the target project.
  * @param synthetic
  *   true disqualifies the edge as an in-edge to its target, allowing the target ot be an imported root for purposes of
  *   the import receipt
  */
case class ValidEdgeExchangeData(
  group: Group,
  target: String,
  position: Long,
  traverse: Boolean,
  edgeId: UUID,
  edgeData: EdgeData,
  targetExists: Boolean,
  targetInWorkspace: Boolean,
  synthetic: Boolean
)

// imports are processed in two phases. First we validate which yields some
// data. Then we import, which yields different data
sealed trait ProcessedFileData

case class ValidatedFileData(
  filename: String,
  mediaType: MediaType,
  file: File
) extends ProcessedFileData

case class ImportedFileData(
  filename: String,
  blobRef: Option[BlobRef],
) extends ProcessedFileData
