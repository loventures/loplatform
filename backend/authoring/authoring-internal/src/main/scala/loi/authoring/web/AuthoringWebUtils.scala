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

package loi.authoring.web

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.exception.HttpApiException.*
import com.learningobjects.cpxp.service.exception.*
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.de.web.UncheckedMessageException
import loi.authoring.AssetType
import loi.authoring.asset.Asset
import loi.authoring.asset.exception.NoSuchAssetType
import loi.authoring.asset.service.exception.NoSuchAssetException
import loi.authoring.bank.service.exception.NoSuchBranchException
import loi.authoring.branch.Branch
import loi.authoring.commit.exception.NoSuchCommitException
import loi.authoring.edge.Group
import loi.authoring.edge.service.exception.EdgeException.{NoSuchGroup, UnreachableTargetType}
import loi.authoring.edge.web.exception.InvalidEdgeName
import loi.authoring.node.AssetNodeService
import loi.authoring.node.web.exception.InvalidNodeName
import loi.authoring.project.exception.*
import loi.authoring.project.{AccessRestriction, Project, ProjectService}
import loi.authoring.workspace.exception.NoSuchNodeInWorkspaceException
import loi.authoring.workspace.{AttachedReadWorkspace, ReadWorkspace, WorkspaceService, WriteWorkspace}
import loi.cp.i18n.AuthoringBundle
import loi.cp.i18n.syntax.bundleMessage.*
import loi.cp.right.Right
import loi.cp.user.UserService
import org.apache.http.HttpStatus
import scalaz.NonEmptyList
import scaloi.syntax.OptionOps.*

import java.util.UUID
import scala.jdk.CollectionConverters.*
import scala.reflect.ClassTag
import scala.util.Try
import scala.util.control.NonFatal

@Service
class AuthoringWebUtils(
  nodeService: AssetNodeService,
  projectService: ProjectService,
  workspaceService: WorkspaceService,
  userDto: => UserDTO,
  userService: => UserService
) extends AuthoringApiWebUtils:

  override def workspaceOrThrow404(
    bronchId: Long,
    accessRestriction: AccessRestriction,
    cache: Boolean
  ): AttachedReadWorkspace =
    workspaceService
      .loadReadWorkspace(bronchId, accessRestriction, cache)
      .getOrElse(throw HttpApiException.notFound(NoSuchBranchException(bronchId.toString)))

  override def workspaceAtCommitOrThrow404(
    bronchId: Long,
    commitId: Long,
    accessRestriction: AccessRestriction,
    cache: Boolean
  ): AttachedReadWorkspace =
    workspaceService
      .loadReadWorkspaceAtCommit(bronchId, commitId, accessRestriction, cache)
      .getOrElse(throw HttpApiException.notFound(NoSuchBranchException(bronchId.toString)))

  override def detachedWorkspaceOrThrow404(commitId: Long, cache: Boolean): ReadWorkspace =
    workspaceService
      .loadDetachedWorkspace(commitId, cache)
      .getOrElse(throw HttpApiException.notFound(NoSuchCommitException(commitId)))

  def writeWorkspaceOrThrow404(bronchId: Long): WriteWorkspace =
    workspaceService
      .loadWriteWorkspace(bronchId)
      .getOrElse(throw HttpApiException.notFound(NoSuchBranchException(bronchId.toString)))

  override def branchOrFakeBranchOrThrow404(bronchId: Long, accessRestriction: AccessRestriction): Branch =
    projectService
      .loadBronch(bronchId, accessRestriction)
      .getOrElse(throw HttpApiException.notFound(NoSuchBranchException(bronchId.toString)))

  override def masterOrFakeBranchOrThrow404(projectId: Long, accessRestriction: AccessRestriction): Branch =
    projectService
      .loadProjectAsMasterBranch(projectId, accessRestriction)
      .getOrElse(throw HttpApiException.notFound(NoSuchProjectIdException(projectId)))

  override def projectOrThrow404(id: Long, accessRestriction: AccessRestriction): Project =
    projectService
      .loadProject(id, accessRestriction)
      .getOrElse(throw HttpApiException.notFound(NoSuchProjectIdException(id)))

  private def nodeNameOrThrow404(nodeName: String): UUID =
    parseNodeName(nodeName)
      .recover({ case inn: InvalidNodeName =>
        throw HttpApiException.notFound(inn)
      })
      .get

  def parseNodeName(nodeName: String): Try[UUID] =
    Try(UUID.fromString(nodeName)).recover({
      case npe: NullPointerException     => throw InvalidNodeName(nodeName)
      case iae: IllegalArgumentException => throw InvalidNodeName(nodeName)
    })

  def edgeNameOrThrow404(edgeName: String): UUID =
    parseEdgeName(edgeName)
      .recover({ case ien: InvalidEdgeName =>
        throw HttpApiException.notFound(ien)
      })
      .get

  private def parseEdgeName(nodeName: String) =
    Try(UUID.fromString(nodeName)).recover({
      case npe: NullPointerException     => throw InvalidNodeName(nodeName)
      case iae: IllegalArgumentException => throw InvalidNodeName(nodeName)
    })

  def nodeOrThrow404(workspace: ReadWorkspace, str: String): Asset[?] =

    val byId        = str.toLongOption.flatMap(id => nodeService.load(workspace).byId(id))
    lazy val byName = Try(UUID.fromString(str)).flatMap(name => nodeService.load(workspace).byName(name)).toOption

    byId
      .orElse(byName)
      .getOrElse(AuthoringBundle.noSuchAsset(str).throw404)

  def nodeOrThrow404(workspace: ReadWorkspace, nodeName: UUID): Asset[?] =
    nodeService.load(workspace).byName(nodeName).recover(nodeLoad404Recovery).get

  def nodeOrThrow404Typed[A: AssetType](workspace: ReadWorkspace, nodeNameStr: String): Asset[A] =
    val nodeName = nodeNameOrThrow404(nodeNameStr)
    nodeService.loadA(workspace).byName(nodeName).recover(nodeLoad404Recovery).get

  def nodeOrThrow404Typed[A: AssetType](ws: ReadWorkspace, id: Long): Asset[A] =
    nodeService.loadA[A](ws).byId(id).toTry(NoSuchAssetException(id)).recover(nodeLoad404Recovery).get

  // kill after laird migration
  def nodeOrThrow404ByGuessing(id: Long): Asset[?] =
    nodeService.loadRawByGuessing(id).getOrElse(AuthoringBundle.noSuchAsset(id).throw404)

  def nodeOrThrow422(workspace: ReadWorkspace, nodeName: UUID): Asset[?] =
    nodeService.load(workspace).byName(nodeName).recover(nodeLoad422Recovery).get

  private val nodeLoad404Recovery: PartialFunction[Throwable, Nothing] = nodeLoadRecovery(HttpApiException.notFound)
  private val nodeLoad422Recovery: PartialFunction[Throwable, Nothing] = nodeLoadRecovery(
    HttpApiException.unprocessableEntity
  )

  private def nodeLoadRecovery(
    f: UncheckedMessageException => HttpApiException
  ): PartialFunction[Throwable, Nothing] = {
    case ex: NoSuchNodeInWorkspaceException => throw f(ex)
    case ex: NoSuchAssetException           => throw f(ex)
  }

  // using 400 to simulate other type mismatch errors
  def groupOrThrow400(group: String): Group =
    Try(Group.withName(group))
      .recover({ case _: NoSuchElementException =>
        throw HttpApiException.badRequest(NoSuchGroup(group, None))
      })
      .get

  /** Throws an [[AccessForbiddenException]] if the current user is neither an owner nor a contributor to `project` and
    * does not have right `A`
    *
    * @param project
    *   the project to check access with
    * @tparam A
    *   a right that would allow the current user access even if not the owner and not a contributor
    */
  override def throw403ForNonProjectUserWithout[A <: Right: ClassTag](
    project: Project
  ): Unit =
    val isAllowed = project.userIds.contains(userDto.id) ||
      userService.userHasDomainRight[A](userDto.id)
    if !isAllowed then throw new AccessForbiddenException()

  /** Throws an [[AccessForbiddenException]] if the current user is not the owner of `project` and does not have right
    * `A`.
    * @param project
    *   the project to check access with
    * @tparam A
    *   a right that would allow the current user access even if not the owner
    */
  override def throw403ForNonProjectOwnerWithout[A <: Right: ClassTag](
    project: Project
  ): Unit =
    val isAllowed = project.ownedBy == userDto.id ||
      userService.userHasDomainRight[A](userDto.id)
    if !isAllowed then throw new AccessForbiddenException()

  // Converts a Nel of failures to a Java List
  def nel2JList[A](nel: NonEmptyList[A]): java.util.List[A] =
    nel.list.toList.asJava
end AuthoringWebUtils

object AuthoringWebUtils:

  val AsApiException: PartialFunction[Throwable, Throwable] = {
    case e: HttpApiException         => e
    case e: AccessForbiddenException =>
      new HttpApiException(
        AuthoringBundle.message("otherError", e.getMsg),
        e.getHttpStatusCode
      )

    case e: NotAContributorException       =>
      new HttpApiException(e.getErrorMessage, HttpStatus.SC_FORBIDDEN)
    case e: NoSuchBranchException          => notFound(e)
    case e: NoSuchAssetType                => unprocessableEntity(e)
    case e: NoSuchNodeInWorkspaceException => notFound(e)
    case e: UnreachableTargetType          => unprocessableEntity(e)
    case e: UncheckedMessageException      => badRequest(e)

    case e: Throwable if NonFatal(e) =>
      new HttpApiException(
        AuthoringBundle.message("otherError", e.getMessage),
        HttpStatus.SC_INTERNAL_SERVER_ERROR
      )
  }
end AuthoringWebUtils
