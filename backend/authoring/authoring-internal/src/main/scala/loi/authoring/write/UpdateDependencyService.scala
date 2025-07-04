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

import argonaut.Json
import argonaut.JsonIdentity.*
import cats.syntax.either.*
import cats.syntax.flatMap.*
import cats.syntax.option.*
import cats.syntax.traverse.*
import com.fasterxml.jackson.databind.node.{JsonNodeFactory, ObjectNode}
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.service.domain.{DomainDTO, DomainFinder}
import com.learningobjects.cpxp.util.HibernateSessionOps.*
import de.tomcat.juli.LogMeta
import loi.authoring.AssetType
import loi.authoring.edge.store.{EdgeDao2, EdgeEntity2}
import loi.authoring.edge.{EdgeElem, Group}
import loi.authoring.node.store.{NodeDao2, NodeEntity2}
import loi.authoring.project.*
import loi.authoring.validate.ValidationService
import loi.authoring.workspace.ProjectWorkspace
import loi.authoring.write.DeclineReason.{DuplicatesTgt, MergeAbort, TooManyEdges, WeExclude}
import loi.authoring.write.LayeredWriteService.*
import loi.authoring.write.SyncState.SyncStateT
import loi.authoring.write.store.DbUpdateDependency
import loi.cp.asset.edge.EdgeData
import loi.jackson.syntax.any.*
import loi.jackson.syntax.jsonNode.*
import org.hibernate.Session
import scaloi.syntax.collection.*

import java.time.LocalDateTime
import java.util.UUID
import scala.collection.immutable.VectorMap
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

/** Utility service of LayeredWriteService. Could be `object` but for wiring @Services.
  */
@Service
class UpdateDependencyService(
  edgeDao2: EdgeDao2,
  nodeDao2: NodeDao2,
  syncReportDao: SyncReportDao,
  validationService: ValidationService,
  commitTime: => LocalDateTime,
  domainDto: => DomainDTO,
  session: => Session,
):

  def syncTransformation(
    ourWs: LayeredWriteWorkspace,
    ourLayer: LayerWithEdges,
    base: FuzzyLayerBase,
    theirWs: ProjectWorkspace
  ): SyncStateT[Unit] = for
    _        <- collectSyncActions(ourWs, ourLayer, base, theirWs)
    nodeReqs <- SyncStateT.inspect(_.nodeReqs)
    edgeReqs <- SyncStateT.inspect(_.edgeReqs)
    nodes     = nodeDao2.load(nodeReqs).groupUniqBy(_.id) // nodes needed to complete the sync
    edges     = edgeDao2.load(edgeReqs).groupUniqBy(_.id) // edges needed to complete the sync
    actions  <- SyncStateT.inspect(_.actions)
    _        <- actions.mergeNodes.traverse(mergeNode(nodes, theirWs))
    _        <- actions.mergeEdges.traverse(mergeEdge(edges, theirWs))
    _        <- actions.spreadGroups.traverse(spreadGroup(edges, theirWs))
    _        <- recordSyncDbWriteOp(theirWs)
  yield ()

  /** Collect sync actions for everything in our layer. Stores them in SyncState.
    */
  private def collectSyncActions(
    ourWs: LayeredWriteWorkspace,
    ourLayer: LayerWithEdges,
    base: FuzzyLayerBase,
    theirWs: ProjectWorkspace
  ): SyncStateT[Unit] =

    def processNodes(nodeNames: Seq[UUID]): SyncStateT[Either[Seq[UUID], Unit]] =
      nodeNames.flatTraverse(processNode).map {
        case Nil  => ().asRight  // Right tells tailRecM to stop iterating
        case more => more.asLeft // Left tails tailRecM to continue
      }

    def processNode(nodeName: UUID): SyncStateT[Seq[UUID]] =
      SyncStateT
        .inspect(_.visitedNodeNames.contains(nodeName))
        .ifM(
          SyncStateT.pure(Nil),
          for
            _          <- collectNodeAction(nodeName, ourLayer, base, theirWs)
            theirGroups = theirWs.outEdgeGroups(nodeName).toSeq
            children   <- theirGroups.flatTraverse(processGroup(nodeName, ourWs, ourLayer, base, theirWs))
          yield children
        )

    // `processNodes` returns children that need to be processed and so on until no more children.
    // any function that returns more input for itself is ripe for `tailRecM`
    ourLayer.nodeNames.toSeq.tailRecM(processNodes)
  end collectSyncActions

  private def collectNodeAction(
    name: UUID,
    ourLayer: LayerWithEdges,
    base: FuzzyLayerBase,
    theirWs: ProjectWorkspace
  ): SyncStateT[Unit] =
    SyncStateT.modify { s =>
      val ourId   = ourLayer.getNodeId(name) // None if we excluded
      val baseId  = base.nodeIds.get(name)   // None if `name` not present when we last claimed
      val theirId = theirWs.getNodeId(name)  // None if they excluded `name`, but OmitNode handled elsewhere

      (ourId, baseId, theirId) match
        case (Some(ourId), Some(baseId), Some(theirId)) if ourId != baseId && theirId != baseId =>
          // we customized and they updated
          s.withMergeNode(name, ourId, baseId, theirId)
        case (Some(_), Some(baseId), Some(theirId)) if theirId != baseId                        =>
          // we did not customize and they updated
          s.withFastForwardNode(name, theirId, theirWs.project.id)
        case (None, Some(baseId), Some(theirId)) if baseId != theirId                           =>
          // they updated, we excluded, decline their change
          // CONSIDER: merge their change anyway while still excluding it, so that recovery does not recover a stale element
          s.withDeclineNode(DeclineNode(name, theirId, WeExclude))
        case (None, None, Some(theirId))                                                        =>
          // they created a new descendant of something that we already use
          s.withClaimNode(name, theirId, theirWs.project.id)
        case _                                                                                  =>
          // no-action/no-decline/impossible cases
          // (Some, Some, Some) they did not update, no action to take and nothing to decline
          // (None, Some, Some) they did not update, no action to take and nothing to decline (we excluded irrelevant)
          // (Some, None, Some) impossible(?), perhaps possible if depending on blue and a blue-shallow-clone, don't care
          // (None, Some, None) they excluded, OmitNode handled elsewhere
          // (Some, None, None) impossible, if `name` not present when last we claimed, how is ours Some?
          // (Some, Some, None) they excluded, OmitNode handled elsewhere
          // (None, None, None) impossible, where did `name` come from if not from one of the 3 parts?
          s.copy(visitedNodeNames = s.visitedNodeNames.incl(name))
      end match
    }

  private def processGroup(
    srcName: UUID,
    ourWs: LayeredWriteWorkspace,
    ourLayer: LayerWithEdges,
    base: FuzzyLayerBase,
    theirWs: ProjectWorkspace
  )(grp: Group): SyncStateT[Seq[UUID]] =

    // this only works because we visit each `srcName-grp` once - that is, InsertState has no InsertEdge/ClaimEdge/ExcludeEdge
    // for any of our edges because this is the first and only time we visit this `srcName-grp`. Thus, I can get our members
    // from our workspace (minus omitted members).
    val getOurMembers = SyncStateT.inspect(s =>
      ourWs
        .fuzzyOutEdgeElems(srcName, grp)
        .filterNot(e => s.insertState.isPendingOmitEdge(e.name))
        .toSeq
        .sortBy(_.position)
        .view
        .map(e => e.tgtName -> e) // for fast lookup by tgtName in this incredibly massive group /s
        .to(VectorMap) // to maintain position-ascending values iteration
    )

    def collectSpreads(ourMembers: Map[UUID, EdgeElem], theirMembers: Seq[EdgeElem]) = SyncStateT.modify { s =>
      // fold to Map so that ours overwrites theirs, which emulates what InsertState will actually do
      val zero       = VectorMap.empty[UUID, EdgeElem]
      val allMembers = theirMembers.concat(ourMembers.values).foldLeft(zero) { case (acc, e) => acc.updated(e.name, e) }

      val willCollideInPosition = allMembers.values.view.map(_.position).toSet.size != allMembers.size
      if willCollideInPosition then s.withSpreadGroup(srcName, grp, allMembers.values.toList) else s
    }

    // Boilerplate because .flatTraverse cannot implicitly convert G[Option[B]] to G[Seq[B]]
    def processTheirEdge(ourMembers: Map[UUID, EdgeElem])(theirs: EdgeElem): SyncStateT[Seq[EdgeElem]] =
      collectEdgeAction(ourMembers, ourLayer, base, theirWs)(theirs).map(_.toSeq)

    for
      ourMembers            <- getOurMembers
      theirMembers           = theirWs.outEdgeElems(srcName, grp).toSeq.sortBy(_.position)
      theirSurvivingMembers <- theirMembers.flatTraverse(processTheirEdge(ourMembers))
      _                     <- collectSpreads(ourMembers, theirSurvivingMembers)
      children              <- SyncStateT.inspect(s => theirSurvivingMembers.flatMap(s.getClaimTraversableTgtName))
    yield children
  end processGroup

  /** Collect the sync action for theirEdge on SyncState. Return their edge if it survives (i.e. not declined)
    */
  private def collectEdgeAction(
    ourGroup: Map[UUID, EdgeElem],
    ourLayer: LayerWithEdges,
    base: FuzzyLayerBase,
    theirWs: ProjectWorkspace
  )(
    theirs: EdgeElem
  ): SyncStateT[Option[EdgeElem]] = SyncStateT.apply { s =>
    val ours   = ourLayer.getFuzzyEdgeElem(theirs.name)
    val baseId = base.fuzzyEdgeIds.get(theirs.name)

    lazy val theirsDuplicatesTgt = ourGroup.get(theirs.tgtName).exists(_.name != theirs.name)
    lazy val groupTooBig         = theirs.grp.cardinality.exists(_ < ourGroup.size + 1)

    def noAction                            = (s, theirs.some)
    def decline(why: DeclineReason)         =
      (s.withDeclineEdge(DeclineEdge(theirs.name, theirs.srcName, theirs.id, why)), None)
    def fastForward                         = (s.withFastForwardEdge(theirs, theirWs.project.id, createDurableEdge = true), theirs.some)
    def claim(createDur: Boolean)           = (s.withClaimEdge(theirs, theirWs.project.id, createDur), theirs.some)
    def merge(ours: EdgeElem, baseId: Long) = (s.withMergeEdge(theirs.name, ours, baseId, theirs), theirs.some)

    (ours, baseId) match
      case (_, Some(baseId)) if theirs.id == baseId        => noAction
      case (None, Some(_))                                 => decline(WeExclude)
      case (_, _) if theirsDuplicatesTgt                   => decline(DuplicatesTgt)
      case (_, None) if groupTooBig /*theirs is new */     => decline(TooManyEdges)
      case (_, None) /* theirs is new  */                  => fastForward
      case (Some(ours), Some(baseId)) if ours.id == baseId => claim(ours.tgtName != theirs.tgtName)
      case (Some(ours), Some(baseId))                      => merge(ours, baseId)
  }

  private def mergeNode(nodes: Map[java.lang.Long, NodeEntity2], theirWs: ProjectWorkspace)(merge: MergeNode) =
    SyncStateT.modify { s =>
      val MergeNode(_, ourId, baseId, theirId) = merge

      val insertNode = for
        ours       <- nodes.get(ourId).toRight(s"our node $ourId not loaded")
        base       <- nodes.get(baseId).toRight(s"base node $baseId not loaded")
        theirs     <- nodes.get(theirId).toRight(s"their node $theirId not loaded")
        ourData    <- ours.data.toObjNode.toRight(s"our ${ours.data.getNodeType} not mergeable")
        baseData   <- base.data.toObjNode.toRight(s"base ${base.data.getNodeType} not mergeable")
        theirData  <- theirs.data.toObjNode.toRight(s"their ${theirs.data.getNodeType} not mergeable")
        assetType   = NodeDao2.assetTypeOrThrow(ours)
        mergedData  = mergeNodeData(ourData, baseData, theirData)
        insertNode <- createInsertNode(ours, ourData, mergedData, assetType, merge, theirWs.project.id)
      yield insertNode

      insertNode match
        case Right(n)  => s.acceptMerge(n)
        case Left(err) => s.rejectMerge(merge, MergeAbort(err))

    }

  private def mergeNodeData(
    ourData: ObjectNode,
    baseData: ObjectNode,
    theirData: ObjectNode
  ): ObjectNode =
    // what could possibly go wrong
    // FIXME create AssetType.merge so each type says what properties merge, and how to do it.
    val ourChanges    = JsonNodeFactory.instance.objectNode()
    val ourFieldNames = mutable.Set.empty[String]
    ourData.properties.asScala
      .foreach(entry =>
        ourFieldNames.add(entry.getKey)
        if baseData.path(entry.getKey) != entry.getValue then ourChanges.set(entry.getKey, entry.getValue)
      )

    val ourRemoves = baseData.fieldNames().asScala.filterNot(ourFieldNames.contains).toSet.asJava
    theirData.setAll[ObjectNode](ourChanges).without[ObjectNode](ourRemoves)
  end mergeNodeData

  private def createInsertNode[A](
    ours: NodeEntity2,
    prevData: ObjectNode,
    mergedData: ObjectNode,
    assetType: AssetType[A],
    merge: MergeNode,
    projectId: Long
  ): Either[String, InsertNode[A]] =

    val merged = Either.catchNonFatal {
      // can't use finatra because ancient pre-finatra data may be finatra-invalid
      val prevA   = JacksonUtils.getMapper.treeToValue(prevData, assetType.dataClass)
      val mergedA = JacksonUtils.getMapper.treeToValue(mergedData, assetType.dataClass)

      // fscking pool assessments
      // FIXME or don't bother, who cares
      // This check should devolve to a warning in the PoolAssessment editor, it should not block saves
      // maybe the user intends to add 10 questions and are just setting the question count first.
      val groupSizes = Map(Group.Questions.asInstanceOf[Group] /* fscking scala */ -> 42000)

      validationService.updateValidate(prevA, mergedA, groupSizes, None)(using assetType).get
      InsertNode(projectId, ours.name, mergedA, assetType, ours.created)
    }

    merged.leftMap(ex =>
      LogMeta.let(
        "nodeName" -> Json.jString(merge.name.toString),
        "ourId"    -> Json.jNumber(merge.ourId),
        "baseId"   -> Json.jNumber(merge.baseId),
        "theirId"  -> Json.jNumber(merge.theirId)
      ) {
        logger.warn(ex)("element merge aborted; taking our side without merge")
      }
      ex.getMessage
    )
  end createInsertNode

  private def mergeEdge(edges: Map[java.lang.Long, EdgeEntity2], theirWs: ProjectWorkspace)(merge: MergeEdge) =
    SyncStateT.modify { s =>
      val MergeEdge(_, _, ourId, baseId, theirId) = merge

      val insertEdge = for
        ours       <- edges.get(ourId).toRight(s"our edge $ourId not loaded")
        base       <- edges.get(baseId).toRight(s"base edge $baseId not loaded")
        theirs     <- edges.get(theirId).toRight(s"their edge $theirId not loaded")
        insertEdge <- createInsertEdge(ours, base, theirs, theirWs.project.id)
      yield insertEdge

      insertEdge match
        case Right((e, createDurable)) => s.acceptMerge(e, createDurable)
        case Left(e)                   => s.rejectMerge(merge, MergeAbort(e))
    }

  private def createInsertEdge(
    ours: EdgeEntity2,
    base: EdgeEntity2,
    theirs: EdgeEntity2,
    projectId: Long,
  ): Either[String, (InsertEdge, Boolean)] =

    val insertEdge = Either.catchNonFatal {

      // note well. there is a legacy auto-upgrader in EdgeData's deserializer
      val ourData   = ours.data.finatraDecoded[EdgeData]
      val baseData  = base.data.finatraDecoded[EdgeData]
      val theirData = theirs.data.finatraDecoded[EdgeData]

      val ourChanges    = JsonNodeFactory.instance.objectNode()
      val ourFieldNames = mutable.Set.empty[String]
      ourData.node.properties.asScala
        .foreach(entry =>
          if baseData.node.path(entry.getKey) != entry.getValue then ourChanges.set(entry.getKey, entry.getValue)
        )

      val ourRemoves    = baseData.node.fieldNames.asScala.filterNot(ourFieldNames.contains).toSet.asJava
      val mergedObjNode = theirData.node.setAll[ObjectNode](ourChanges).without[ObjectNode](ourRemoves)
      val mergedData    = EdgeData(mergedObjNode)

      def oursOrTheirs[A](f: EdgeEntity2 => A): A =
        val baseA   = f(base)
        val oursA   = f(ours)
        val theirsA = f(theirs)
        if oursA == baseA && theirsA != baseA then theirsA else oursA

      InsertEdge(
        projectId,
        ours.name,
        ours.sourceName,
        oursOrTheirs(_.targetName),
        null /* only important for List[WriteOp], and sync bypasses that */,
        Group.withName(ours.group),
        oursOrTheirs(_.position) /* any collision will be resolved later */,
        oursOrTheirs(_.traverse),
        oursOrTheirs(_.localId),
        mergedData,
        ours.created
      )
    }

    insertEdge match
      case Right(e) =>
        val createDurableEdge = e.tgtName != ours.targetName
        Right(e -> createDurableEdge)
      case Left(ex) =>
        LogMeta.let(
          "edgeName" -> Json.jString(ours.name.toString),
          "ourId"    -> Json.jNumber(ours.id),
          "baseId"   -> Json.jNumber(base.id),
          "theirId"  -> Json.jNumber(theirs.id)
        ) {
          logger.warn(ex)("element merge aborted; taking our side without merge")
        }
        Left(ex.getMessage)
    end match
  end createInsertEdge

  private def spreadGroup(edges: Map[java.lang.Long, EdgeEntity2], theirWs: ProjectWorkspace)(
    action: SpreadGroup
  ) =
    SyncStateT.modify { s =>
      val members = action.members.flatMap(m =>
        val pending       = s.insertState.getInsertEdge(m.name).asInstanceOf[Option[ElementalEdge]] // widen
        lazy val existing =
          edges.get(m.id).map(e => ExistingEdge(theirWs.project.id, e, null, s.insertState.isExpressed(e)))
        pending.orElse(existing)
      )

      s.copy(insertState = LayeredWriteService.spreadPositions(members).runS(s.insertState).value)
    }

  private def recordSyncDbWriteOp(theirWs: ProjectWorkspace): SyncStateT[Unit] = SyncStateT.modify { s =>
    val project = session.ref[ProjectEntity2](s.insertState.parentWs.project.id)
    val root    = session.ref[DomainFinder](domainDto.id)
    val report  = SyncReport.fromSyncActions(s.actions)

    val reportEntity = syncReportDao.persist(new SyncReportEntity(project, commitTime, report.asJson, root))

    val op = DbUpdateDependency(
      theirWs.project.id,
      theirWs.project.name,
      theirWs.project.code,
      s.actions.narrativelyUpdatedNodeNames,
      s.actions.claimNodes,
      s.actions.omitNodes,
      reportEntity.id
    )
    s.copy(insertState = s.insertState.copy(dbOps = Vector(op.finatraEncoded)))
  }

  private val logger: org.log4s.Logger = org.log4s.getLogger
end UpdateDependencyService
