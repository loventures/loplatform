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

import argonaut.CodecJson
import cats.data.Ior
import cats.kernel.instances.MapMonoid
import cats.syntax.monoid.*
import cats.syntax.option.*
import cats.{Monoid, Semigroup}
import com.fasterxml.jackson.annotation.JsonIgnore
import loi.authoring.*
import loi.authoring.commit.Commit
import loi.authoring.edge.EdgeAttrs
import scaloi.json.ArgoExtras
import scaloi.syntax.localDateTime.*

import java.time.LocalDateTime
import java.util.UUID
import scala.collection.View

final case class Commit2(
  id: Long,
  rootName: UUID,
  homeName: UUID,
  created: LocalDateTime,
  createdBy: Option[Long],
  parentId: Option[Long],
  kfDocId: Long,
  driftDocId: Option[Long],
  rootId: Long
):

  @JsonIgnore
  lazy val asLegacy: Commit = Commit(id, created.asDate, createdBy.getOrElse(0L), Map.empty, parentId, rootId)
end Commit2

object Commit2:

  /** One class that serves two purposes:
    *   - A Json-decoded and authoring-api visible version of CommitDocEntity
    *   - The result of combining a keyframe CommitDocEntity and a drift CommitDocEntity.
    *
    * And splitting the purpose of this class furthermore: `deps` is always empty on drift documents. Dependencies are
    * not combinable (no way to subtract a dependency in Map[Long, Dep] form). The keyframe document provides the exact
    * dependency map.
    */
  // The inner elem maps are combinable with `++` in any order (no UUID collisions)
  final case class Doc(
    nodes: Map[ProjectId, Map[UUID, NodeId]],
    edges: Map[ProjectId, Map[UUID, EdgeId]],
    deps: Map[ProjectId, Dep],
  ):

    lazy val elemSize: Int = nodes.values.map(_.size).sum + edges.values.map(_.size).sum

    // for creating keyframes....
    // excluded elements in the local layer have different behavior than excluded elements in a remote layer
    // when creating keyframes:
    //   - excluded local elements are removed - like emptying the trash
    //   - excluded remote elements remain - they must continue hiding the remote element.
    lazy val excludedLocalElemsRemoved: Doc =
      val updatedLayerNodes = nodes.get(0).map(_.filter(_._2 > 0))
      val updatedLayerEdges = edges.get(0).map(_.filter(_._2 > 0))

      withUpdatedLayer(0, updatedLayerNodes, updatedLayerEdges)

    // for creating the keyframe for a sync....
    // remove elements that are no longer in the workspace of the dependency
    def elemsRemoved(projectId: Long, nodeKeys: Set[UUID], edgeKeys: Set[UUID]): Doc =
      val updatedLayerNodes = nodes.get(projectId).map(_.removedAll(nodeKeys))
      val updatedLayerEdges = edges.get(projectId).map(_.removedAll(edgeKeys))

      withUpdatedLayer(projectId, updatedLayerNodes, updatedLayerEdges)

    private def withUpdatedLayer(
      projectId: Long,
      updatedNodes: Option[Map[UUID, Long]],
      updatedEdges: Option[Map[UUID, Long]]
    ): Doc =
      val updatedDoc = Ior.fromOptions(updatedNodes, updatedEdges).map {
        case Ior.Left(nextNodes)            => copy(nodes = nodes.updated(projectId, nextNodes))
        case Ior.Right(nextEdges)           => copy(edges = edges.updated(projectId, nextEdges))
        case Ior.Both(nextNodes, nextEdges) =>
          copy(
            nodes = nodes.updated(projectId, nextNodes),
            edges = edges.updated(projectId, nextEdges)
          )
      }

      updatedDoc.getOrElse(this)
    end withUpdatedLayer

    // for creating keyframe docs...
    // only keyframe documents carry the dependency info
    def withDepInfos(deps: Map[Long, Commit2.Dep]): Doc = copy(deps = deps)
  end Doc

  object Doc:

    // also the document for the first commit of a new project
    val empty: Doc = Doc(Map(0L -> Map.empty), Map(0L -> Map.empty), Map.empty)

    def localOnly(
      nodes: Map[UUID, Long] = Map.empty,
      edges: Map[UUID, Long] = Map.empty,
      deps: Map[Long, Dep] = Map.empty
    ): Doc =
      Doc(Map(0L -> nodes), Map(0L -> edges), deps)

    // Cats' gives CommutativeMonoids higher implicit resolution priority than regular Monoids, and
    // Semigroup.last[Long] won't precede a CommutativeSemigroup[Long].
    // Result is I have to specify the Monoid entirely myself. Rude.
    val semigroupForMapMap: Semigroup[Map[Long, Map[UUID, Long]]] =
      new MapMonoid[Long, Map[UUID, Long]]()(using new MapMonoid[UUID, Long]()(using Semigroup.last[Long]))

    implicit val monoidForDoc: Monoid[Doc] = Monoid.instance(
      empty,
      (a, b) =>
        Doc(
          nodes = semigroupForMapMap.combine(a.nodes, b.nodes),
          edges = semigroupForMapMap.combine(a.edges, b.edges),
          deps = a.deps ++ b.deps
        )
    )
  end Doc

  final case class Dep(commitId: Long)

  object Dep:
    val local: Dep = Dep(0L) // i.e. the special non-dependency that is the local layer

    implicit val codecJsonForDep: CodecJson[Dep] = CodecJson.casecodec1(Dep.apply, ArgoExtras.unapply1)("commitId")

  final case class ComboDoc(
    kf: Doc,
    drift: Option[Doc]
  ) extends ElementContainer:

    private lazy val doc = kf |+| drift.orEmpty

    private lazy val projectIdByNodeName = projectIndex(_.nodes, _._1)
    private lazy val projectIdByEdgeName = projectIndex(_.edges, _._1)
    private lazy val projectIdByNodeId   = projectIndex(_.nodes, _._2)

    lazy val localLayer: Layer = constructLayer(0, Commit2.Dep.local)

    private lazy val layers: Map[ProjectId, Layer] = doc.deps.foldLeft(Map(0L -> localLayer)) {
      case (acc, (projectId, depInfo)) => acc.updated(projectId, constructLayer(projectId, depInfo))
    }

    def deps: Map[ProjectId, Commit2.Dep] = doc.deps
    def elemSize: Int                     = doc.elemSize

    def getLayer(projectId: Long): Option[Layer] = layers.get(projectId)

    def getLayer(projectId: Long, edges: Map[UUID, EdgeAttrs]): Option[LayerWithEdges] =
      getLayer(projectId).map(layer => new LayerWithEdges(layer, edges))

    def findLayerN(name: UUID): Option[Layer]       = projectIdByNodeName.get(name).flatMap(getLayer)
    def findLayerE(name: UUID): Option[Layer]       = projectIdByEdgeName.get(name).flatMap(getLayer)
    private def findLayerN(id: Long): Option[Layer] = projectIdByNodeId.get(id).flatMap(getLayer)

    private def projectIndex[A](
      getElems: Doc => Map[ProjectId, Map[UUID, Long]],
      key: ((UUID, Long)) => A
    ): Map[A, ProjectId] =

      val elems = getElems(doc)

      def index(projectId: Long): Map[A, ProjectId] =
        elems.get(projectId).orEmpty.map(t => key(t) -> projectId)

      doc.deps.keys.foldLeft(index(0))(_ ++ index(_))
    end projectIndex

    private def constructLayer(projectId: Long, depInfo: Commit2.Dep): Layer = new Layer(
      doc.nodes.get(projectId).orEmpty,
      doc.edges.get(projectId).orEmpty,
      projectId,
      depInfo
    )

    // ------------ ElementContainer implementations ------------
    override def getNodeId(name: UUID): Option[Long]   = findLayerN(name).flatMap(_.getNodeId(name))
    override def getNodeName(id: Long): Option[UUID]   = findLayerN(id).flatMap(_.getNodeName(id))
    override def getNodeElem(name: UUID): Option[Elem] = findLayerN(name).flatMap(_.getNodeElem(name))
    override lazy val nodeNameIds: View[(UUID, Long)]  = layers.view.flatMap(_._2.nodeNameIds)
    override lazy val nodeNames: View[UUID]            = layers.view.flatMap(_._2.nodeNames)
    override lazy val nodeElems: View[Elem]            = layers.view.flatMap(_._2.nodeElems)

    override lazy val docEdgeIds: View[Long]              = layers.view.flatMap(_._2.docEdgeIds)
    override def getFuzzyEdgeId(name: UUID): Option[Long] = findLayerE(name).flatMap(_.getFuzzyEdgeId(name))
    override def fuzzyEdgeNameIds: View[(UUID, Long)]     = layers.view.flatMap(_._2.fuzzyEdgeNameIds)
  end ComboDoc

  final case class Elem(name: UUID, id: Long, projectId: Long, excluded: Boolean):
    val isLocal: Boolean = projectId == 0

  object Elem:
    def incl(name: UUID, id: Long, projectId: Long): Elem = Elem(name, id, projectId, excluded = false)
end Commit2
