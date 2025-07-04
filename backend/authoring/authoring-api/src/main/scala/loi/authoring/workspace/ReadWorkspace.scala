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

package loi.authoring.workspace

import loi.authoring.ProjectId
import loi.authoring.branch.Branch
import loi.authoring.edge.{EdgeAttrs, EdgeElem, Group}
import loi.authoring.project.*
import loi.authoring.project.Commit2.{ComboDoc, Elem}
import loi.authoring.workspace.exception.NoSuchNodeInWorkspaceException
import scaloi.syntax.OptionOps.*

import java.time.LocalDateTime
import java.util.UUID
import scala.collection.View
import scala.util.Try

trait ReadWorkspace:

  def commitId: Long
  def created: LocalDateTime
  def createdBy: Long
  def layered: Boolean

  def rootIds: Map[UUID, Long]

  def getNodeId(name: UUID): Option[Long]
  def getNodeName(id: Long): Option[UUID]
  def nodeNameIds: View[(UUID, Long)]
  def nodeNames: View[UUID]

  def getEdgeId(name: UUID): Option[Long]
  def getEdgeInfo(name: UUID): Option[EdgeInfo]
  def getEdgeAttrs(name: UUID): Option[EdgeAttrs]
  def getEdgeElem(name: UUID): Option[EdgeElem]
  def edgeIds: View[Long]
  def edgeNames: View[UUID]
  def edgeInfos: View[EdgeInfo]

  def outEdgeInfos(srcId: Long): View[EdgeInfo]
  def outEdgeAttrs(srcName: UUID): View[EdgeAttrs]
  def outEdgeElems(srcName: UUID): View[EdgeElem]
  def inEdgeInfos(tgtId: Long): View[EdgeInfo]
  def inEdgeInfos(tgtName: UUID): View[EdgeInfo]
  def inEdgeAttrs(tgtName: UUID): View[EdgeAttrs]
  def inEdgeElems(tgtName: UUID): View[EdgeElem]

  // convenience methods

  final def nodeId(name: UUID): Long          = getNodeId(name).orThrow(new NoSuchElementException(s"node $name"))
  final def nodeName(id: Long): UUID          = getNodeName(id).orThrow(new NoSuchElementException(s"node $id"))
  final def nodeIds: View[Long]               = nodeNameIds.map(_._2)
  final def containsNode(name: UUID): Boolean = getNodeId(name).isDefined
  final def containsNode(id: Long): Boolean   = getNodeName(id).isDefined

  final def getNodeIds(names: Iterable[UUID]): View[Long] = names.view.flatMap(getNodeId)
  final def requireNodeId(nodeName: UUID): Try[Long]      =
    getNodeId(nodeName).toTry(NoSuchNodeInWorkspaceException(nodeName, long2Long(commitId)))

  final def getEdgeIds(names: Iterable[UUID]): View[Long]       = names.view.flatMap(getEdgeId)
  final def getEdgeInfos(names: Iterable[UUID]): View[EdgeInfo] = names.view.flatMap(getEdgeInfo)
  final def edgeInfo(name: UUID): EdgeInfo                      = getEdgeInfo(name).orThrow(new NoSuchElementException(s"edge $name"))
  final def containsEdge(name: UUID): Boolean                   = getEdgeId(name).isDefined

  final def outEdgeInfos(srcName: UUID): View[EdgeInfo]                      = getNodeId(srcName).view.flatMap(outEdgeInfos)
  final def outEdgeInfos(srcId: Long, group: Group): View[EdgeInfo]          = outEdgeInfos(srcId).filter(_.group == group)
  final def outEdgeInfos(srcName: UUID, group: Group): View[EdgeInfo]        = outEdgeInfos(srcName).filter(_.group == group)
  final def outEdgeAttrs(srcName: UUID, group: Group): View[EdgeAttrs]       = outEdgeAttrs(srcName).filter(_.group == group)
  final def outEdgeAttrs(srcName: UUID, groups: Set[Group]): View[EdgeAttrs] =
    outEdgeAttrs(srcName).filter(e => groups.contains(e.group))
  final def inEdgeInfos(tgtId: Long, group: Group): View[EdgeInfo]           = inEdgeInfos(tgtId).filter(_.group == group)
  final def inEdgeInfos(tgtName: UUID, group: Group): View[EdgeInfo]         = inEdgeInfos(tgtName).filter(_.group == group)
  final def inEdgeAttrs(tgtName: UUID, group: Group): View[EdgeAttrs]        = inEdgeAttrs(tgtName).filter(_.group == group)
end ReadWorkspace

// a ReadWorkspace that is attached to some branch or project
// would rename to AttachedWorkspace if it didn't change 1000 files (as there are read and write subtypes)
trait AttachedReadWorkspace extends ReadWorkspace:
  def bronchId: Long
  def rootName: UUID
  def homeName: UUID
  def projectInfo: Project
  // real or fake depending on layered
  def branch: Branch // we've come full circle now

class LayeredWorkspace(final val commit: Commit2, final val doc: Commit2.ComboDoc, docEdgeAttrs: Map[UUID, EdgeAttrs])
    extends EdgeElementContainer(docEdgeAttrs)
    with ReadWorkspace:
  override final val commitId: Long         = commit.id
  override final val created: LocalDateTime = commit.created
  override final val createdBy: Long        = commit.createdBy.getOrElse(0)
  override final val layered: Boolean       = true

  final val rootName = commit.rootName
  final val homeName = commit.homeName

  override final def getNodeId(name: UUID): Option[Long]       = doc.getNodeId(name)
  override final def getNodeName(id: Long): Option[UUID]       = doc.getNodeName(id)
  override final def getNodeElem(name: UUID): Option[Elem]     = doc.getNodeElem(name)
  override final lazy val nodeNameIds: View[(UUID, Long)]      = doc.nodeNameIds
  override final lazy val nodeNames: View[UUID]                = doc.nodeNameIds.map(_._1)
  override final lazy val nodeElems: View[Elem]                = doc.nodeElems
  override final lazy val docEdgeIds: View[Long]               = doc.docEdgeIds
  override final def getFuzzyEdgeId(name: UUID): Option[Long]  = doc.getFuzzyEdgeId(name)
  override final lazy val fuzzyEdgeNameIds: View[(UUID, Long)] = doc.fuzzyEdgeNameIds

  final def depInfos: Map[ProjectId, Commit2.Dep]             = doc.deps
  final def getLayer(projectId: Long): Option[LayerWithEdges] = doc.getLayer(projectId, docEdgeAttrs)
  final lazy val localLayer: LayerWithEdges                   = new LayerWithEdges(doc.localLayer, docEdgeAttrs)
end LayeredWorkspace

class ProjectWorkspace(
  final val project: Project2,
  commit: Commit2,
  doc: ComboDoc,
  docEdgeAttrs: Map[UUID, EdgeAttrs]
) extends LayeredWorkspace(commit, doc, docEdgeAttrs)
    with AttachedReadWorkspace:
  final val bronchId: Long       = project.id
  final val projectInfo: Project = project
  final val branch: Branch       = project.asBranch
end ProjectWorkspace
