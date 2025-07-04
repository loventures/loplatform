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

package loi.authoring.index

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.mime.MimeWebService
import loi.asset.util.Assex.asshatOps
import loi.authoring.asset.Asset
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.blob.BlobService
import loi.authoring.edge.{EdgeService, Group}
import loi.authoring.node.AssetNodeService
import loi.authoring.project.*
import loi.authoring.syntax.index.*
import loi.authoring.workspace.*
import loi.authoring.write.CommitResult
import scalaz.std.list.*
import scalaz.std.set.*
import scalaz.syntax.foldable.*
import scalaz.syntax.monad.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scalaz.{Foldable, IStream, Monoid}
import scaloi.syntax.option.*
import scaloi.syntax.set.*

import java.util.UUID
import scala.collection.mutable

/** Proof-of-concept index service that demonstrates how to index a branch. */
@Service
trait IndexService:

  /** Index all branches into elastic search. */
  def indexAll(delete: Boolean = false): Unit

  /** Index a branch into elastic search. */
  def indexBranch(branchId: Long, delete: Boolean = false): Unit

  /** Index an offering into elastic search. */
  def indexOffering(offeringId: Long, branchId: Long, commitId: Long, delete: Boolean): Unit

  /** Index an asset into elastic search. */
  def indexAsset(branchId: Long, assetId: Long): Unit

  /** Index the result of a write service commit.
    */
  def indexCommit(commit: CommitInfo): Unit
end IndexService

@Service
class IndexServiceImpl(
  assetNodeService: AssetNodeService,
  projectService: ProjectService,
  workspaceService: WorkspaceService,
  esService: EsService,
)(implicit blobService: BlobService, mimeWebService: MimeWebService, edgeService: EdgeService)
    extends IndexService:
  import IndexServiceImpl.*

  override def indexAll(delete: Boolean): Unit =
    indexAllCommands(delete) `foldMapU` execute

  override def indexBranch(branchId: Long, delete: Boolean): Unit =
    val workspace = workspaceService.requireReadWorkspace(branchId, AccessRestriction.none)
    indexBranchCommands(workspace, delete) `foldMapU` execute

  override def indexOffering(offeringId: Long, branchId: Long, commitId: Long, delete: Boolean): Unit =
    val workspace = workspaceService.requireReadWorkspaceAtCommit(branchId, commitId, AccessRestriction.none)
    indexBranchCommands(workspace, delete, Some(offeringId)) `foldMapU` execute

  override def indexAsset(branchId: Long, assetId: Long): Unit =
    val workspace    = workspaceService.requireReadWorkspace(branchId, AccessRestriction.none)
    val usedAssetIds = subtree(workspace, workspace.requireNodeId(workspace.homeName).get)

    if workspace.containsNode(assetId) then execute(indexAssetCommand(workspace, assetId, usedAssetIds))

  override def indexCommit(commit: CommitInfo): Unit =
    indexCommitCommands(commit) `foldMapU` execute

  private def indexAllCommands(delete: Boolean): IStream[IndexCommand] =
    val projects = projectService.loadProjects(excludeArchived = false)

    // again this farce of there being multiple branches. but I don't know what garbage is still floating around
    def loadWorkspaces(project: Project): IStream[AttachedReadWorkspace] = project match
      case layery: Project2 => IStream.Strict(workspaceService.requireReadWorkspace(layery.id, AccessRestriction.none))

    for
      (project, index) <- IStream.fromFoldable(projects.zipWithIndex.toList)
      _                 = logger.info(s"Index project ${index + 1}/${projects.size}: ${project.name}")
      workspace        <- loadWorkspaces(project)
      command          <- indexBranchCommands(workspace, delete)
    yield command
  end indexAllCommands

  private def indexBranchCommands(
    workspace: AttachedReadWorkspace,
    delete: Boolean,
    offering: Option[Long] = None,
  ): IStream[IndexCommand] =
    val usedAssetIds = subtree(workspace, workspace.requireNodeId(workspace.homeName).get)

    val deleteBranch = delete ?? IStream.Strict(DeleteBranch(workspace.bronchId, offering).widen)

    val indexAssets = for
      nodeId <- IStream.fromFoldable(workspace.nodeIds.toList)
      command = indexAssetCommand(workspace, nodeId, usedAssetIds, offering)
    yield command

    deleteBranch !! indexAssets
  end indexBranchCommands

  private def indexAssetCommand(
    workspace: AttachedReadWorkspace,
    assetId: Long,
    usedAssetIds: Set[Long],
    offering: Option[Long] = None,
  ): IndexCommand =
    val asset = loadNode(workspace, assetId)

    // If this asset can be browsed to in DCM, then include all linked assets that you can't browse to.
    // This lets you search a content designer by its contents (but not, say, a one column layout). The
    // child assets are also indexed separately for search individually (for example by content designer).
    // With the death of content designer this should probably die.

    // Embed questions in assessments and surveys so you can find an assessment by question text.

    // Embed descendant titles in modules and lessons so they are easier to find without the expense of
    // embedding the full subtree of all content.

    val embeddedAssets = embeddedDescendants(workspace, asset).toSeq.map(loadNode(workspace, _))

    // as there is only one branch per project, workspace.projectInfo.archived ought to be
    // satisfactory for all cases, but just in case...
    val branchArchived = workspace.projectInfo.archived

    IndexDocument(
      AssetNodeDocument(
        project = workspace.projectInfo.id,
        projectRetired = workspace.projectInfo.archived,
        projectMetadata = ProjectMetadata(workspace.projectInfo),
        branch = workspace.bronchId,
        branchArchived = branchArchived,
        commit = workspace.commitId,
        offering = offering | 0L, // has to have a value as null is unsearchable
        name = asset.info.name,
        archived = asset.info.archived,
        used = usedAssetIds.contains(assetId),
        typeId = asset.assetType.id,
        created = asset.info.created,
        modified = asset.info.modified,
        data = indexAsset(asset).copy(embedded =
          if AssetTypeId.ContainerTypes.contains(asset.info.typeId) then stringifyOpt(embeddedAssets.flatMap(_.title))
          else stringifyOpt(embeddedAssets.map(indexAsset))
        )
      )
    )
  end indexAssetCommand

  private def indexAsset(asset: Asset[?]): AssetDataDocument = asset.index

  private def indexCommitCommands(commit: CommitInfo): IStream[IndexCommand] =
    val workspace    = workspaceService.requireReadWorkspace(commit.branchId, AccessRestriction.none)
    val usedAssetIds = subtree(workspace, workspace.nodeId(workspace.homeName))

    val modifiedNodeIds    = commit.modifiedNodes.map(workspace.nodeId)
    val indexModifiedNodes = IStream.fromFoldable(modifiedNodeIds) map { id =>
      indexAssetCommand(workspace, id, usedAssetIds).widen
    }

    // Also update all ancestors of new nodes that also embed said new nodes.
    val ancestors                     = modifiedNodeIds.foldMap(supertree(workspace, _)) -- modifiedNodeIds
    def isAffected(id: Long): Boolean =
      embeddedDescendants(workspace, loadNode(workspace, id)) `intersects` modifiedNodeIds
    val indexAffectedNodes            = IStream.fromFoldable(ancestors.filter(isAffected)) map { id =>
      indexAssetCommand(workspace, id, usedAssetIds).widen
    }

    // Find the full subtree beneath each demoted node and update documents to used.
    // Some may have been promoted onto unused nodes and so still unused, hence `& usedAssetIds`.
    // Some  members of the subtree may already have been used, but no harm.
    val demotedNodeIds     = commit.demotedNodes.foldMap(name => subtree(workspace, workspace.requireNodeId(name).get))
    val updateDemotedNodes = IStream.fromFoldable(demotedNodeIds & usedAssetIds) map { id =>
      UpdateDocument(workspace.bronchId, None, workspace.nodeName(id), "used" -> true).widen
    }

    // Find the full subtree beneath each promoted node and update documents to unused.
    // Some descendants may still be used elsewhere, hence `-- usedAssetIds`.
    val promotedNodeIds     = commit.promotedNodes.foldMap(name => subtree(workspace, workspace.requireNodeId(name).get))
    val updatePromotedNodes = IStream.fromFoldable(promotedNodeIds -- usedAssetIds) map { id =>
      UpdateDocument(workspace.bronchId, None, workspace.nodeName(id), "used" -> false).widen
    }

    indexModifiedNodes !! indexAffectedNodes !! updateDemotedNodes !! updatePromotedNodes
  end indexCommitCommands

  /** Gather the ids of all descendants of a given node that should be embedded in the index document for that node. For
    * example, embed questions in an assessment, embed content parts in a learning activity. It would perhaps be wise to
    * think using the edge rules.
    */
  private def embeddedDescendants(workspace: ReadWorkspace, asset: Asset[?]): Set[Long] =
    // Generally embed anything that you can't walk to in DCM, but also include questions
    def embedEdgeTarget(e: EdgeInfo): Boolean =
      e.traverse && !WalkableTypeIds.contains(loadNode(workspace, e.targetId).info.typeId)

    // almost certainly this is just content designer nodes
    val embeddedChildren    = WalkableTypeIds.contains(asset.info.typeId) ??
      workspace.outEdgeInfos(asset.info.id).filter(embedEdgeTarget).map(_.targetId).toList
    val embeddedDescendants = embeddedChildren.foldMap(subtree(workspace, _, embedEdgeTarget))

    // embed element titles in containers
    val embeddedElements = AssetTypeId.ContainerTypes.contains(asset.info.typeId) ??
      subtree(workspace, asset.info.id, _.group == Group.Elements)

    // embed questions in assessments
    val embeddedQuestions =
      workspace.outEdgeInfos(asset.info.id).filter(_.group == Group.Questions).map(_.targetId).toList

    embeddedDescendants ++ embeddedElements ++ embeddedQuestions
  end embeddedDescendants

  private def loadNode(ws: ReadWorkspace, id: Long): Asset[?] = assetNodeService.load(ws).byId(id).get

  private def execute(command: IndexCommand): Unit =
    logger.debug(s"Index: $command")
    command match
      case IndexDocument(document)                         =>
        esService.indexDocument(document)
      case UpdateDocument(branch, offering, name, fields*) =>
        esService.updateDocument(branch, offering | 0L, name, fields*)
      case DeleteDocument(branch, offering, name)          =>
        esService.deleteDocument(branch, offering | 0L, name)
      case DeleteBranch(branch, offering)                  =>
        esService.deleteByQuery(EsQuery(branch = Some(branch), offering = offering || Some(0L)))
  end execute
end IndexServiceImpl

object IndexServiceImpl:
  private final val logger = org.log4s.getLogger

  /** The asset types that DCM can directly navigate to. */
  final val WalkableTypeIds =
    AssetTypeId.QuestionTypes ++ AssetTypeId.LessonElementTypes +
      AssetTypeId.Lesson + AssetTypeId.Module + AssetTypeId.Course + AssetTypeId.Rubric ++
      AssetTypeId.SurveyQuestionTypes + AssetTypeId.Survey1

  /** A simple algebra for indexing commands. */
  sealed trait IndexCommand:
    def widen: IndexCommand = this // IStream invariance...

  final case class IndexDocument(document: AssetNodeDocument) extends IndexCommand

  final case class UpdateDocument(branch: Long, offering: Option[Long], name: UUID, fields: (String, Any)*)
      extends IndexCommand

  final case class DeleteDocument(branch: Long, offering: Option[Long], name: UUID) extends IndexCommand

  final case class DeleteBranch(branch: Long, offering: Option[Long]) extends IndexCommand

  /** Find all the node ids in a [[ReadWorkspace]] that are reachable from a root. */
  def subtree(workspace: ReadWorkspace, rootId: Long, traverse: EdgeInfo => Boolean = _ => true): Set[Long] =
    walk[EdgeInfo](rootId, workspace.outEdgeInfos, traverse, _.targetId).to(Set)

  /** Find all the node ids in a [[ReadWorkspace]] that are reachable from a root. */
  def inOrderSubtree(
    workspace: ReadWorkspace,
    rootId: Long,
    traverse: EdgeInfo => Boolean = _ => true
  ): List[Long] =
    walk[EdgeInfo](rootId, workspace.outEdgeInfos(_).toSeq.sortBy(_.position), traverse, _.targetId).to(List)

  /** Find all the node ids in `workspace` that are ancestral to a leaf. */
  def supertree(workspace: ReadWorkspace, leafId: Long, traverse: EdgeInfo => Boolean = _ => true): Set[Long] =
    walk[EdgeInfo](leafId, workspace.inEdgeInfos, traverse, _.sourceId).to(Set)

  /** Walk an edgy tree accumulating ids. */
  private def walk[E](id: Long, edges: Long => Iterable[E], traverse: E => Boolean, whither: E => Long) =
    val result                   = mutable.LinkedHashSet.empty[Long] // :shame:
    def walkTree(id: Long): Unit =
      if result.add(id) then
        edges(id).filter(traverse) foreach { edge =>
          walkTree(whither(edge)) // while revolting, tree depth is very shallow
        }
    walkTree(id)
    result

  /** So... [[Stream]] isn't heapsafe, but [[IStream#foldMap]] isn't stacksafe. So our own adventure. Stackandheapsafely
    * fold-map over some [[F]] to [[Unit]].
    */
  implicit class FoldableUnitOps[F[_], A](private val self: F[A]) extends AnyVal:
    def foldMapU[B](f: A => Unit)(implicit Foldable: Foldable[F]): Unit = Foldable.foldLeft(self, ())((_, a) => f(a))

  /** [[Monoid]] for [[IStream]]. */
  implicit def IStreamMonoid[A]: Monoid[IStream[A]] = Monoid.instance(_ !! _, IStream.empty)
end IndexServiceImpl

final case class CommitInfo(
  branchId: Long,
  modifiedNodes: Set[UUID],
  demotedNodes: Set[UUID],
  promotedNodes: Set[UUID],
)

object CommitInfo:
  def apply[A <: AttachedReadWorkspace](commit: CommitResult[A]): CommitInfo =
    CommitInfo(
      branchId = commit.ws.bronchId,
      modifiedNodes = commit.newNodes.keySet,
      demotedNodes = commit.demotedNodes,
      promotedNodes = commit.promotedNodes
    )
