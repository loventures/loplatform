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

import argonaut.Argonaut.*
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.dto.{Facade, FacadeComponent, FacadeCondition, FacadeItem}
import com.learningobjects.cpxp.operation.Operations
import com.learningobjects.cpxp.service.component.ComponentConstants
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.folder.FolderConstants
import loi.authoring.configuration.AuthoringConfigurationConstants
import loi.authoring.index.ReindexServiceImpl.ReindexCommand
import loi.cp.bus.MessageBusService
import loi.cp.config.ConfigurationService
import loi.cp.integration.SystemComponent

import java.util.UUID
import scala.reflect.ClassTag

@Service
trait ReindexService extends OfferingIndexService: // the reindex is like a reindeer but with recurved horns
  def indexBranch(branchId: Long): Unit
  def indexAsset(branchId: Long, name: UUID): Unit
  def indexCommit(domainId: Long, commit: CommitInfo): Unit
  def deleteBranch(branchId: Long): Unit
  def deleteProject(projectId: Long): Unit
  def projectRetired(projectId: Long, retired: Boolean): Unit
  def projectMetadata(projectId: Long, metadata: ProjectMetadata): Unit

  private[index] def execute(command: ReindexCommand): Unit
end ReindexService

object NoindexService extends ReindexService:
  override def indexBranch(branchId: Long): Unit                                     = ()
  override def indexOffering(offeringId: Long, branchId: Long, commitId: Long): Unit = ()
  override def indexAsset(branchId: Long, name: UUID): Unit                          = ()
  override def indexCommit(domainId: Long, commit: CommitInfo): Unit                 = ()
  override def deleteBranch(branchId: Long): Unit                                    = ()
  override def deleteProject(projectId: Long): Unit                                  = ()
  override def projectRetired(projectId: Long, retired: Boolean): Unit               = ()
  override def projectMetadata(projectId: Long, metadata: ProjectMetadata): Unit     = ()
  override def execute(command: ReindexCommand): Unit                                = ()
end NoindexService

@Service
class ReindexServiceImpl(
  esService: EsService,
  indexService: IndexService,
  messageBusService: MessageBusService,
  domain: => DomainDTO
)(implicit configurationService: ConfigurationService, facadeService: FacadeService)
    extends ReindexService:
  import ReindexServiceImpl.*

  override def indexBranch(branchId: Long): Unit = proffer(ReindexBranch(branchId))

  override def indexOffering(offeringId: Long, branchId: Long, commitId: Long): Unit =
    proffer(ReindexOffering(offeringId, branchId, commitId))

  override def indexAsset(branchId: Long, name: UUID): Unit = proffer(ReindexAsset(branchId, name))

  override def indexCommit(domainId: Long, commit: CommitInfo): Unit =
    if domainId == domain.id then proffer(ReindexCommit(commit))
    else Operations.asDomain(domainId, () => proffer(ReindexCommit(commit)))

  override def deleteBranch(branchId: Long): Unit = proffer(DeleteBranch(branchId))

  override def deleteProject(projectId: Long): Unit = proffer(DeleteProject(projectId))

  override def projectRetired(projectId: Long, retired: Boolean): Unit = proffer(ProjectRetired(projectId, retired))

  override def projectMetadata(projectId: Long, metadata: ProjectMetadata): Unit = proffer(
    ProjectMetadatad(projectId, metadata)
  )

  private def proffer(command: ReindexCommand): Unit =
    val config = AuthoringConfigurationConstants.domainAuthoringConfig.getDomain
    if config.synchronousIndexing then execute(command)
    else messageBusService.publishMessage(EsSystemImpl.getOrCreate(), command)

  override private[index] def execute(command: ReindexCommand): Unit =
    command match
      case ReindexBranch(branchId) =>
        indexService.indexBranch(branchId) // arguably this is slower than permitted by the message bus...

      case ReindexOffering(offeringId, branchId, commitId) =>
        indexService.indexOffering(
          offeringId,
          branchId,
          commitId,
          delete = true
        ) // arguably this is slower than permitted by the message bus...

      case ReindexAsset(branchId, name) => // treat this like a commit so ancestors are done
        indexService.indexCommit(CommitInfo(branchId, Set(name), Set.empty, Set.empty))

      case ReindexCommit(commit) =>
        indexService.indexCommit(commit)

      case DeleteBranch(branchId) =>
        esService.deleteByQuery(EsQuery(branch = Some(branchId)))

      case BranchArchived(branchId, archived) =>
        esService.updateByQuery(EsQuery(branch = Some(branchId)), "branchArchived" -> archived.asJson.nospaces)

      case DeleteProject(projectId) =>
        esService.deleteByQuery(EsQuery(project = Some(projectId)))

      case ProjectRetired(projectId, retired) =>
        esService.updateByQuery(EsQuery(project = Some(projectId)), "projectRetired" -> retired.asJson.nospaces)

      case ProjectMetadatad(projectId, metadata) =>
        esService.updateByQuery(
          EsQuery(project = Some(projectId)),
          metadata.asJson.objectOrEmpty.toList.map(kv => s"projectMetadata.${kv._1}" -> kv._2.nospaces)*
        )
end ReindexServiceImpl

object ReindexServiceImpl:

  /** An algebra of reindexing messages. */
  sealed trait ReindexCommand

  final case class ReindexBranch(branchId: Long) extends ReindexCommand

  final case class ReindexOffering(offeringId: Long, branchId: Long, commitId: Long) extends ReindexCommand

  final case class ReindexAsset(branchId: Long, name: UUID) extends ReindexCommand

  final case class ReindexCommit(commit: CommitInfo) extends ReindexCommand

  final case class DeleteBranch(branchId: Long) extends ReindexCommand

  final case class BranchArchived(branchId: Long, archived: Boolean) extends ReindexCommand

  final case class DeleteProject(projectId: Long) extends ReindexCommand

  final case class ProjectRetired(projectId: Long, retired: Boolean) extends ReindexCommand

  final case class ProjectMetadatad(projectId: Long, metadata: ProjectMetadata) extends ReindexCommand
end ReindexServiceImpl

@FacadeItem(FolderConstants.ITEM_TYPE_FOLDER)
trait SystemParentFacade extends Facade:
  @FacadeComponent def getOrCreateEsSystem[T <: SystemComponent[?]: ClassTag](
    @FacadeCondition(value = ComponentConstants.DATA_TYPE_COMPONENT_IDENTIFIER) systemId: String
  ): T
