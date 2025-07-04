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

package loi.authoring.copy

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.operation.Operations
import com.learningobjects.cpxp.service.domain.{DomainDTO, DomainFinder}
import com.learningobjects.cpxp.service.user.{UserDTO, UserFinder}
import com.learningobjects.cpxp.util.HibernateSessionOps.*
import com.learningobjects.cpxp.util.task.Priority
import com.learningobjects.de.task.{TaskReportService, UnboundedTaskReport}
import loi.asset.course.model.Course
import loi.asset.root.model.Root
import loi.authoring.asset.Asset
import loi.authoring.branch.Branch
import loi.authoring.copy.store.{CopyReceiptDao, CopyReceiptEntity}
import loi.authoring.edge.{DurableEdgeService, EdgeService}
import loi.authoring.index.ReindexService
import loi.authoring.node.AssetNodeService
import loi.authoring.project.*
import loi.authoring.workspace.*
import loi.authoring.write.*
import org.hibernate.Session
import scaloi.misc.TimeSource

import java.util.{Date, UUID}

@Service
class CopyService(
  copyReceiptDao: CopyReceiptDao,
  durableEdgeService: DurableEdgeService,
  edgeService: EdgeService,
  nodeService: AssetNodeService,
  projectService: BaseProjectService,
  session: => Session,
  ts: TimeSource,
  userDto: => UserDTO,
  currentDomain: => DomainDTO,
  workspaceService: WorkspaceService,
  writeService: BaseWriteService,
  reindexService: ReindexService,
):
  import CopyService.*

  /* this takes a ReadWorkspace despite modifying the branch, since copy
   * can be slow and we don't want to hold on to the branch lock for the
   * duration of the copy operation...
   */
  def deferDeepCopy(
    workspace: AttachedReadWorkspace,
    node: Asset[?],
    newTitle: String
  ): CopyReceipt =
    deepCopyInternal(workspace, node, newTitle, defer = true)

  def doDeepCopy(
    workspace: AttachedReadWorkspace,
    node: Asset[?],
    newTitle: String
  ): CopyReceipt =
    deepCopyInternal(workspace, node, newTitle, defer = false)

  private def deepCopyInternal(
    workspace: AttachedReadWorkspace,
    node: Asset[?],
    newTitle: String,
    defer: Boolean
  ): CopyReceipt =
    val receipt = newReceipt(node)
    val op      = new SubGraphCopyOperation(
      copyReceiptDao,
      nodeService,
      edgeService,
      workspaceService,
      writeService,
      receipt,
      workspace,
      node,
      newTitle
    )
    deferOrPerform(op, defer)
  end deepCopyInternal

  private def deferOrPerform(
    op: CopyOperation,
    defer: Boolean
  ) =
    if defer then
      Operations.deferTransact(op, Priority.High, s"Authoring-Node-Copy-(${op.receipt.id})")
      op.receipt
    else
      op.perform()
      loadReceipt(op.receipt.id).get

  def loadReceipt(id: Long): Option[CopyReceipt] =
    copyReceiptDao.loadReceipt(id).map(CopyReceiptDao.entityToReceipt)

  def deleteReceipt(receipt: CopyReceipt): Unit =
    copyReceiptDao.deleteReceipt(copyReceiptDao.loadReference(receipt))

  /** Performs a deep copy of the project. Which is a slower operation than shallow copies and is only able to copy a
    * single branch, but it can copy to another domain.
    */
  def deepCopyProject(
    branch: Branch,
    destinationDomain: DomainDTO,
    name: String,
    defer: Boolean = true
  ): CopyReceipt =
    val workspace = workspaceService.requireReadWorkspace(branch.id)
    val rootNode  = nodeService.load(workspace).byName(workspace.rootName).get
    val receipt   = newReceipt(rootNode)
    val op        =
      new ProjectCopyOperation(
        copyReceiptDao,
        nodeService,
        edgeService,
        projectService,
        workspaceService,
        writeService,
        receipt,
        workspace,
        name,
        destinationDomain,
        userDto
      )
    deferOrPerform(op, defer)
  end deepCopyProject

  def shallowCopyProject(src: Branch, newProjectName: String): Branch =
    shallowCopyProject(src, CreateProjectDto(newProjectName, ProjectType.Course, userDto.id))

  /** Copies `src` without copying any nodes and edges. The new project's head commit is `src.head` */
  def shallowCopyProject(src: Branch, dto: CreateProjectDto): Branch =

    val owner      = session.ref[UserFinder](dto.createdBy)
    val root       = session.ref[DomainFinder](currentDomain.id)
    val head       = session.ref[CommitEntity2](src.head.id)
    val srcProject = session.ref[ProjectEntity2](src.id)

    val tgt2 = new ProjectEntity2(
      id = null,
      name = dto.projectName,
      head = head,
      created = ts.localDateTime,
      createdBy = owner,
      ownedBy = owner,
      contributors = new java.util.ArrayList(),
      archived = false,
      published = false,
      del = null,
      code = dto.code.orNull,
      productType = dto.productType.orNull,
      category = dto.category.orNull,
      subCategory = dto.subCategory.orNull,
      revision = dto.revision.map(Int.box).orNull,
      launchDate = dto.launchDate.orNull,
      liveVersion = dto.liveVersion.orNull,
      s3 = dto.s3.orNull,
      configuration = srcProject.configuration,
      maintenance = null,
      root = root
    )

    session.persist(tgt2)

    val tgt0 = projectService.loadBronch(tgt2.id, AccessRestriction.none).get

    val tgt = updateCopiedProject(tgt0, dto)
    durableEdgeService.copy(src, tgt)

    logger.info(s"shallow-copied bronch ${src.id} to ${tgt.id}")

    reindexService.indexBranch(tgt.id) // for the title-edited course.1

    tgt
  end shallowCopyProject

  private def updateCopiedProject(branch: Branch, dto: CreateProjectDto): Branch =
    val ws       = workspaceService.requireWriteWorkspace(branch.id, AccessRestriction.none)
    val rootNode = nodeService.loadA[Root](ws).byName(ws.rootName).get
    val homeNode = nodeService.loadA[Course](ws).byName(ws.homeName).get
    val result   = writeService
      .commit(
        ws,
        List(
          SetNodeData(ws.rootName, rootNode.data.copy(title = dto.projectName, projectStatus = dto.projectStatus)),
          SetNodeData(ws.homeName, homeNode.data.copy(title = dto.projectName, contentStatus = dto.courseStatus))
        )
      )
      .get
    result.ws.branch
  end updateCopiedProject

  private def newReceipt(source: Asset[?]): CopyReceipt =

    val report = new UnboundedTaskReport(s"Copying node ${source.info.id}")
    TaskReportService.track(report)

    val receiptEntity = new CopyReceiptEntity(
      null,
      source.info.id,
      null,
      JacksonUtils.getFinatraMapper.valueToTree(report),
      CopyReceiptStatus.REQUESTED.name(),
      new Date(),
      null,
      null,
      userDto.id,
      currentDomain.id
    )

    copyReceiptDao.save(receiptEntity)
    CopyReceiptDao.entityToReceipt(receiptEntity)
  end newReceipt
end CopyService

object CopyService:
  private final val logger = org.log4s.getLogger

private case class CopyState(
  copiedNodes: Map[Long, UUID], // original node id to new node name
  addNodeOps: List[ValidatedAddNode[?]],
  addEdgeOps: List[ValidatedAddEdge],
  receipt: CopyReceipt
):

  def markProgress(progressUnits: Int): CopyState =
    copy(
      receipt = receipt.markProgress(progressUnits)
    )
end CopyState

private object CopyState:
  def empty(receipt: CopyReceipt) = CopyState(Map.empty, Nil, Nil, receipt)
