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

package loi.authoring.commit.web

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.item.ItemService
import loi.asset.util.Assex.*
import loi.authoring.asset.Asset
import loi.authoring.edge.{EdgeService, Group}
import loi.authoring.feedback.FeedbackProfileDto
import loi.authoring.index.ReindexService
import loi.authoring.node.AssetNodeService
import loi.authoring.project.*
import loi.authoring.security.right.EditContentAnyProjectRight
import loi.authoring.web.AuthoringWebUtils
import loi.authoring.workspace.{AttachedReadWorkspace, WorkspaceService}
import loi.cp.i18n.AuthoringBundle
import loi.cp.i18n.syntax.bundleMessage.*
import loi.cp.web.HandleService
import org.hibernate.Session
import scalaz.NonEmptyList
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scaloi.syntax.collection.*
import scaloi.syntax.date.*
import scaloi.syntax.localDateTime.*
import scaloi.syntax.option.*

import java.util.{Date, UUID}
import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.util.Try

@Component
@Controller(root = true)
private[web] class CommitHistoryWebController(
  val componentInstance: ComponentInstance,
  webUtils: AuthoringWebUtils,
  commitDao2: CommitDao2,
  workspaceService: WorkspaceService,
  nodeService: AssetNodeService,
  projectService: ProjectService,
  reindexService: ReindexService,
  mapper: ObjectMapper,
  session: => Session
)(implicit is: ItemService, hs: HandleService, edgeService: EdgeService)
    extends ApiRootComponent
    with ComponentImplementation:
  import CommitHistoryWebController.*

  @RequestMapping(path = "authoring/branches/{id}/nodes/{name}/commitHistory", method = Method.GET)
  def getNodeCommitHistory(
    @PathVariable("id") branchId: Long,
    @PathVariable("name") name: UUID,
    @QueryParam(value = "detail", decodeAs = classOf[Boolean]) detail: Option[Boolean],
  ): List[CommitSegment] =
    val workspace = webUtils.workspaceOrThrow404(branchId)
    val nameStr   = name.toString

    val commits = commitDao2.loadAncestorsAffectingName(workspace.commitId, name)

    // For imported content there is no initial add node. Without a prior commit
    // we cannot do diffs against the first edit after an import. So add parent
    // commit in this case.
    val priorCommit = for
      last   <- commits.lastOption
      parent <- Option(last.parent)
      if !last.ops.elements.asScala.exists(op => op.text("op") == "addNode" && op.text("name") == nameStr)
    yield parent
    val allCommits  = commits ?:: priorCommit

    val contributors =
      allCommits
        .flatMap(commit => Option(commit.createdByFfs(is)))
        .distinct
        .map(FeedbackProfileDto.apply)
        .groupUniqBy(_.id)

    // help ourselves to the titles of all the added/removed target assets because the titles will not
    // be in the project graph if the target is no longer in the tree
    val targetNames = for
      commit     <- allCommits
      op         <- commit.ops.elements.asScala
      if op.text("sourceName") == nameStr
      targetName <- Option(op.uuid("targetName"))
    yield targetName

    val targetAssets = nodeService.load(workspace).byName(targetNames.distinct).get.groupUniqBy(_.info.name)

    // Moving forwards, delete edges records more information.
    // This is a bit crappy. If you delete a node, we would only tell the front end that there was a `setEdgeOrder`
    // since `deleteEdge` doesn't mention `sourceName`. But it needs that info. So we just include all `deleteEdge`
    // on the assumption that most old changes are just to a single asset. But as-is we will tell you about all
    // delete edges from the commit, regardless of whether they are from this node.
    // I believe that deleted edges will not be in the workspace and so we can't get info2
    // to find out which edges were relevant
    coalesce(allCommits, detail.isTrue).map(segment =>
      CommitSegment(
        segment.head.id,
        segment.last.id,
        segment.head.created.asDate,
        Option(segment.head.createdById).flatMap(contributors.get(_)).getOrElse(FeedbackProfileDto.Unknown),
        segment.last.ops.elements.asScala
          .collect({
            case node: ObjectNode
                if node.text("name") == nameStr ||
                  node.text("sourceName") == nameStr ||
                  (node.text("op") == "deleteEdge" && !node.has("sourceName")) =>
              // just to reduce the payload size..
              node.remove("data")
              for
                targetName  <- Option(node.uuid("targetName"))
                targetAsset <- targetAssets.get(targetName)
              do node.set[ObjectNode]("target", assetNode(Some(targetAsset)))
              node
            case node: ObjectNode if node.text("op") == "updateDependency" =>
              // just to reduce the payload size..
              node.remove(
                Seq("narrativelyUpdatedNodeNames", "narrativelyAddedNodeNames", "narrativelyRemovedNodeNames").asJava
              )
              node

          })
          .toList,
        truncated = false,
        hidden = priorCommit.exists(_ eq segment.last),
      )
    )
  end getNodeCommitHistory

  /** @param limit
    *   a limit on the number of commits to load but we coalesce them so the return size may be less.
    */
  @RequestMapping(path = "authoring/branches/{id}/commits/log", method = Method.GET)
  def getCommitLog(
    @PathVariable("id") branchId: Long,
    @QueryParam(value = "after", decodeAs = classOf[Long]) after: Option[Long],
    @QueryParam("limit") limit: Int,
    @QueryParam(value = "detail", decodeAs = classOf[Boolean]) detail: Option[Boolean],
  ): Seq[CommitSegment] =
    val branch = webUtils.branchOrFakeBranchOrThrow404(branchId)

    def loadAncestors(commitId: Long, limit: Int): List[CommitEntityish] =
      commitDao2.loadAncestors(commitId, limit)

    // This does not check that "after" is in the branch...
    val results =
      after.cata(
        id => loadAncestors(id, limit + 1).drop(1),
        loadAncestors(branch.head.id, limit)
      )

    val contributors =
      results
        .flatMap(commit => Option(commit.createdByFfs(is)))
        .distinctBy(_.getId)
        .map(FeedbackProfileDto.apply)
        .groupUniqBy(_.id)

    val coalesced = coalesce(results, detail.isTrue)

    /** Serialize ops of the commit segments. Uses the parent workspace to get information about assets that are deleted
      * in a commit.
      */
    @tailrec def loop2(
      segments: List[CommitSpan],
      result: List[CommitSegment],
      parentWorkspace: Option[AttachedReadWorkspace]
    ): List[CommitSegment] = segments match
      case Nil                                         => result
      case segment :: rest if segment.last.ops.isEmpty => loop2(rest, result, parentWorkspace)
      case segment :: rest                             =>
        val commitWorkspace =
          workspaceService.requireReadWorkspaceAtCommit(branchId, segment.head.id, AccessRestriction.none)
        val ops             = bowdlerize(segment.last.ops, commitWorkspace, parentWorkspace | commitWorkspace)
        val dto             = CommitSegment(
          segment.head.id,
          segment.last.id,
          segment.head.created.asDate,
          contributors.getOrElse(segment.head.createdById, FeedbackProfileDto.Unknown),
          ops.take(100),
          ops.length > MaxOps,
          hidden = false,
        )
        loop2(rest, dto :: result, Some(commitWorkspace))
    val parentWorkspace = for
      segment <- coalesced.lastOption
      parent  <- Option(segment.last.parent)
    yield workspaceService.requireReadWorkspaceAtCommit(branchId, parent.id, AccessRestriction.none)

    loop2(coalesced.reverse, Nil, parentWorkspace)
  end getCommitLog

  @RequestMapping(path = "authoring/branches/{id}/commits/{commitId}/revert", method = Method.POST)
  def revertBranch(
    @PathVariable("id") branchId: Long,
    @PathVariable("commitId") commitId: Long,
    @QueryParam("head") headId: Long,
  ): Unit =
    val branch =
      webUtils.branchOrFakeBranchOrThrow404(branchId, AccessRestriction.projectMemberOr[EditContentAnyProjectRight])

    // we didn't lock the branch so this is weak, but so be it.
    if headId != branch.head.id then throw AuthoringBundle.commitConflict.throw409

    if projectService.rewindHead(branch, commitId).isDefined then reindexService.indexBranch(branch.id)
    else throw AuthoringBundle.branchCommit.throw422
  end revertBranch

  private def coalesce(commits: List[CommitEntityish], detail: Boolean): List[CommitSpan] =

    /** Coalesce sequences of commits that are just edit node data on the same asset */
    @tailrec def loop(
      commits: List[CommitEntityish],
      spans: List[CommitSpan],
      span: CommitSpan
    ): List[CommitSpan] =
      (commits, span) match
        case (Nil, span)                                              => span :: spans
        case (commit :: rest, span) if coalescible(commit, span.head) => loop(rest, spans, commit <:: span)
        case (commit :: rest, span)                                   => loop(rest, span :: spans, NonEmptyList(commit))

    if detail then commits.map(NonEmptyList(_))
    else
      commits.reverse match
        case Nil            => Nil
        case commit :: rest => loop(rest, Nil, NonEmptyList(commit))
  end coalesce

  // this relies on contemporary behaviour of front-end picking persistent node names
  private def coalescible(commit: CommitEntityish, prior: CommitEntityish): Boolean =
    (commit.createdById == prior.createdById) &&
      (commit.created.asDate - prior.created.asDate < MaxCoalesceInterval) &&
      (commit.ops.size == 1) &&
      isSetNodeData(commit.ops.get(0)) && {
        val name = commit.ops.get(0).text("name")
        prior.ops.elements.asScala.exists(op => (isSetNodeData(op) || isAddNode(op)) && op.text("name") == name)
      }

  private def isSetNodeData(node: JsonNode): Boolean = node.text("op") == "setNodeData"

  private def isAddNode(node: JsonNode): Boolean = node.text("op") == "addNode"

  private def assetNode(attempt: Try[Asset[?]]): ObjectNode =
    assetNode(attempt.toOption)

  private def assetNode(maybe: Option[Asset[?]]): ObjectNode =
    val node = mapper.createObjectNode()
    maybe match
      case Some(asset) =>
        node.put("name", asset.info.name.toString)
        node.put("typeId", asset.info.typeId.toString)
        asset.title.foreach(node.put("title", _))
      case None        =>
        node.put("title", "<error>")
    node
  end assetNode

  private def bowdlerize(
    commitOps: JsonNode,
    commitWorkspace: AttachedReadWorkspace,
    parentWorkspace: AttachedReadWorkspace
  ): List[ObjectNode] =
    val added    = mutable.Set.empty[UUID]
    val srcEdged = mutable.Set.empty[(UUID, Group)] // parent-groups with added/deleted edges
    val addEdged = mutable.Set.empty[UUID]          // edge targets added

    val jsonOps = commitOps.elements.asScala.toList

    // we have to handle old-school commits that have bare deleteEdge ops and nasty edge ops
    jsonOps foreach {
      case node: ObjectNode if node.text("op") == "deleteEdge" && !node.has("group") =>
        val name        = node.uuid("name")
        val edge        = parentWorkspace.getEdgeInfo(name).get
        val sourceAsset = nodeService.load(parentWorkspace).byId(edge.sourceId).get
        val targetAsset = nodeService.load(parentWorkspace).byId(edge.targetId).get
        srcEdged.add(sourceAsset.info.name -> edge.group)

      case node: ObjectNode if node.text("op") == "deleteEdge" =>
        srcEdged.add(node.uuid("sourceName") -> node.group("group"))

      case node: ObjectNode if node.text("op") == "addEdge" =>
        srcEdged.add(node.uuid("sourceName") -> node.group("group"))
        addEdged.add(node.uuid("targetName"))

      case node: ObjectNode if node.text("op") == "addNode" =>
        added.add(node.uuid("name"))

      case _ =>
    }

    jsonOps.iterator
      .collect({
        case node: ObjectNode if node.text("op") == "deleteEdge" && !node.has("group") =>
          val name        = node.uuid("name")
          val edge        = parentWorkspace.getEdgeInfo(name).get
          val sourceAsset = nodeService.load(parentWorkspace).byId(edge.sourceId)
          val targetAsset = nodeService.load(parentWorkspace).byId(edge.targetId)
          node.put("group", edge.group.entryName)
          node.set[ObjectNode]("source", assetNode(sourceAsset))
          node.set[ObjectNode]("target", assetNode(targetAsset))
          node

        case node: ObjectNode if node.text("op") == "deleteEdge" =>
          val sourceAsset = nodeService.load(parentWorkspace).byName(node.uuid("sourceName"))
          val targetAsset = nodeService.load(parentWorkspace).byName(node.uuid("targetName"))
          node.set[ObjectNode]("source", assetNode(sourceAsset))
          node.set[ObjectNode]("target", assetNode(targetAsset))
          node

        case node: ObjectNode if node.text("op") == "addEdge" =>
          val sourceName  = node.uuid("sourceName")
          val sourceAsset =
            nodeService.load(added(sourceName).fold(commitWorkspace, parentWorkspace)).byName(sourceName)
          val targetAsset = nodeService.load(commitWorkspace).byName(node.uuid("targetName"))
          node.set[ObjectNode]("source", assetNode(sourceAsset))
          node.set[ObjectNode]("target", assetNode(targetAsset))
          node.remove("data")
          node

        case node: ObjectNode if node.text("op") == "setNodeData" =>
          val asset = nodeService.load(parentWorkspace).byName(node.uuid("name"))
          node.set[ObjectNode]("asset", assetNode(asset))
          node.remove("data")
          node

        case node: ObjectNode if node.text("op") == "addNode" && !addEdged(node.uuid("name")) =>
          val name  = node.uuid("name")
          val asset = nodeService.load(commitWorkspace).byName(name)
          node.set[ObjectNode]("asset", assetNode(asset))
          node.remove("data")
          node

        case node: ObjectNode
            if node.text("op") == "setEdgeOrder" && !srcEdged(node.uuid("sourceName"), node.group("group")) =>
          // we exclude set-edge-order if we added or deleted from the parent under the assumption that was the only change..
          val sourceName  = node.uuid("sourceName")
          val sourceAsset =
            nodeService.load(added(sourceName).fold(commitWorkspace, parentWorkspace)).byName(sourceName)
          node.set[ObjectNode]("source", assetNode(sourceAsset))
          node

        case node: ObjectNode if node.text("op") == "setEdgeData" =>
          // FE doesn't handle this, would have to add source name so we know what it applies to
          node.remove("data")
          node

        case node: ObjectNode if node.text("op") == "addDependency"    => node
        case node: ObjectNode if node.text("op") == "updateDependency" => node
      })
      .take(1 + MaxOps)
      .toList
  end bowdlerize
end CommitHistoryWebController

private[web] object CommitHistoryWebController:
  private final val MaxCoalesceInterval = 10.minutes
  private final val MaxOps              = 100

  type CommitSpan = NonEmptyList[CommitEntityish]

  implicit class JsonNodeOps(private val self: JsonNode) extends AnyVal:
    def text(propertyName: String): String = self.path(propertyName).textValue
    def uuid(propertyName: String): UUID   = if self.has(propertyName) then UUID.fromString(text(propertyName)) else null
    def group(propertyName: String): Group = Group.withName(text(propertyName))
end CommitHistoryWebController

private[web] final case class CommitSegment(
  first: Long,
  last: Long,
  created: Date,
  createdBy: FeedbackProfileDto,
  ops: List[JsonNode], // bowdlerized subset of the ops
  truncated: Boolean,
  hidden: Boolean,     // artificially added initial commit
)
