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

package loi.authoring.project

import loi.authoring.edge.{EdgeAttrs, EdgeElem, Group}
import loi.authoring.project.Commit2.Elem
import loi.authoring.workspace.EdgeInfo

import java.util.UUID
import scala.collection.View

trait ElementContainer:

  def getNodeId(name: UUID): Option[Long]
  def getNodeName(id: Long): Option[UUID]
  def getNodeElem(name: UUID): Option[Elem]
  def nodeNameIds: View[(UUID, Long)]
  def nodeNames: View[UUID]
  def nodeElems: View[Elem]

  def docEdgeIds: View[Long]
  def getFuzzyEdgeId(name: UUID): Option[Long]
  def fuzzyEdgeNameIds: View[(UUID, Long)]

  // ---- derived methods ----
  final def fuzzyEdgeNames: View[UUID]       = fuzzyEdgeNameIds.map(_._1)
  final def toFuzzyLayerBase: FuzzyLayerBase = FuzzyLayerBase(nodeNameIds.toMap, fuzzyEdgeNameIds.toMap)
end ElementContainer

abstract class EdgeElementContainer(final val docEdgeAttrs: Map[UUID, EdgeAttrs]) extends ElementContainer:

  private final lazy val docEdgeAttrsBySrc: Map[UUID, Iterable[EdgeAttrs]] = docEdgeAttrs.values.groupBy(_.srcName)
  private final lazy val docEdgeAttrsByTgt: Map[UUID, Iterable[EdgeAttrs]] = docEdgeAttrs.values.groupBy(_.tgtName)

  final lazy val rootIds: Map[UUID, Long] =
    val tgtNames = edgeAttrs.map(_.tgtName).toSet
    nodeNameIds.filterNot({ case (name, _) => tgtNames.contains(name) }).toMap

  final def getDocEdgeAttrs(name: UUID): Option[EdgeAttrs] = docEdgeAttrs.get(name)

  final def getFuzzyEdgeAttrs(name: UUID): Option[EdgeAttrs] =
    docEdgeAttrs.get(name).filter(_ => getFuzzyEdgeId(name).isDefined)

  final def getFuzzyEdgeElem(name: UUID): Option[EdgeElem] = for
    id    <- getFuzzyEdgeId(name)
    attrs <- docEdgeAttrs.get(name)
  yield EdgeElem(id, attrs)

  final def getEdgeId(name: UUID): Option[Long] = for
    id    <- getFuzzyEdgeId(name)
    attrs <- docEdgeAttrs.get(name)
    if isExpressed(attrs)
  yield id

  final def getEdgeInfo(name: UUID): Option[EdgeInfo] = getFuzzyEdgeId(name).flatMap(toEdgeInfo(name, _))

  final def getEdgeAttrs(name: UUID): Option[EdgeAttrs] =
    getFuzzyEdgeId(name).flatMap(_ => docEdgeAttrs.get(name)).filter(isExpressed)

  final def getEdgeElem(name: UUID): Option[EdgeElem] = for
    id    <- getFuzzyEdgeId(name)
    attrs <- docEdgeAttrs.get(name)
    if isExpressed(attrs)
  yield EdgeElem(id, attrs)

  final lazy val edgeIds: View[Long]        = fuzzyEdgeNameIds.filter(isExpressed).map(_._2)
  final lazy val edgeNames: View[UUID]      = fuzzyEdgeNameIds.filter(isExpressed).map(_._1)
  final lazy val edgeAttrs: View[EdgeAttrs] = fuzzyEdgeNameIds.map(_._1).flatMap(docEdgeAttrs.get).filter(isExpressed)
  final lazy val edgeInfos: View[EdgeInfo]  = fuzzyEdgeNameIds.flatMap((toEdgeInfo).tupled)

  final def fuzzyOutEdgeAttrs(srcName: UUID): View[EdgeAttrs] = for
    attrs <- docEdgeAttrsBySrc.getOrElse(srcName, Nil).view
    id    <- getFuzzyEdgeId(attrs.name) // ensures not excluded
  yield attrs

  final def fuzzyOutEdgeElems(srcName: UUID): View[EdgeElem] = for
    attrs <- docEdgeAttrsBySrc.getOrElse(srcName, Nil).view
    id    <- getFuzzyEdgeId(attrs.name) // also ensures not excluded
  yield EdgeElem(id, attrs)

  final def outEdgeInfos(srcId: Long): View[EdgeInfo] = for
    srcName <- getNodeName(srcId).view    // also ensures expression
    attrs   <- docEdgeAttrsBySrc.getOrElse(srcName, Nil)
    id      <- getFuzzyEdgeId(attrs.name) // also ensures not excluded
    tgtId   <- getNodeId(attrs.tgtName)   // also ensures expression
  yield EdgeInfo(id, attrs, srcId, tgtId)

  final def outEdgeAttrs(srcName: UUID): View[EdgeAttrs] = for
    attrs <- docEdgeAttrsBySrc.getOrElse(srcName, Nil).view
    id    <- getFuzzyEdgeId(attrs.name) // ensures not excluded
    if isExpressed(attrs)
  yield attrs

  final def outEdgeElems(srcName: UUID): View[EdgeElem] = for
    attrs <- docEdgeAttrsBySrc.getOrElse(srcName, Nil).view
    id    <- getFuzzyEdgeId(attrs.name) // also ensures not excluded
    if isExpressed(attrs)
  yield EdgeElem(id, attrs)

  final def inEdgeInfos(tgtId: Long): View[EdgeInfo] = for
    tgtName <- getNodeName(tgtId).view    // also ensures expression
    attrs   <- docEdgeAttrsByTgt.getOrElse(tgtName, Nil)
    id      <- getFuzzyEdgeId(attrs.name) // also ensures not excluded
    srcId   <- getNodeId(attrs.srcName)   // also ensures expression
  yield EdgeInfo(id, attrs, srcId, tgtId)

  final def inEdgeInfos(tgtName: UUID): View[EdgeInfo] = for
    tgtId <- getNodeId(tgtName).view    // also ensures expression
    attrs <- docEdgeAttrsByTgt.getOrElse(tgtName, Nil)
    id    <- getFuzzyEdgeId(attrs.name) // also ensures not excluded
    srcId <- getNodeId(attrs.srcName)   // also ensures expression
  yield EdgeInfo(id, attrs, srcId, tgtId)

  final def inEdgeAttrs(tgtName: UUID): View[EdgeAttrs] = for
    attrs <- docEdgeAttrsByTgt.getOrElse(tgtName, Nil).view
    id    <- getFuzzyEdgeId(attrs.name) // ensures not excluded
    if isExpressed(attrs)
  yield attrs

  final def inEdgeElems(tgtName: UUID): View[EdgeElem] = for
    attrs <- docEdgeAttrsByTgt.getOrElse(tgtName, Nil).view
    id    <- getFuzzyEdgeId(attrs.name) // also ensures not excluded
    if isExpressed(attrs)
  yield EdgeElem(id, attrs)

  // ---- derived methods ----
  final def isRoot(name: UUID): Boolean                                  = rootIds.contains(name)
  final def fuzzyOutEdgeElems(srcName: UUID, grp: Group): View[EdgeElem] =
    fuzzyOutEdgeElems(srcName).filter(_.grp == grp)
  final def outEdgeElems(srcName: UUID, grp: Group): View[EdgeElem]      = outEdgeElems(srcName).filter(_.grp == grp)
  final def outEdgeGroups(srcName: UUID): Set[Group]                     = outEdgeAttrs(srcName).map(_.group).toSet

  private def isExpressed(attrs: EdgeAttrs): Boolean          =
    getNodeId(attrs.srcName).isDefined && getNodeId(attrs.tgtName).isDefined
  private def isExpressed(fuzzyNameId: (UUID, Long)): Boolean = fuzzyNameId match
    case (name, id) => docEdgeAttrs.get(name).exists(isExpressed)

  // also functions as an isExpressed filter
  private def toEdgeInfo(name: UUID, fuzzyId: Long): Option[EdgeInfo] = for
    attrs <- docEdgeAttrs.get(name)
    srcId <- getNodeId(attrs.srcName)
    tgtId <- getNodeId(attrs.tgtName)
  yield EdgeInfo(fuzzyId, attrs, srcId, tgtId)
end EdgeElementContainer
