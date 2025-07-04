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

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.user.UserWebService
import com.learningobjects.cpxp.util.GuidUtil
import loi.authoring.blob.BlobService
import loi.authoring.exchange.imprt.web.LoafImportRequest
import loi.authoring.exchange.imprt.{ConvertedImportDto, File2BlobService, ImportError, ImportService}
import loi.authoring.workspace.WorkspaceService
import loi.cp.asset.exchange.impl.AssetExchangeRequestStatus
import loi.cp.bootstrap.Bootstrap
import loi.cp.offering.ProjectOfferingService
import org.log4s.Logger

@Component
class ProjectBootstrap(val componentInstance: ComponentInstance)(
  userService: UserWebService,
  projectService: BaseProjectService,
  importService: ImportService,
  workspaceService: WorkspaceService,
  blobService: BlobService,
  file2BlobService: File2BlobService,
  projectOfferingService: ProjectOfferingService,
  domainDto: => DomainDTO,
) extends ComponentImplementation:
  import ProjectBootstrap.*

  // significant duplication with ImportWebController.importProjectLoaf
  @Bootstrap("project.importCourse")
  def importCourse(bootstrapDto: BootstrapImportProjectDto): Unit =

    val createdBy = userService.getUserByUserName(bootstrapDto.createdBy).getId

    val rootlessProject = projectService
      .insertProject(
        CreateProjectDto(bootstrapDto.projectName, ProjectType.Course, createdBy),
        domainDto.id,
        None,
        None
      )
      .valueOr(errors =>
        throw new RuntimeException(ImportError.OtherError(errors.list.toList.map(_.value).mkString(", ")).value)
      )

    val provider = blobService.getDefaultProvider
    val blobName = file2BlobService.createBlobName(bootstrapDto.zip.getFile, "authoring/")
    val blobRef  = blobService.putBlob(provider, blobName, bootstrapDto.zip).get
    val dataJson = json(LoafImportRequest(bootstrapDto.importDescription, blobRef))

    val importDto        = ConvertedImportDto(rootlessProject, bootstrapDto.importDescription, dataJson, blobRef)
    val completedReceipt = importService.doImport(importDto)

    if completedReceipt.status != AssetExchangeRequestStatus.Success then
      throw new RuntimeException(
        s"Import failed ${GuidUtil.errorGuid()}: ${json(completedReceipt.report).toString}"
      )

    val names = completedReceipt.importedRoots.map(r => s"(${r.typeId} ${r.name})").mkString("[", ",", "]")
    logger.info(s"Successfully imported $names")

    val ws = workspaceService.requireReadWorkspace(rootlessProject.id, AccessRestriction.none)

    if bootstrapDto.publish then
      projectOfferingService.publishProject(ws.projectInfo)
      logger.info(s"Published project ${ws.projectInfo.name}")

    for
      offeringId <- bootstrapDto.offeringId
      offering   <- projectOfferingService.getOfferingComponentForBranch(ws.branch)
    yield offering.setGroupId(offeringId)
  end importCourse

  private def json(anyRef: AnyRef): JsonNode = JacksonUtils.getFinatraMapper.valueToTree[JsonNode](anyRef)
end ProjectBootstrap

// import project, publish it (create offering), optionally set the groupid of the offering
case class BootstrapImportProjectDto(
  projectName: String,
  createdBy: String,
  importDescription: String,
  zip: UploadInfo,
  publish: Boolean,
  offeringId: Option[String]
)

object ProjectBootstrap:
  private val logger: Logger = org.log4s.getLogger
