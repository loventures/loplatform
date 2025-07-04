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

import cats.syntax.option.*
import cats.syntax.semigroup.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{ArrayNode, JsonNodeFactory}
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.service.domain.DomainFinder
import com.learningobjects.cpxp.service.user.UserFinder
import com.learningobjects.cpxp.util.HibernateSessionOps.*
import loi.authoring.ProjectId
import loi.authoring.project.{Commit2, CommitDocEntity, CommitEntity2}
import loi.authoring.write.LayeredWriteService.InsertState
import loi.authoring.write.store.{DbAddNode, DbSetNodeData, DbWriteOp}
import mouse.boolean.*
import org.hibernate.Session
import scaloi.syntax.any.*
import scaloi.syntax.boolean.*

import java.time.LocalDateTime
import java.util.UUID
import scala.collection.immutable.VectorMap
import scala.jdk.CollectionConverters.*

/** Builds the next authoringcommit.
  */
final case class NextCommit(
  parentWs: LayeredWriteWorkspace,
  localBuilder: LayerBuilder,
  depBuilders: Map[ProjectId, LayerBuilder],
  rootName: UUID,
  homeName: UUID,
  dbOps: Vector[JsonNode],
  forceKeyframe: Boolean,
  maybeSquash: Boolean,
):

  private def findApply(projectId: Long, f: LayerBuilder => LayerBuilder): NextCommit =
    def applyLocal                = copy(localBuilder = f(localBuilder))
    def applyDep(d: LayerBuilder) = copy(depBuilders = depBuilders.updated(projectId, f(d)))

    if projectId == 0 then applyLocal else depBuilders.get(projectId).map(applyDep).getOrElse(applyLocal)

  def withNode(name: UUID, id: Long, projectId: Long): NextCommit = findApply(projectId, _.withNode(name, id))
  def withEdge(name: UUID, id: Long, projectId: Long): NextCommit = findApply(projectId, _.withEdge(name, id))
  def withNodeOmitted(name: UUID, projectId: Long): NextCommit    = findApply(projectId, _.withNodeOmitted(name))
  def withEdgeOmitted(name: UUID, projectId: Long): NextCommit    = findApply(projectId, _.withEdgeOmitted(name))

  def withNodeExcluded(name: UUID, id: Long, projectId: Long): NextCommit =
    findApply(projectId, _.withNodeExcluded(name, id))

  def withEdgeExcluded(name: UUID, id: Long, projectId: Long): NextCommit =
    findApply(projectId, _.withEdgeExcluded(name, id))

  private lazy val addendDoc: Commit2.Doc =
    val docNodes = Map.newBuilder[Long, Map[UUID, Long]]
    val docEdges = Map.newBuilder[Long, Map[UUID, Long]]

    docNodes.addOne(0L -> localBuilder.nextNodes)
    docEdges.addOne(0L -> localBuilder.nextEdges)

    for (projectId, builder) <- depBuilders
    do
      docNodes.addOne(projectId, builder.nextNodes)
      docEdges.addOne(projectId, builder.nextEdges)

    Commit2.Doc(docNodes.result(), docEdges.result(), Map.empty)
  end addendDoc

  def result(
    session: Session,
    created: LocalDateTime,
    createdBy: UserFinder,
    root: DomainFinder,
  ): CommitEntity2 =

    val parentCommitEntity = session.ref[CommitEntity2](parentWs.commitId)

    val (parentEntity, dbOpVector) =
      maybeSquash
        .flatOption(SquashCommitOps(parentCommitEntity, dbOps))
        .getOrElse(parentCommitEntity, dbOps)

    val driftDoc = parentWs.driftDoc.orEmpty |+| addendDoc

    // very scientific this
    val nextIsKeyframe = forceKeyframe || driftDoc.elemSize > 64

    val nextDriftDoc = if nextIsKeyframe then null else new CommitDocEntity(driftDoc, root).tap(session.persist)

    val nextKfDoc = if nextIsKeyframe then
      val doc1 = parentWs.kfDoc |+| driftDoc
      val doc2 = doc1.excludedLocalElemsRemoved
      val doc3 = depBuilders.foldLeft(doc2) { case (acc, (projectId, layer)) =>
        acc.elemsRemoved(projectId, layer.omitNodes, layer.omitEdges)
      }
      val doc4 = doc3.withDepInfos(depBuilders.view.mapValues(_.nextDepInfo).toMap)
      new CommitDocEntity(doc4, root).tap(session.persist)
    else parentCommitEntity.kfDoc

    new CommitEntity2(
      null,
      rootName,
      homeName,
      created,
      createdBy,
      parentEntity,
      nextKfDoc,
      nextDriftDoc,
      JsonNodeFactory.instance.arrayNode(dbOpVector.size).addAll(dbOpVector.asJava),
      root
    ).tap(session.persist)
  end result

  // for CommitResult, not involved in entity building
  lazy val modifiedNodes: Map[UUID, Long] =
    depBuilders.values.foldLeft(localBuilder.modifiedNodes)(_ ++ _.modifiedNodes)
  lazy val modifiedEdges: Map[UUID, Long] =
    depBuilders.values.foldLeft(localBuilder.modifiedEdges)(_ ++ _.modifiedEdges)

  lazy val excludedNodes: Set[UUID] = depBuilders.values.foldLeft(localBuilder.excludedNodes)(_ ++ _.excludedNodes)
  lazy val excludedEdges: Set[UUID] = depBuilders.values.foldLeft(localBuilder.excludedEdges)(_ ++ _.excludedEdges)
end NextCommit

object NextCommit:
  def newBuilder(s: InsertState): NextCommit =

    val nextDepInfos = s.parentWs.depInfos ++ s.pendingDepInfos

    NextCommit(
      s.parentWs,
      LayerBuilder.emptyLocal,
      nextDepInfos.view.mapValues(LayerBuilder.emptyRemote).toMap,
      s.pendingRootName.getOrElse(s.parentWs.rootName),
      s.pendingHomeName.getOrElse(s.parentWs.homeName),
      s.dbOps,
      forceKeyframe = s.forceKeyframe,
      maybeSquash = s.maybeSquash,
    )
  end newBuilder
end NextCommit

object SquashCommitOps:
  def apply(parentCommit: CommitEntity2, dbOps: Vector[JsonNode]): Option[(CommitEntity2, Vector[JsonNode])] =
    for
      grandparentCommit <- Option(parentCommit.parent)
      if (parentCommit.driftDoc ne null) && (dbOps.length == 1)
      parentOps          = parentCommit.ops.asInstanceOf[ArrayNode]
      squashedOps       <- squash(dbOps.head, parentOps)
    yield grandparentCommit -> squashedOps

  private def squash(childOp: JsonNode, parentOps: ArrayNode): Option[Vector[JsonNode]] =
    parseDbWriteOp(childOp) match
      case setNodeData: DbSetNodeData =>
        val (success, newOps) = squashOp(setNodeData, parentOps)
        success.option(newOps)

      case _ => None

  private def squashOp(childSet: DbSetNodeData, parentOps: ArrayNode): (Boolean, Vector[JsonNode]) =
    parentOps.iterator.asScala.foldLeft(false -> Vector.empty[JsonNode]) { case ((success, ops), parentOp) =>
      parseDbWriteOp(parentOp) match
        case parentSet: DbSetNodeData if parentSet.name == childSet.name =>
          true -> (ops :+ JacksonUtils.getMapper.valueToTree(childSet))
        case parentAdd: DbAddNode if parentAdd.name == childSet.name     =>
          val mergedOp = parentAdd.copy(data = childSet.data)
          true -> (ops :+ JacksonUtils.getMapper.valueToTree(mergedOp))
        case _                                                           =>
          success -> (ops :+ parentOp)
    }

  private def parseDbWriteOp(node: JsonNode): DbWriteOp =
    JacksonUtils.getMapper.treeToValue(node, classOf[DbWriteOp])
end SquashCommitOps

final case class LayerBuilder(
  nextNodes: VectorMap[UUID, Long],
  nextEdges: VectorMap[UUID, Long],
  omitNodes: Set[UUID] /* for sync only, means name no longer present in dep */,
  omitEdges: Set[UUID] /* for sync only, means name no longer present in dep */,
  nextDepInfo: Commit2.Dep,
):
  def withNode(name: UUID, id: Long): LayerBuilder         = copy(nextNodes = nextNodes.updated(name, id))
  def withEdge(name: UUID, id: Long): LayerBuilder         = copy(nextEdges = nextEdges.updated(name, id))
  def withNodeExcluded(name: UUID, id: Long): LayerBuilder = withNode(name, -id)
  def withEdgeExcluded(name: UUID, id: Long): LayerBuilder = withEdge(name, -id)
  def withNodeOmitted(name: UUID): LayerBuilder            = copy(omitNodes = omitNodes.incl(name))
  def withEdgeOmitted(name: UUID): LayerBuilder            = copy(omitEdges = omitEdges.incl(name))

  lazy val modifiedNodes: Map[UUID, Long] = nextNodes.filter(_._2 > 0)
  lazy val modifiedEdges: Map[UUID, Long] = nextEdges.filter(_._2 > 0)
  lazy val excludedNodes: Set[UUID]       = nextNodes.filter(_._2 <= 0).keySet
  lazy val excludedEdges: Set[UUID]       = nextEdges.filter(_._2 <= 0).keySet
end LayerBuilder

object LayerBuilder:
  val emptyLocal: LayerBuilder                        = LayerBuilder(VectorMap.empty, VectorMap.empty, Set.empty, Set.empty, Commit2.Dep.local)
  def emptyRemote(depInfo: Commit2.Dep): LayerBuilder =
    LayerBuilder(VectorMap.empty, VectorMap.empty, Set.empty, Set.empty, depInfo)
