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

import cats.data.{State, StateT}
import cats.syntax.either.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*
import cats.syntax.traverse.*
import clots.syntax.boolean.*
import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.domain.{DomainDTO, DomainFinder}
import com.learningobjects.cpxp.service.user.{UserDTO, UserFinder}
import com.learningobjects.cpxp.util.HibernateSessionOps.*
import com.learningobjects.de.web.UncheckedMessageException
import loi.authoring.*
import loi.authoring.asset.Asset
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.edge.DurableEdgeService.ReplaceNames
import loi.authoring.edge.*
import loi.authoring.edge.store.{DurableEdgeDao2, EdgeDao2, EdgeEntity2}
import loi.authoring.index.{CommitInfo, ReindexService}
import loi.authoring.node.store.{NodeDao2, NodeEntity2}
import loi.authoring.project.*
import loi.authoring.validate.ValidationService
import loi.authoring.workspace.*
import loi.authoring.write.LayeredWriteService.ValError.*
import loi.authoring.write.LayeredWriteService.*
import loi.authoring.write.store.*
import loi.cp.asset.edge.EdgeData
import loi.cp.i18n.syntax.bundleMessage.*
import loi.cp.i18n.{AuthoringBundle, BundleMessage}
import loi.jackson.syntax.any.*
import loi.jackson.syntax.jsonNode.*
import org.hibernate.Session
import scaloi.syntax.collection.*
import scaloi.syntax.date.*
import scaloi.syntax.int.*
import scaloi.syntax.map.*
import scaloi.syntax.vectorMap.*

import java.time.LocalDateTime
import java.util.UUID
import scala.annotation.tailrec
import scala.collection.immutable.VectorMap
import scala.collection.mutable

@Service
class LayeredWriteService(
  commitTime: => LocalDateTime,
  domainDto: => DomainDTO,
  durableEdgeService: DurableEdgeService,
  durableEdgeDao2: DurableEdgeDao2,
  edgeService: BaseEdgeService,
  validationService: ValidationService,
  reindexService: ReindexService,
  updateDependencyService: UpdateDependencyService,
  workspaceService: WorkspaceService,
  projectDao: ProjectDao2,
  nodeDao2: NodeDao2,
  edgeDao2: EdgeDao2,
  session: => Session,
  userDto: => UserDTO,
):

  def commit(
    ws: LayeredWriteWorkspace,
    ops: List[WriteOp],
    squash: Boolean = false
  ): ValErrorOr[CommitResult[LayeredWriteWorkspace]] =
    if idempotent(ws, ops) then CommitResult.empty(ws).asRight
    else commit(ws, ops, domainDto, squash)

  private def idempotent(ws: LayeredWriteWorkspace, ops: List[WriteOp]): Boolean = ops forall {
    case snd @ SetNodeData(name, data, None) =>
      ws.getNodeId(name)
        .exists(id => nodeDao2.load(id).exists(entity => entity.toAssetA(using snd.assetType).data == data))
    case _                                   => false
  }

  /** overload for workspace copy, the new commit and all elements are ascribed to the given domain
    */
  def commit(
    ws: LayeredWriteWorkspace,
    ops: List[WriteOp],
    domain: DomainDTO,
    squash: Boolean,
  ): ValErrorOr[CommitResult[LayeredWriteWorkspace]] =
    validate(ws, ops, squash).map(s => insert(s, domain))

  def validate(
    ws: LayeredWriteWorkspace,
    ops: List[WriteOp],
    squash: Boolean = false,
  ): ValErrorOr[InsertState] =
    val rn = durableEdgeService.replaceNames(ws, ops)
    // Iteratively validate `ops` one span at a time - each span demarcated by a workspace-expanding AddEdge.
    // The genesis of this FP hell is to chunk `ops` to more efficiently load nodes and edges.
    // The chunking function, `validateSpan` returns the remaining ops and we loop until there are no more ops.
    rn.ops.tailRecM(validateSpan).runS(ValState.initial(ws, rn, squash)).map(_.insertState)
  end validate

  /** Take a span of `ops`, validate said span, return the remaining `ops`.
    */
  // Desired return type is ValStateT[List[WriteOp]], that is, the remaining ops. But `tailRecM` doesn't use Monoid.
  // It uses Either. `tailRecM` halts when the iteration yields Right, continues when it yields Left
  private def validateSpan(ops: List[WriteOp]): ValStateT[Either[List[WriteOp], Unit]] = for
    (span, expansion)  <- spanUntilExpansion(ops)
    (nodeIds, edgeIds) <- ValStateT.inspect(_.collectRequirements(span))
    _                  <- loadNodes(nodeIds)
    _                  <- loadEdges(edgeIds)
    _                  <- span.traverse(validateOp).void
    remainingOps       <- expandWorkspace(expansion)
  yield remainingOps match
    case Nil  => ().asRight  // Right tells tailRecM to stop iterating
    case more => more.asLeft // Left tails tailRecM to continue

  private def spanUntilExpansion(ops: List[WriteOp]) = ValStateT.inspectF { s =>
    val (span, remaining) = spanCollectBoundary(ops) {
      case op: AddEdge if s.requiresExpansion(op) => Some(op)
      case _                                      => None
    }

    remaining match
      case Some((op, remaining)) =>
        val candidates = for
          (projectId, layerBase) <- s.insertState.parentWs.layerBases
          nodeId                 <- layerBase.nodeIds.get(op.targetName)
        yield UnclaimedNode(
          op.targetName,
          nodeId,
          projectId,
          s.insertState.parentWs.depInfos(projectId).commitId,
          layerBase
        )

        // TODO let AddEdge choose the winner when it provides a projectId, for now first is OK
        // (because the dependency namespace can have the same name many times, up to once per dependency)
        // (the workspace namespace cannot, at AddEdge time we pick the winning dependency's element)
        val winner = candidates.headOption

        winner.map(w => (span, (w, remaining).some)).toRight(noSuchTgtInDeps(s))

      case None => (span, None).asRight
    end match

  }

  private def loadNodes(nodeReqs: Map[NodeId, ElemRequirement]) = ValStateT.modify { s =>
    val nodes = for
      entity <- nodeDao2.load(nodeReqs.keys).view
      req    <- nodeReqs.get(entity.id)
    yield entity.name -> ExistingNode(req.projectId, entity.toAsset)

    s.copy(existingNodes = s.existingNodes ++ nodes)
  }

  private def loadEdges(edgeReqs: Map[EdgeId, ElemRequirement]) = ValStateT.modify { s =>
    val edges = for
      entity <- edgeDao2.load(edgeReqs.keys).view
      req    <- edgeReqs.get(entity.id)
      tgt    <- s.getElemNode(entity.targetName)
    yield entity.name -> ExistingEdge(req.projectId, entity, tgt.assetType, s.insertState.isExpressed(entity))

    s.copy(existingEdges = s.existingEdges ++ edges)
  }

  // Splits the collection into a prefix/suffix pair. The prefix are the elements until p returns some boundary element.
  // The suffix is the boundary element and the remaining elements.
  // This is List.span but we are collecting the boundary
  private def spanCollectBoundary[A <: WriteOp](
    ops: List[WriteOp]
  )(p: WriteOp => Option[A]): (List[WriteOp], Option[(A, List[WriteOp])]) =
    val span      = mutable.ListBuffer.empty[WriteOp]
    var remaining = ops
    var boundary  = Option.empty[A]

    while remaining.nonEmpty && boundary.isEmpty do
      p(remaining.head) match
        case b: Some[A] => boundary = b
        case None       =>
          span += remaining.head
          remaining = remaining.tail

    (span.toList, boundary.tupleRight(remaining))
  end spanCollectBoundary

  private def validateOp(op: WriteOp): ValStateT[Unit] = op match
    case op: AddNode[?]     => validateAddNode(op)
    case op: DeleteNode     => validateDeleteNode(op)
    case op: SetNodeData[?] => validateSetNodeData(op)
    case op: AddEdge        =>
      for
        peers <- ValStateT.inspect(_.getFuzzyPeers(op.sourceName, op.group))
        _     <- peers.find(_.name == op.name) match
                   case Some(prevEdge) => validateOverwritingAddEdge(op, peers, prevEdge)
                   case None           => validateAddEdge(op, peers)
      yield ()
    case op: SetEdgeData    => validateSetEdgeData(op)
    case op: SetEdgeOrder   => validateSetEdgeOrder(op)
    case op: DeleteEdge     => validateDeleteEdge(op)
    case op: SetHomeName    => validateSetHomeName(op)
    case op: SetRootName    => validateSetRootName(op)

  private def validateAddNode[A](op: AddNode[A]): ValStateT[Unit] = StateT.modifyF { s =>
    implicit val assetType: AssetType[A] = op.assetType

    for
      _ <- s.insertState.containsNode(op.name).thenRaise(duplicateNode(s))
      _ <- validationService.createValidateE(op.data, op.exemptBlobName, s.opIndex.some).left.map(dataInvalid(s))
    yield s.increment(_.withAddNode(op, commitTime))
  }

  private def validateDeleteNode(op: DeleteNode): ValStateT[Unit] = StateT.modifyF { s =>
    for
      _ <- s.insertState.getRootName.contains(op.name).thenRaise(rootNodeRequired(s))
      _ <- s.insertState.getHomeName.contains(op.name).thenRaise(homeNodeRequired(s))
    yield s.getElemNode(op.name) match
      case Some(prev: ElementalNode[?]) =>
        s.increment(_.withDeleteNode(op, prev))
      case None                         =>
        // or should we fail because op.name already doesn't exist?
        s.increment(identity)
  }

  private def validateSetNodeData[A](op: SetNodeData[A]): ValStateT[Unit] = StateT.modifyF { s =>
    implicit val assetType: AssetType[A] = op.assetType

    lazy val groupSizes = s.getGroupSizes(op.name)

    for
      prev <- s.getElemNode(op.name).toRight(noSuchNode(s))
      _    <- validationService.updateValidateE(prev.data, op.data, groupSizes, s.opIndex.some).left.map(dataInvalid(s))
    yield s.increment(_.withSetNodeData(op, prev))
  }

  private def validateAddEdge(op: AddEdge, peers: List[ElementalEdge]): ValStateT[Unit] =

    val checkSrcTgt = ValStateT.inspectF { s =>
      for
        _   <- (op.sourceName == op.targetName).thenRaise(sourceIsTarget(s))
        src <- s.getElemNode(op.sourceName).toRight(noSuchSrc(s))
        tgt <- s.getElemNode(op.targetName).toRight(noSuchTgt(s))
      yield (src, tgt)
    }

    def checkEdgeRule(src: ElementalNode[?], tgt: ElementalNode[?]) = ValStateT.inspectF { s =>
      for
        edgeRule <- src.assetType.edgeRules.get(op.group).toRight(noSuchGroup(s))
        _        <- edgeRule.typeIds.contains(tgt.assetType.id).elseRaise(illegalTgtType(s, tgt.assetType))
      yield edgeRule
    }

    // should be tailRec and isn't :shame:
    // how will I sleep at night on mere hope that the count of fuzzy edges does not exceed the stack frame limit
    def checkCardinality(group: Group, peers: List[ElementalEdge]): ValStateT[List[ElementalEdge]] =
      if group.cardinality.exists(_ < peers.size + 1) then

        var exclusion = Option.empty[ExistingEdge]
        val nextPeers = peers.flatMap { // try to exclude any fuzzy edge, don't care which one
          case e if exclusion.isDefined     => Some(e)
          case e: ExistingEdge if e.isFuzzy => exclusion = Some(e); None
          case e                            => Some(e)
        }

        exclusion match
          case Some(e) => ValStateT.modify(_.withExcludedEdge(e)).flatMap(_ => checkCardinality(group, nextPeers))
          case None    => ValStateT.inspectF(s => Left(groupTooBig(s, group.cardinality)))
      else ValStateT.pure(peers)

    for
      (src, tgt)      <- checkSrcTgt
      uniquePeers     <- peers.flatTraverse(checkUniquePeer(op))
      _               <- checkEdgeRule(src, tgt)
      goldilocksPeers <- checkCardinality(op.group, uniquePeers)
      pos             <- establishPosition(op.position, goldilocksPeers)
      _               <- ValStateT.modify(_.increment(_.withAddEdge(op, pos, tgt, commitTime)))
    yield ()
  end validateAddEdge

  private def validateOverwritingAddEdge(
    op: AddEdge,
    peers: List[ElementalEdge],
    prevEdge: ElementalEdge
  ): ValStateT[Unit] =

    val checkSrcTgt = ValStateT.inspectF { s =>
      for
        _   <- (op.sourceName == op.targetName).thenRaise(sourceIsTarget(s))
        _   <- (op.sourceName == prevEdge.srcName).elseRaise(requiresSrc(s, prevEdge.srcName))
        _   <- (op.group == prevEdge.grp).elseRaise(requiresGrp(s, prevEdge.grp))
        tgt <- s.getElemNode(op.targetName).toRight(noSuchTgt(s))
        _   <- (tgt.assetType == prevEdge.tgtAssetType).elseRaise(requiresTgtType(s, prevEdge.tgtAssetType))
      yield tgt
    }

    for
      tgt         <- checkSrcTgt
      uniquePeers <- peers.flatTraverse(checkUniquePeer(op))
      pos         <- establishPosition(op.position, uniquePeers)
      _           <- ValStateT.modify(_.increment(_.withOverwritingAddEdge(op, pos, tgt, commitTime, prevEdge)))
    yield ()
  end validateOverwritingAddEdge

  // desired return type is `ValStateT[Option[ElementalEdge]]` as this transformer can drop duplicate peers.
  // but forced to use `List` instead of `Option` for `flatTraverse` appeasement.
  private def checkUniquePeer(op: AddEdge)(peer: ElementalEdge): ValStateT[List[ElementalEdge]] = ValStateT.apply { s =>
    peer match
      case e if e.tgtName == op.targetName && e.isExpressed           => Left(duplicateTarget(s))
      case e: ExistingEdge if e.tgtName == op.targetName && e.isFuzzy => Right(s.withExcludedEdge(e) -> Nil)
      case e                                                          => Right(s, List(e))
  }

  private def validateSetEdgeData(op: SetEdgeData): ValStateT[Unit] = StateT.modifyF { s =>
    for prev <- s.getElemEdge(op.name).toRight(noSuchEdge(s))
    yield s.increment(_.withSetEdgeData(op, prev))
  }

  private def validateSetEdgeOrder(op: SetEdgeOrder): ValStateT[Unit] =

    def checkOp(peerNames: Set[UUID]): ValStateT[Unit] = ValStateT.inspectF { s =>
      for
        src <- s.getElemNode(op.sourceName).toRight(noSuchSrc(s))
        _   <- src.assetType.edgeRules.get(op.group).toRight(noSuchGroup(s))
        -   <- (op.ordering.size == op.ordering.distinct.size).elseRaise(ambiguousOrdering(s))
        _   <- peerNames.forall(op.ordering.contains).elseRaise(requiresOrdering(s, peerNames))
      yield ()
    }

    for
      fuzzyPeers  <- ValStateT.inspect(_.getFuzzyPeers(op.sourceName, op.group))
      peers        = fuzzyPeers.view.filter(_.isExpressed).map(_.name).toSet
      _           <- checkOp(peers)
      fuzzyMap     = mutable.Map.from(fuzzyPeers.view.map(p => p.name -> p))
      orderedPeers = op.ordering.view.flatMap(fuzzyMap.remove).toList ++ fuzzyMap.values.toList.sortBy(_.pos)
      _           <- spreadPositionsForVal(orderedPeers)
      _           <- ValStateT.modify(_.increment(_.withSetEdgeOrder(op)))
    yield ()
  end validateSetEdgeOrder

  private def validateDeleteEdge(op: DeleteEdge): ValStateT[Unit] = StateT.modifyF { s =>
    for prev <- s.getElemEdge(op.name).toRight(noSuchEdge(s))
    yield s.increment(_.withDeleteEdge(op, prev, s.existingEdges.get(op.name)))
  }

  private def validateSetHomeName(op: SetHomeName): ValStateT[Unit] = StateT.modifyF { s =>
    for
      node <- s.getElemNode(op.name).toRight(noSuchNode(s))
      _    <- (node.assetType.id == AssetTypeId.Course).elseRaise(requiresType(s, AssetTypeId.Course))
    yield s.increment(_.withSetHomeName(op))
  }

  private def validateSetRootName(op: SetRootName): ValStateT[Unit] = StateT.modifyF { s =>
    for
      node <- s.getElemNode(op.name).toRight(noSuchNode(s))
      _    <- (node.assetType.id == AssetTypeId.Root).elseRaise(requiresType(s, AssetTypeId.Root))
    yield s.increment(_.withSetRootName(op))
  }

  // gets the position integer, rearranging peers if necessary
  private def establishPosition(position: Option[Position], peers: List[ElementalEdge]): ValStateT[Int] =
    position match
      case None                          => establishPositionAtEnd(peers)
      case Some(Position.Start)          => establishPositionAtStart(peers)
      case Some(Position.End)            => establishPositionAtEnd(peers)
      case Some(Position.Before(anchor)) => establishPositionBefore(peers, anchor)
      case Some(Position.After(anchor))  => establishPositionAfter(peers, anchor)

  // .as(0) because position int for Position.Start is always 0
  private def establishPositionAtStart(peers: List[ElementalEdge]): ValStateT[Int] = peers match
    case a :: b :: _ if b.pos - a.pos < 2 => spreadPositionsForVal(peers, 1).as(0)
    case a :: b :: _                      => updatePositionForVal(a, a.pos + (b.pos - a.pos) / 2).as(0)
    case a :: Nil                         => updatePositionForVal(a, AssetEdge.Gap).as(0)
    case _                                => StateT.pure(0)

  private def establishPositionAtEnd(peers: List[ElementalEdge]): ValStateT[Int] =
    StateT.pure(peers.lastOption.map(_.pos + AssetEdge.Gap).getOrElse(0))

  private def establishPositionBefore(peers: List[ElementalEdge], anchor: UUID): ValStateT[Int] =
    @tailrec
    def loop(remainingPeers: List[ElementalEdge]): ValStateT[Int] = remainingPeers match
      case a :: _ if a.name == anchor                           => establishPositionAtStart(peers)
      case a :: b :: _ if b.name == anchor && b.pos - a.pos < 2 =>
        spreadPositionsForVal(peers).flatMap(spacedApartPeers => establishPositionBefore(spacedApartPeers, anchor))
      case a :: b :: _ if b.name == anchor                      => StateT.pure(a.pos + (b.pos - a.pos) / 2)
      case head :: tail                                         => loop(tail)
      case Nil                                                  => StateT.inspectF(s => noSuchPositionAnchor(s).asLeft)

    loop(peers)
  end establishPositionBefore

  private def establishPositionAfter(peers: List[ElementalEdge], anchor: UUID): ValStateT[Int] =
    @tailrec
    def loop(remainingPeers: List[ElementalEdge]): ValStateT[Int] = remainingPeers match
      case a :: Nil if a.name == anchor                         => establishPositionAtEnd(peers)
      case a :: b :: _ if a.name == anchor && b.pos - a.pos < 2 =>
        spreadPositionsForVal(peers).flatMap(spacedApartPeers => establishPositionAfter(spacedApartPeers, anchor))
      case a :: b :: _ if a.name == anchor                      => StateT.pure(a.pos + (b.pos - a.pos) / 2)
      case head :: tail                                         => loop(tail)
      case Nil                                                  => StateT.inspectF(s => noSuchPositionAnchor(s).asLeft)

    loop(peers)
  end establishPositionAfter

  private def updatePositionForVal(prev: ElementalEdge, position: Int): ValStateT[ElementalEdge] =
    ValStateT.fromInsertState(updatePosition(prev, position))

  private def spreadPositionsForVal(peers: List[ElementalEdge], offset: Int = 0): ValStateT[List[ElementalEdge]] =
    ValStateT.fromInsertState(spreadPositions(peers, offset))

  private def expandWorkspace(
    expansion: Option[(UnclaimedNode, List[WriteOp])]
  ): ValStateT[List[WriteOp]] =
    expansion match
      case Some((claimant, remainingOps)) =>
        for
          layerWs <- loadWs(claimant.theirProjectId, claimant.theirCommitId)
          _       <- ValStateT.modify(_.expandWorkspace(claimant.name, layerWs))
        yield remainingOps
      case None                           => StateT.empty

  private def loadWs(projectId: Long, commitId: Long): ValStateT[ProjectWorkspace] = ValStateT.apply { s =>
    s.layerWorkspaces.get(commitId) match
      case Some(layerWs) => (s, layerWs).asRight
      case None          =>
        val layerWs = workspaceService.requireLayeredReadAtCommit(projectId, commitId, AccessRestriction.none)
        (s.copy(layerWorkspaces = s.layerWorkspaces.updated(commitId, layerWs)), layerWs).asRight
  }

  /** Writes a commit based on InsertState. Trusts the caller to have provided a valid `s`.
    * @param s
    *   content of new commit
    * @param domainDto
    *   root attribute of all the new elements and commitentity (for copy-to-domain)
    */
  def insert(s: InsertState, domainDto: DomainDTO): CommitResult[LayeredWriteWorkspace] =
    val project   = session.find(classOf[ProjectEntity2], s.parentWs.project.id)
    val createdBy = session.ref[UserFinder](userDto.id)
    val root      = session.ref[DomainFinder](domainDto.id)

    // b for builder. This is what it looks like when you don't use `cats.State`: b1, b2, b3
    // But I don't have an effect this time like Either[ValError, *], so I tolerated the b1, b2, b3.
    val b1 = NextCommit.newBuilder(s)

    val b2 = s.pendingNodes.foldLeft(b1) {
      case (acc, (_, n: InsertNode[?]))       =>
        val node = createNode(n, commitTime, root)
        acc.withNode(node.name, node.id, n.projectId)
      case (acc, (name, n: ClaimNode))        =>
        acc.withNode(name, n.id, n.projectId)
      case (acc, (name, OmitNode(projectId))) =>
        acc.withNodeOmitted(name, projectId)
      case (acc, (name, e: ExcludeNode))      =>
        acc.withNodeExcluded(name, e.id, e.projectId)
    }

    val b3 = s.pendingEdges.foldLeft(b2) {
      case (acc, (_, e: InsertEdge))          =>
        if s.createDurableEdge.contains(e.name) then durableEdgeDao2.create(e, project, root)
        val edge = createEdge(e, commitTime, root)
        acc.withEdge(edge.name, edge.id, e.projectId)
      case (acc, (name, e: ClaimEdge))        =>
        if e.createDurableEdge then durableEdgeDao2.create(e, project, root)
        acc.withEdge(name, e.id, e.projectId)
      case (acc, (name, OmitEdge(projectId))) =>
        acc.withEdgeOmitted(name, projectId)
      case (acc, (name, e: ExcludeEdge))      =>
        acc.withEdgeExcluded(name, e.id, e.projectId)
    }

    val nextCommit = b3.result(session, commitTime, createdBy, root)

    project.head = nextCommit

    val nextDoc   = nextCommit.comboDoc // efficient, kfdoc and driftdoc entities already in session
    val nextEdges = edgeService.loadEdgeAttrs(nextDoc)

    val nextLayerBases = nextDoc.deps.map { case (projectId, _) =>
      val updatedBase = s.pendingLayerBases.get(projectId)
      val prevBase    = s.parentWs.layerBases.get(projectId)
      val nextBase    = updatedBase
        .orElse(prevBase)
        .getOrElse(
          throw new NoSuchElementException(s"next base for layer $projectId; parent commit ${s.parentWs.commitId}")
        )
      projectId -> nextBase
    }

    val nextWs = new LayeredWriteWorkspace(
      project.toProject2,
      nextDoc,
      nextEdges,
      nextLayerBases
    )

    val prevWs = s.parentWs

    val promotedNodes = Set.newBuilder[UUID]
    val demotedNodes  = Set.newBuilder[UUID]

    s.inDegDelta.foreach {
      case (name, d) if d > 0 && prevWs.isRoot(name)                                       => demotedNodes.addOne(name)
      case (name, d) if d <= 0 && !prevWs.isRoot(name) && nextWs.inEdgeAttrs(name).isEmpty => promotedNodes.addOne(name)
      case _                                                                               =>
    }

    val result = CommitResult(
      nextWs,
      b3.modifiedNodes,
      b3.modifiedEdges,
      b3.modifiedNodes,
      b3.modifiedEdges,
      b3.excludedNodes,
      b3.excludedEdges,
      s.replacedEdgeNames,
      demotedNodes.result(),
      promotedNodes.result(),
      catastrophic = false,
      squashed = prevWs.commit.parentId.contains(nextCommit.parent.id),
    )

    // guard indexCommit from rootless/homeless projects
    // (a temporary state during creation in prod, a common permanent state in dbtests)
    if result.ws.branch.project.exists(_.homeName != null) then
      reindexService.indexCommit(domainDto.id, CommitInfo(result))

    result
  end insert

  // declaration needed to capture the existential of InsertNode[_] in A
  private def createNode[A](n: InsertNode[A], modified: LocalDateTime, domain: DomainFinder): NodeEntity2 =
    nodeDao2.create(n.name, n.data, n.assetType, n.created, modified, domain)

  private def createEdge(e: InsertEdge, modified: LocalDateTime, domain: DomainFinder): EdgeEntity2 =
    edgeDao2.create(
      e.name,
      e.srcName,
      e.tgtName,
      e.grp,
      e.pos,
      e.traverse,
      e.localId,
      e.data,
      e.created,
      modified,
      domain
    )

  /** Adds a dependency to `ws` project.
    */
  def addDependency(
    ws: LayeredWriteWorkspace,
    dependent: Project2
  ): Either[CreatesProjectCycle, LayeredWriteWorkspace] =

    // really ought to row lock Project2 also but /shrug
    val depProjectIds = projectDao.loadTransitiveDependencies(dependent.id)

    if depProjectIds.contains(ws.project.id) then
      CreatesProjectCycle(ws.project.name, dependent.name, depProjectIds).asLeft
    else if ws.depInfos.contains(dependent.id) then
      // dep is already a dep, no-op, or should I cause failure?
      // or should I trigger update?
      ws.asRight
    else

      val theirWs    = workspaceService.requireLayeredRead(dependent.id, AccessRestriction.none)
      val addDepDbOp = DbAddDependency(dependent.id, dependent.name, dependent.code)
      val s1         = InsertState.forSync(ws, dependent.id, dependent.head.id, theirWs.toFuzzyLayerBase)
      val s2         = s1.copy(dbOps = Vector(addDepDbOp.finatraEncoded))
      val result     = insert(s2, domainDto)

      result.ws.asRight
    end if
  end addDependency

  /** Updates a dependency of `ws` to its head.
    */
  def updateDependency(ws: LayeredWriteWorkspace, tgtProjectId: Long): SyncResult =

    val existingDep = ws.depInfos.getOrElse(
      tgtProjectId,
      AuthoringBundle.noSuchDependency(ws.project.id, tgtProjectId).throw500
    )

    val tgtProject = projectDao.load(tgtProjectId).getOrElse(throw new NoSuchElementException(s"project $tgtProjectId"))

    if existingDep.commitId == tgtProject.head.id then
      // already up to date
      SyncResult.empty(ws)
    else

      val ourLayer = ws.getLayer(tgtProjectId).get
      val base     = ws.layerBases(tgtProjectId)
      val theirWs  = workspaceService.requireLayeredRead(tgtProjectId, AccessRestriction.none)
      val init     = SyncState.initial(ws, base, theirWs)

      val syncState = updateDependencyService.syncTransformation(ws, ourLayer, base, theirWs).runS(init).value

      val result = insert(syncState.insertState, domainDto)
      SyncResult(result, syncState.actions)
    end if
  end updateDependency
end LayeredWriteService

object LayeredWriteService:

  // claim is NOT A VERB! Is part of compound adjective: claim-traversable target name.
  def claimTraversableTgtName(
    outEdge: EdgeElem,
    visited: collection.Set[UUID]
  ): Option[UUID] =
    val doNotTraverse = IgnoreDuringClaim.contains(outEdge.grp) || visited.contains(outEdge.tgtName)
    if doNotTraverse then None else outEdge.tgtName.some

  val IgnoreDuringClaim: Set[Group] = Set(Group.Assesses, Group.Teaches, Group.Gates, Group.GradebookCategory)

  /** Reset `peers` position integers (even if some gaps don't need enlarged).
    * @return
    *   the updated peers
    */
  // if scala 3, then offset would have type `0 | 1`
  def spreadPositions(
    fuzzyPeers: List[ElementalEdge],
    offset: Int = 0
  ): State[InsertState, List[ElementalEdge]] =
    fuzzyPeers.zipWithIndex
      .traverse({ case (prev, i) =>
        val newPos = AssetEdge.Gap * (i + offset)
        if prev.pos != newPos then updatePosition(prev, newPos)
        else State.pure(prev)
      })

  def updatePosition(prev: ElementalEdge, position: Int): State[InsertState, ElementalEdge] = State.apply { s =>
    val updated = InsertEdge.fromPos(position, prev)
    val nextS   = s.withPendingEdge(updated.name, updated)
    (nextS, updated)
  }

  case class CreatesProjectCycle(
    projectName: String,
    newDepProjectName: String,
    transitiveDeps: Set[Long]
  ):
    val msg =
      s"cycle detected; dependencies of \"$newDepProjectName\" include \"$projectName\"; dependency project ids: ${transitiveDeps
          .mkString(",")}"

  private type ValErrorOr[A] = Either[ValError, A]
  private type ValStateT[A]  = StateT[ValErrorOr, ValState, A]

  // duplicates `object StateT` but sets the type parameters for type inference's sake
  private object ValStateT:
    def apply[A](f: ValState => ValErrorOr[(ValState, A)]): ValStateT[A] = StateT.apply(f)
    def modify(f: ValState => ValState): ValStateT[Unit]                 = StateT.modify(f)
    def modifyF(f: ValState => ValErrorOr[ValState]): ValStateT[Unit]    = StateT.modifyF(f)
    def inspect[A](f: ValState => A): ValStateT[A]                       = StateT.inspect(f)
    def inspectF[A](f: ValState => ValErrorOr[A]): ValStateT[A]          = StateT.inspectF(f)
    def pure[A](a: A): ValStateT[A]                                      = StateT.pure(a)

    // jfc
    // spreadPositions is used during validation and during merge.
    // Merge doesn't use ValState transformers, Validation does.
    // To share the said same functions, adapter is needed:
    def fromInsertState[A](ist: State[InsertState, A]): ValStateT[A] =
      ist
        .transformF(_.value.asRight[ValError])
        .transformS[ValState](_.insertState, (vs, is) => vs.copy(insertState = is))
  end ValStateT

  final case class ValState(
    insertState: InsertState,
    opIndex: Int,
    addNodeNames: Set[UUID],
    existingNodes: Map[UUID, ExistingNode[?]],
    existingEdges: Map[UUID, ExistingEdge],
    layerWorkspaces: Map[CommitId, ProjectWorkspace],
  ):

    /** @return
      *   ids of the nodes and edges of `span` that need to be loaded and appended to `existingNodes` and
      *   `existingEdges`
      */
    def collectRequirements(span: List[WriteOp]): (Map[NodeId, ElemRequirement], Map[EdgeId, ElemRequirement]) =

      val nodeReqs = Map.newBuilder[NodeId, ElemRequirement]
      val edgeReqs = Map.newBuilder[EdgeId, ElemRequirement]

      span foreach {
        case op: SetNodeData[?] =>
          findNodeReq(op.name).foreach(r => nodeReqs.addOne(r.id, r))
        case op: DeleteNode     =>
          findNodeReq(op.name).foreach(r => nodeReqs.addOne(r.id, r))
        case op: AddEdge        =>
          // need src & tgt because we need its assetType to verify group rules
          findNodeReq(op.sourceName).foreach(r => nodeReqs.addOne(r.id, r))
          findNodeReq(op.targetName).foreach(r => nodeReqs.addOne(r.id, r))

          for
            peer    <- insertState.fuzzyOutEdges(op.sourceName, op.group)
            edgeReq <- findEdgeReq(peer.name)
          // additional peers from subsequent AddEdge in `span` are processed when the loop reaches them
          // we don't need to lookahead. We are only collecting elements to load, not validating at this time.
          // if anything were to be optimized here, it would be to eliminate redundant peer processing
          do
            edgeReqs.addOne(edgeReq.id, edgeReq)

            // need tgt because we need its assetType, not for AddEdge but for any subsequent WriteOp
            // that pulls this edge from pendingEdges
            findTgtNodeReq(peer, edgeReq.projectId).foreach(r => nodeReqs.addOne(r.id, r))
          end for
        case op: SetEdgeData    =>
          for edgeReq <- findEdgeReq(op.name)
          do
            edgeReqs.addOne(edgeReq.id, edgeReq)

            // Need tgt because we need its assetType, not for SetEdgeData, but for any
            // subsequent WriteOp that pulls this edge from pendingEdges.
            val edgeAttrs = insertState.getFuzzyEdgeAttrs(op.name)
            edgeAttrs.flatMap(e => findTgtNodeReq(e, edgeReq.projectId)).foreach(r => nodeReqs.addOne(r.id, r))
        case op: SetEdgeOrder   =>
          // need src because we need its assetType to verify group
          findNodeReq(op.sourceName).foreach(r => nodeReqs.addOne(r.id, r))

          for
            peer    <- insertState.fuzzyOutEdges(op.sourceName, op.group)
            edgeReq <- findEdgeReq(peer.name)
          do
            edgeReqs.addOne(edgeReq.id, edgeReq)

            // Need tgt because we need its assetType, not for SetEdgeOrder, but for any
            // subsequent WriteOp that pulls this edge from pendingEdges.
            findTgtNodeReq(peer, edgeReq.projectId).foreach(r => nodeReqs.addOne(r.id, r))

        case op: DeleteEdge =>
          for edgeReq <- findEdgeReq(op.name)
          do
            // need edge only because DbDeleteEdge records edge info
            edgeReqs.addOne(edgeReq.id, edgeReq)

            // Need tgt because we need its assetType, not for DeleteEdge, but for any
            // subsequent WriteOp that pulls this edge from pendingEdges.... Do we though?
            val edgeAttrs = insertState.getFuzzyEdgeAttrs(op.name)
            edgeAttrs.flatMap(e => findTgtNodeReq(e, edgeReq.projectId)).foreach(r => nodeReqs.addOne(r.id, r))

        case op: SetHomeName =>
          // so that we can check that it is a course.1
          findNodeReq(op.name).foreach(r => nodeReqs.addOne(r.id, r))
        case op: SetRootName =>
          // so that we can check that it is a root.1
          findNodeReq(op.name).foreach(r => nodeReqs.addOne(r.id, r))
        case _               =>
      }

      (nodeReqs.result(), edgeReqs.result())
    end collectRequirements

    /** Decides if we need to load the node for `name` and which id to load.
      * @param name
      *   node name
      * @param inEdgeProjectId
      *   if deciding for an edge target, the project of that edge, otherwise None.
      * @return
      *   None if List[WriteOp]/InsertState already fulfill our validation needs. Some if we must load a node.
      */
    private def findNodeReq(name: UUID, inEdgeProjectId: Option[Long] = None): Option[ElemRequirement] =
      if addNodeNames.contains(name) then None // AddNode op provides all elemental data, nothing from DB needed
      else

        insertState.pendingNodes.get(name) match
          case Some(_: InsertNode[?]) => None // InsertNode provides all elemental data
          case Some(_: OmitNode)      => None // `name` is gone
          case Some(_: ExcludeNode)   => None // `name` is gone
          case Some(c: ClaimNode)     => ElemRequirement(c.id, c.projectId).some
          case None                   =>
            val ourId = for
              layer <- insertState.parentWs.doc.findLayerN(name)
              id    <- layer.getNodeId(name)
            yield ElemRequirement(id, layer.projectId)

            lazy val theirId = for
              projectId <- inEdgeProjectId
              base      <- insertState.parentWs.layerBases.get(projectId)
              id        <- base.nodeIds.get(name)
            yield ElemRequirement(id, projectId)

            // when `name` is the tgt of a fuzzy edge, outId will be None, but we require the node nevertheless
            // the id will be found in the layer base of the fuzzy edge project.
            ourId.orElse(theirId)

    private def findTgtNodeReq(edge: EdgeAttrs, edgeProjectId: ProjectId): Option[ElemRequirement] =
      findNodeReq(edge.tgtName, edgeProjectId.some)

    /** Decides if we need to load the edge for `name` and which id to load.
      * @param name
      *   edge name
      * @return
      *   None if List[WriteOp]/InsertState already fulfill our validation needs. Some if we must load an edge.
      */
    private def findEdgeReq(name: UUID): Option[ElemRequirement] = insertState.pendingEdges.get(name) match
      case Some(_: InsertEdge)  => None // InsertEdge provides all elemental data
      case Some(_: OmitEdge)    => None // `name` is gone
      case Some(_: ExcludeEdge) => None // `name` is gone
      case Some(c: ClaimEdge)   => ElemRequirement(c.id, c.projectId).some
      case None                 =>
        for
          layer <- insertState.parentWs.doc.findLayerE(name)
          id    <- layer.getFuzzyEdgeId(name)
        yield ElemRequirement(id, layer.projectId)

    // adding an edge to an excluded node triggers expansion,
    // expansion will fail with noSuchTgtInDeps, but it triggers expansion nonetheless
    def requiresExpansion(op: AddEdge): Boolean =
      !insertState.parentWs.containsNode(op.targetName) &&
        !insertState.containsNode(op.targetName) &&
        !addNodeNames.contains(op.targetName)

    def getElemNode(name: UUID): Option[ElementalNode[?]] =
      insertState.getInsertNode(name).orElse(existingNodes.get(name))

    def getElemEdge(name: UUID): Option[ElementalEdge] = insertState
      .getInsertEdge(name)
      .orElse(existingEdges.get(name))

    // just remember that ValState is copied for every single WriteOp
    private lazy val edges      = existingEdges -- insertState.excludedEdgeNames ++ insertState.insertEdges
    private lazy val fuzzyPeers = edges.groupBy({ case (_, e) => e.srcName -> e.grp })

    // A VectorMap[ElementalEdge.name, ElementalEdge] return value tempts, but I iterate the peers often, so I stayed
    // with List, for examples:
    //   * checking for target duplication is a `.find`
    //   * establishing position int for 3 of the 4 Position enums requires adjacent values to some anchor
    //
    // Caution! ValState only has peers for src-grps of AddEdge and SetEdgeOrder, not for the entire workspace
    // Caution! sort by position is important for establishPositionAtEnd
    def getFuzzyPeers(sourceName: UUID, group: Group): List[ElementalEdge] =
      fuzzyPeers.getOrElse(sourceName -> group, Map.empty).values.toList.sortBy(_.pos)

    // time wasting warning: only evaluates for poolAssessment.1 updates, so a few times per year(?)
    def getGroupSizes(srcName: UUID): Map[Group, Int] =
      insertState.fuzzyOutEdges(srcName).groupBy(_.group).view.mapValues(_.size).toMap

    def increment(f: InsertState => InsertState): ValState = copy(opIndex = opIndex + 1, insertState = f(insertState))

    def withExcludedEdge(e: ExistingEdge): ValState =
      copy(insertState = insertState.withExcludedEdge(e.name, e.id, e.projectId))

    // add the descendants of rootName (according to theirWs) to our insert state.
    // doing so permits WriteOps that use said descendants.
    def expandWorkspace(
      rootName: UUID,
      theirWs: ProjectWorkspace
    ): ValState =

      val visited          = mutable.Set.empty[UUID]
      var nextPendingNodes = insertState.pendingNodes
      var nextPendingEdges = insertState.pendingEdges

      def addNode(name: UUID, theirId: Long): Unit =
        val ourId = insertState.parentWs.getNodeId(name)
        if ourId.isEmpty then
          nextPendingNodes = nextPendingNodes.meeklyUpdated(name, ClaimNode(theirWs.project.id, theirId))

      def addEdge(theirEdge: EdgeElem): Unit =
        val ours = insertState.getFuzzyEdgeAttrs(theirEdge.name)
        if ours.isEmpty then
          val claim = ClaimEdge(theirWs.project.id, theirEdge, createDurableEdge = true)
          nextPendingEdges = nextPendingEdges.meeklyUpdated(theirEdge.name, claim)

      // should unify this with buildSyncActions
      @tailrec
      def loop(nodeNames: Seq[UUID]): Unit =
        val next = for
          nodeName    <- nodeNames
          theirNodeId <- theirWs.getNodeId(nodeName).toSeq
          _            = visited.addOne(nodeName)
          _            = addNode(nodeName, theirNodeId)
          theirEdge   <- theirWs.outEdgeElems(nodeName).toSeq
          _            = addEdge(theirEdge)
          tgtName     <- claimTraversableTgtName(theirEdge, visited)
        yield tgtName

        if next.nonEmpty then loop(next)
      end loop

      loop(Seq(rootName))

      copy(
        insertState = insertState.copy(pendingNodes = nextPendingNodes, pendingEdges = nextPendingEdges)
      )
    end expandWorkspace
  end ValState

  object ValState:
    def initial(ws: LayeredWriteWorkspace, rn: ReplaceNames, squash: Boolean = false): ValState =

      val insertState  = InsertState.initial(ws, rn, squash)
      val addNodeNames = rn.ops.view.collect({ case op: AddNode[?] => op.name }).toSet

      ValState(
        insertState,
        0,
        addNodeNames,
        Map.empty,
        Map.empty,
        Map.empty,
      )
    end initial
  end ValState

  // VectorMap maintains Hibernate session.persist order which maintains cpxp_sequence.nextval order and
  // tends to maintain sql"SELECT..." default order which pleases many dbtests and me
  case class InsertState(
    parentWs: LayeredWriteWorkspace,
    dbOps: Vector[JsonNode],
    forceKeyframe: Boolean,
    maybeSquash: Boolean,
    pendingNodes: VectorMap[UUID, PendingNode],
    pendingEdges: VectorMap[UUID, PendingEdge],
    pendingDepInfos: Map[ProjectId, Commit2.Dep] /* for sync only */,
    pendingLayerBases: Map[ProjectId, FuzzyLayerBase] /* for sync only */,
    pendingHomeName: Option[UUID],
    pendingRootName: Option[UUID],
    inDegDelta: Map[UUID, Int],
    replacedEdgeNames: Map[UUID, UUID], // requested name -> durable name
    createDurableEdge: Set[UUID]        // pendingEdges.keySet subset that we should insert a durable edge for
  ):

    def getRootName: Option[UUID] = pendingRootName.orElse(Option(parentWs.rootName))
    def getHomeName: Option[UUID] = pendingHomeName.orElse(Option(parentWs.homeName))

    def containsNode(name: UUID): Boolean = pendingNodes.get(name) match
      case Some(_: InsertNode[?]) => true
      case Some(_: ClaimNode)     => true
      case Some(_: ExcludeNode)   => false
      case Some(_: OmitNode)      => false
      case None                   => parentWs.containsNode(name)

    def getInsertNode(name: UUID): Option[InsertNode[?]] =
      pendingNodes.get(name).collect({ case i: InsertNode[?] => i })

    def getInsertEdge(name: UUID): Option[InsertEdge] = pendingEdges.get(name).collect({ case i: InsertEdge => i })

    def isPendingOmitEdge(name: UUID): Boolean = pendingEdges.get(name).exists(_.isInstanceOf[OmitEdge])

    lazy val insertEdges: VectorMap[UUID, InsertEdge] = pendingEdges.collect({ case (k, v: InsertEdge) => (k, v) })
    lazy val excludedEdgeNames: Set[UUID]             = pendingEdges.view.collect({ case (name, e: ExcludeEdge) => name }).toSet

    def fuzzyOutEdges(srcName: UUID): Iterable[EdgeAttrs] =
      val initialOutEdges = parentWs.fuzzyOutEdgeAttrs(srcName).groupUniqBy(_.name)
      val outEdges        = pendingEdges.foldLeft(initialOutEdges) {
        case (acc, (name, _: OmitEdge))                           => acc.removed(name)
        case (acc, (name, _: ExcludeEdge))                        => acc.removed(name)
        case (acc, (name, c: ClaimEdge)) if c.srcName == srcName  => acc.updated(name, c.attrs)
        case (acc, (name, i: InsertEdge)) if i.srcName == srcName => acc.updated(name, i.attrs)
        case (acc, _)                                             => acc
      }
      outEdges.values
    end fuzzyOutEdges

    def fuzzyOutEdges(srcName: UUID, grp: Group): Iterable[EdgeAttrs] =
      fuzzyOutEdges(srcName).filter(_.group == grp)

    def getFuzzyEdgeAttrs(name: UUID): Option[EdgeAttrs] = pendingEdges.get(name) match
      case Some(_: OmitEdge)    => None
      case Some(_: ExcludeEdge) => None
      case Some(c: ClaimEdge)   => c.attrs.some
      case Some(i: InsertEdge)  => i.attrs.some
      case None                 => parentWs.getFuzzyEdgeAttrs(name)

    def isExpressed(entity: EdgeEntity2): Boolean = containsNode(entity.sourceName) && containsNode(entity.targetName)

    def withAddNode[A](op: AddNode[A], created: LocalDateTime): InsertState = copy(
      pendingNodes = pendingNodes.updated(op.name, InsertNode.fromOp(op, created)),
      dbOps = dbOps.appended(DbAddNode.fromOp(op).finatraEncoded),
    )

    def withDeleteNode[A](op: DeleteNode, prev: ElementalNode[A]): InsertState =
      val nextPendingNodes = prev match
        case e: ExistingNode[?] => pendingNodes.updated(op.name, ExcludeNode(e.projectId, e.id))
        case i: InsertNode[?]   => pendingNodes.removed(i.name)

      copy(
        pendingNodes = nextPendingNodes,
        dbOps = dbOps.appended(DbDeleteNode.fromOp(op, prev).finatraEncoded)
      )

    def withSetNodeData[A](op: SetNodeData[A], prev: ElementalNode[?]): InsertState =
      val pendingNode = op.restoreNode match
        case Some(nodeId) => ClaimNode(prev.projectId, nodeId)
        case None         => InsertNode.fromOp(op, prev)
      copy(
        pendingNodes = pendingNodes.updated(op.name, pendingNode),
        dbOps = dbOps.appended(DbSetNodeData.fromOp(op).finatraEncoded),
      )

    def withAddEdge(op: AddEdge, pos: Int, tgt: ElementalNode[?], created: LocalDateTime): InsertState = copy(
      pendingEdges = pendingEdges.updated(op.name, InsertEdge.fromOp(op, pos, tgt.assetType, created)),
      inDegDelta = inDegDelta.updatedApply(tgt.name)(_ + 1),
      dbOps = dbOps.appended(DbAddEdge.fromOp(op, pos).finatraEncoded),
    )

    def withOverwritingAddEdge(
      op: AddEdge,
      pos: Int,
      tgt: ElementalNode[?],
      created: LocalDateTime,
      prev: ElementalEdge
    ): InsertState = copy(
      pendingEdges = pendingEdges.updated(op.name, InsertEdge.fromOp(op, pos, tgt.assetType, created)),
      inDegDelta = inDegDelta.updatedApply(tgt.name)(_ + 1).updatedApply(prev.tgtName)(_ - 1),
      dbOps = dbOps.appended(DbAddEdge.fromOp(op, pos).finatraEncoded),
    )

    def withSetEdgeData(op: SetEdgeData, prev: ElementalEdge): InsertState =
      val pendingEdge = op.restoreEdge match
        case Some(edge) => ClaimEdge(prev.projectId, edge, createDurableEdge = false)
        case None       => InsertEdge.fromOp(op, prev)
      copy(
        pendingEdges = pendingEdges.updated(op.name, pendingEdge),
        dbOps = dbOps.appended(DbSetEdgeData.fromOp(op).finatraEncoded),
      )

    def withSetEdgeOrder(op: SetEdgeOrder): InsertState = copy(
      dbOps = dbOps.appended(DbSetEdgeOrder.fromOp(op).finatraEncoded),
    )

    def withPendingEdge(name: UUID, edge: PendingEdge): InsertState = copy(
      pendingEdges = pendingEdges.updated(name, edge)
    )

    // I expect `existing.contains(prev)` to be true 99% of the time. But that 1%....
    // Suppose AB exists and then I commit List(AddEdge(AB), DeleteEdge(AB)).
    // Op 1 is overwriting AB, creating a pending InsertEdge
    // Op 2 is deleting AB.
    // `prev` is the pending InsertEdge, and we merely remove the overwrite.
    // AB survives! This cannot stand.
    // So we provide the "original"/"existing" AB in addition to the prev
    def withDeleteEdge(op: DeleteEdge, prev: ElementalEdge, existing: Option[ExistingEdge]): InsertState =
      val nextPendingEdges = existing match
        case Some(e) => pendingEdges.updated(op.name, ExcludeEdge(e.projectId, e.id))
        case None    => pendingEdges.removed(op.name)

      copy(
        pendingEdges = nextPendingEdges,
        inDegDelta = inDegDelta.updatedApply(prev.tgtName)(_ - 1),
        dbOps = dbOps.appended(DbDeleteEdge.fromOp(op, prev.srcName, prev.grp, prev.tgtName).finatraEncoded)
      )
    end withDeleteEdge

    def withSetHomeName(op: SetHomeName): InsertState = copy(pendingHomeName = op.name.some)
    def withSetRootName(op: SetRootName): InsertState = copy(pendingRootName = op.name.some)

    def withPendingNode(name: UUID, node: PendingNode): InsertState =
      copy(pendingNodes = pendingNodes.updated(name, node))

    def withMergedEdge(edge: InsertEdge, createDurableEdge: Boolean): InsertState = if createDurableEdge then
      copy(
        pendingEdges = pendingEdges.updated(edge.name, edge),
        createDurableEdge = this.createDurableEdge.incl(edge.name)
      )
    else copy(pendingEdges = pendingEdges.updated(edge.name, edge))

    def withExcludedEdge(name: UUID, id: Long, projectId: Long): InsertState =
      copy(pendingEdges = pendingEdges.updated(name, ExcludeEdge(projectId, id)))
  end InsertState

  object InsertState:

    def initial(ws: LayeredWriteWorkspace, rn: ReplaceNames, squash: Boolean): InsertState = InsertState(
      ws,
      Vector.empty,
      forceKeyframe = false,
      maybeSquash = squash,
      VectorMap.empty,
      VectorMap.empty,
      Map.empty,
      Map.empty,
      Option.empty,
      Option.empty,
      Map.empty.withDefaultValue(0),
      rn.replaced,
      rn.create
    )

    def forSync(
      ws: LayeredWriteWorkspace,
      depId: ProjectId,
      depCommitId: CommitId,
      depBase: FuzzyLayerBase,
      omitNodes: Set[UUID] = Set.empty,
      omitEdges: Set[UUID] = Set.empty,
    ): InsertState = InsertState(
      ws,
      Vector.empty,
      forceKeyframe = true,
      maybeSquash = false,
      omitNodes.view.map(_ -> OmitNode(depId)).to(VectorMap),
      omitEdges.view.map(_ -> OmitEdge(depId)).to(VectorMap),
      Map(depId -> Commit2.Dep(depCommitId)),
      Map(depId -> depBase),
      None,
      None,
      Map.empty.withDefaultValue(0),
      Map.empty,
      Set.empty
    )

    def forWsCopy(
      ws: LayeredWriteWorkspace,
      roots: Set[UUID],
      addNodes: List[ValidatedAddNode[?]],
      addEdges: List[ValidatedAddEdge],
      commitTime: LocalDateTime
    ): InsertState =
      val dbOps          = Vector.newBuilder[JsonNode]
      val pendingNodes   = VectorMap.newBuilder[UUID, PendingNode]
      val pendingEdges   = VectorMap.newBuilder[UUID, PendingEdge]
      val createDurEdges = Set.newBuilder[UUID]

      addNodes.foreach(op =>
        dbOps.addOne(DbAddNode.fromOp(op).finatraEncoded)
        pendingNodes.addOne(op.name -> InsertNode.fromOp(op, commitTime))
      )

      addEdges.foreach(op =>
        dbOps.addOne(DbAddEdge.fromOp(op).finatraEncoded)
        pendingEdges.addOne(op.name -> InsertEdge.fromOp(op, null /* ok, validation skipped */, commitTime))
      )

      InsertState(
        ws,
        dbOps.result(),
        forceKeyframe = false,
        maybeSquash = false,
        pendingNodes.result(),
        pendingEdges.result(),
        Map.empty,
        Map.empty,
        Option.empty,
        Option.empty,
        roots.view.map(_ -> 0).toMap.withDefaultValue(0),
        Map.empty,
        createDurEdges.result(),
      )
    end forWsCopy
  end InsertState

  /** Either InsertNode or ClaimNode. A future entry in the next commit doc. And when InsertNode, a new authoringnode
    * row.
    */
  sealed trait PendingNode

  /** Either InsertNode or ExistingNode. Fields needed by validation for both new and existing authoringnode.
    */
  sealed trait ElementalNode[A]:
    def projectId: Long
    def name: UUID
    def data: A
    def assetType: AssetType[A]
    def created: LocalDateTime

  /** A future authoringnode row, either the first of `name` or a subequent version of `name`. Also, the corresponding
    * entry in the next commit doc.
    */
  final case class InsertNode[A](
    projectId: Long,
    name: UUID,
    data: A,
    assetType: AssetType[A],
    created: LocalDateTime
  ) extends ElementalNode[A]
      with PendingNode

  object InsertNode:
    def fromOp[A](op: AddNode[A], created: LocalDateTime): InsertNode[A]          =
      InsertNode(0L, op.name, op.data, op.assetType, created)
    def fromOp[A](op: ValidatedAddNode[A], created: LocalDateTime): InsertNode[A] =
      InsertNode(0L, op.name, op.data, op.assetType, created)
    def fromOp[A](op: SetNodeData[A], prev: ElementalNode[?]): InsertNode[A]      =
      InsertNode(prev.projectId, op.name, op.data, op.assetType, prev.created)

  /** A future entry in the next commit doc. Not a future authoringnode row. Represents the next commit merely using an
    * existing authoringnode (because it belongs to another project or because we are reverting to it)
    */
  case class ClaimNode(projectId: Long, id: Long) extends PendingNode

  /** Remove a node name from the workspace by recording a `-id` in the commit document.
    */
  case class ExcludeNode(projectId: Long, id: Long) extends PendingNode

  /** Remove a node name from the workspace by omitting the name's entry in the commit document. Only for keyframe
    * commits. No effect for drift commits (Map combination cannot subtract).
    */
  case class OmitNode(projectId: Long) extends PendingNode

  /** Fields needed by validation for existing authoringnode.
    */
  final case class ExistingNode[A](
    projectId: Long,
    asset: Asset[A],
  ) extends ElementalNode[A]:
    override val name: UUID              = asset.info.name
    override val data: A                 = asset.data
    override val assetType: AssetType[A] = asset.assetType
    override val created: LocalDateTime  = asset.info.created.asLocalDateTime
    val id: Long                         = asset.info.id

  /** Either InsertEdge or ClaimEdge. A future entry in the next commit doc. And when InsertEdge, a new authoringedge
    * row.
    */
  sealed trait PendingEdge

  /** Either InsertEdge or ExistingEdge. Fields needed by validation for both new and existing authoringedge.
    */
  // we don't store ElementalNode[_] on here because ElementalNodes can change as we fold the List[WriteOp]
  // and I don't want to have to scan the pendingEdges to cascade the changes.
  // however `name` and `assetType` of `ElementalNode[_]` cannot change, so safe to store here absent said cascade.
  sealed trait ElementalEdge:
    def projectId: Long
    def name: UUID
    def srcName: UUID
    def tgtName: UUID
    def tgtAssetType: AssetType[?]
    def grp: Group
    def pos: Int
    def traverse: Boolean
    def localId: UUID
    def data: EdgeData
    def created: LocalDateTime
    def isExpressed: Boolean
    final def isFuzzy: Boolean = !isExpressed
  end ElementalEdge

  /** A future authoringedge row, either the first of `name` or a subequent version of `name`. Also, the corresponding
    * entry in the next commit doc.
    */
  final case class InsertEdge(
    projectId: Long,
    name: UUID,
    srcName: UUID,
    tgtName: UUID,
    tgtAssetType: AssetType[?],
    grp: Group,
    pos: Int,
    traverse: Boolean,
    localId: UUID,
    data: EdgeData,
    created: LocalDateTime,
  ) extends ElementalEdge
      with PendingEdge:
    lazy val attrs: EdgeAttrs = EdgeAttrs(name, srcName, tgtName, localId, grp, pos, traverse)
    override val isExpressed  = true // but what if you are overwriting a fuzzy edge? how could DCM even?
  end InsertEdge

  object InsertEdge:
    def fromOp(op: SetEdgeData, prev: ElementalEdge): InsertEdge = InsertEdge(
      prev.projectId,
      prev.name,
      prev.srcName,
      prev.tgtName,
      prev.tgtAssetType,
      prev.grp,
      prev.pos,
      prev.traverse,
      prev.localId,
      op.data,
      prev.created
    )

    def fromOp(op: AddEdge, position: Int, tgtType: AssetType[?], created: LocalDateTime): InsertEdge =
      InsertEdge(
        0,
        op.name,
        op.sourceName,
        op.targetName,
        tgtType,
        op.group,
        position,
        op.traverse,
        op.edgeId,
        op.data,
        created,
      )

    def fromOp(op: ValidatedAddEdge, tgtType: AssetType[?], created: LocalDateTime): InsertEdge =
      InsertEdge(
        0,
        op.name,
        op.sourceName,
        op.targetName,
        tgtType,
        op.group,
        op.position.toInt,
        op.traverse,
        op.edgeId,
        op.data,
        created
      )

    def fromPos(position: Int, prev: ElementalEdge): InsertEdge = InsertEdge(
      prev.projectId,
      prev.name,
      prev.srcName,
      prev.tgtName,
      prev.tgtAssetType,
      prev.grp,
      position,
      prev.traverse,
      prev.localId,
      prev.data,
      prev.created
    )
  end InsertEdge

  /** A future entry in the next commit doc. Not a future authoringedge row. Represents the next commit merely using an
    * existing authoringedge (because it belongs to another project or because we are reverting to it).
    * @param createDurableEdge
    *   if true, record this durable edge for our project, if false, skip that. It is always safe to pass true. Create
    *   is implemented as "create if not exists". However, passing false will spare cpxp_sequence from needless
    *   incrementation.
    */
  case class ClaimEdge(
    projectId: Long,
    edge: EdgeElem /* needed over `id: Long` for durable edge and for `InsertState.fuzzyOutEdges` */,
    createDurableEdge: Boolean
  ) extends PendingEdge:
    val id: Long         = edge.id
    def srcName: UUID    = edge.srcName
    def attrs: EdgeAttrs = edge.attrs

  /** Remove an edge name from the workspace by recording a `-id` in the commit document.
    */
  private case class ExcludeEdge(projectId: Long, id: Long) extends PendingEdge

  /** Remove an edge name from the workspace by omitting the name's entry in the commit document. Only for keyframe
    * commits. No effect for drift commits (Map combination cannot subtract).
    */
  private case class OmitEdge(projectId: Long) extends PendingEdge

  /** Fields needed by validation for existing authoringedge.
    */
  case class ExistingEdge(
    projectId: Long,
    entity: EdgeEntity2,
    tgtAssetType: AssetType[?] /* only required when validating List[WriteOp] */,
    override val isExpressed: Boolean,
  ) extends ElementalEdge:

    val id: Long                        = entity.id
    override val name: UUID             = entity.name
    override val srcName: UUID          = entity.sourceName
    override val tgtName: UUID          = entity.targetName
    override val grp: Group             = Group.withName(entity.group)
    override val pos: Int               = entity.position
    override val traverse: Boolean      = entity.traverse
    override val localId: UUID          = entity.localId
    override val data: EdgeData         = entity.data.finatraDecoded[EdgeData]
    override val created: LocalDateTime = entity.created
  end ExistingEdge

  case class ElemRequirement(id: Long, projectId: Long)

  sealed abstract class ValError(opIndex: Int, msg: String):
    lazy val bundleMsg: BundleMessage = AuthoringBundle.commitValError(msg, opIndex)
    lazy val exception                = new UncheckedMessageException(bundleMsg)

  case class DuplicateNode(i: Int)                               extends ValError(i, "node name already exists")
  case class NoSuchNode(i: Int)                                  extends ValError(i, "no such node")
  case class NoSuchSrc(i: Int)                                   extends ValError(i, "no such source")
  case class NoSuchTgt(i: Int)                                   extends ValError(i, "no such target")
  case class NoSuchTgtInDeps(i: Int)                             extends ValError(i, "no such target in any dependency")
  case class NoSuchEdge(i: Int)                                  extends ValError(i, "no such edge")
  case class SourceIsTarget(i: Int)                              extends ValError(i, "source is target")
  case class DuplicateTarget(i: Int)                             extends ValError(i, "group already contains target")
  case class NoSuchGroup(i: Int)                                 extends ValError(i, "no such group")
  case class IllegalTargetType(i: Int, typeId: AssetTypeId)      extends ValError(i, s"group cannot contain $typeId")
  case class GroupTooBig(i: Int, max: Int)
      extends ValError(i, s"group cannot contain more than ${max.labelled("asset")}")
  case class RequiresSource(i: Int, prevSrc: UUID)               extends ValError(i, s"edge must source $prevSrc")
  case class RequiresGroup(i: Int, prevGroup: Group)             extends ValError(i, s"edge must be in group $prevGroup")
  case class RequiresTargetType(i: Int, prevTypeId: AssetTypeId) extends ValError(i, s"edge must target $prevTypeId")
  case class RequiresOrdering(i: Int, names: Iterable[UUID])
      extends ValError(i, s"ordering must contain all elements: ${names.mkString(",")}")
  case class AmbiguousOrdering(i: Int)                           extends ValError(i, s"ambiguous ordering")
  case class NoSuchPositionAnchor(i: Int)                        extends ValError(i, "no such edge for position anchor")
  case class RequiresType(i: Int, typeId: AssetTypeId)           extends ValError(i, s"type must be $typeId")
  case class HomeNodeRequired(i: Int)                            extends ValError(i, "home node may not be deleted")
  case class RootNodeRequired(i: Int)                            extends ValError(i, "root node may not be deleted")

  case class DataInvalid(i: Int, cause: UncheckedMessageException) extends ValError(i, cause.getErrorMessage.value):
    override lazy val exception: UncheckedMessageException = cause

  object ValError:
    def duplicateNode(s: ValState): ValError                                 = DuplicateNode(s.opIndex)
    def noSuchNode(s: ValState): ValError                                    = NoSuchNode(s.opIndex)
    def noSuchSrc(s: ValState): ValError                                     = NoSuchSrc(s.opIndex)
    def noSuchTgt(s: ValState): ValError                                     = NoSuchTgt(s.opIndex)
    def noSuchTgtInDeps(s: ValState): ValError                               = NoSuchTgtInDeps(s.opIndex)
    def noSuchEdge(s: ValState): ValError                                    = NoSuchEdge(s.opIndex)
    def dataInvalid(s: ValState)(cause: UncheckedMessageException): ValError = DataInvalid(s.opIndex, cause)
    def sourceIsTarget(s: ValState): ValError                                = SourceIsTarget(s.opIndex)
    def duplicateTarget(s: ValState): ValError                               = DuplicateTarget(s.opIndex)
    def illegalTgtType(s: ValState, tgtType: AssetType[?]): ValError         = IllegalTargetType(s.opIndex, tgtType.id)
    def groupTooBig(s: ValState, max: Option[Int]): ValError                 =
      GroupTooBig(s.opIndex, max.getOrElse(Int.MaxValue))
    def noSuchGroup(s: ValState): ValError                                   = NoSuchGroup(s.opIndex)
    def ambiguousOrdering(s: ValState): ValError                             = AmbiguousOrdering(s.opIndex)
    def requiresSrc(s: ValState, prevSrc: UUID): ValError                    = RequiresSource(s.opIndex, prevSrc)
    def requiresGrp(s: ValState, prevGrp: Group): ValError                   = RequiresGroup(s.opIndex, prevGrp)
    def requiresTgtType(s: ValState, prevTgtType: AssetType[?]): ValError    =
      RequiresTargetType(s.opIndex, prevTgtType.id)
    def requiresOrdering(s: ValState, names: Iterable[UUID]): ValError       = RequiresOrdering(s.opIndex, names)
    def noSuchPositionAnchor(s: ValState): ValError                          = NoSuchPositionAnchor(s.opIndex)
    def requiresType(s: ValState, typeId: AssetTypeId): ValError             = RequiresType(s.opIndex, typeId)
    def homeNodeRequired(s: ValState): ValError                              = HomeNodeRequired(s.opIndex)
    def rootNodeRequired(s: ValState): ValError                              = RootNodeRequired(s.opIndex)
  end ValError
end LayeredWriteService
