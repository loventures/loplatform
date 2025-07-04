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

package loi.authoring.dropbox

import com.learningobjects.cpxp.component.*
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.ApiQueries.*
import com.learningobjects.cpxp.component.query.{ApiQueries, ApiQuery, ApiQueryResults, PredicateOperator}
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.dto.Ontology
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.cpxp.Item.*
import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import com.learningobjects.cpxp.service.attachment.{AttachmentFinder, AttachmentWebService}
import com.learningobjects.cpxp.service.authoring.dropbox.DropboxFileFinder
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.query.QueryService
import com.learningobjects.cpxp.service.user.{UserDTO, UserFinder}
import com.learningobjects.cpxp.util.{FormattingUtils, HttpUtils, MimeUtils}
import com.learningobjects.cpxp.web.ExportFile.cleanFilename
import loi.authoring.folder.AuthoringFolderFacade
import loi.authoring.node.AssetNodeService
import loi.authoring.project.AccessRestriction
import loi.authoring.security.right.{EditContentAnyProjectRight, ViewAllProjectsRight}
import loi.authoring.web.AuthoringWebUtils
import loi.authoring.workspace.WorkspaceService
import loi.cp.attachment.AttachmentComponent
import loi.cp.right.Right
import loi.cp.web.HandleService
import org.apache.commons.io.IOUtils
import scalaz.\/
import scalaz.std.option.*
import scalaz.syntax.either.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scaloi.\|/
import scaloi.\|/.{That, This}
import scaloi.misc.TimeSource
import scaloi.syntax.`try`.*
import scaloi.syntax.boolean.*
import scaloi.syntax.option.*

import java.lang
import java.util.zip.{ZipEntry, ZipOutputStream}
import java.util.{Date, UUID}
import scala.jdk.CollectionConverters.*
import scala.reflect.ClassTag
import scala.util.Using

// This class leaves all filename conflict management to the front end..

@Component
@Controller(value = "dopbox", root = true)
@RequestMapping(path = "authoring/dropbox")
class DropboxWebController(
  val componentInstance: ComponentInstance,
  domain: DomainDTO,
  user: UserDTO,
  now: TimeSource
)(implicit
  is: ItemService,
  fs: FacadeService,
  qs: QueryService,
  hs: HandleService,
  cs: ComponentService,
  ontology: Ontology,
  webUtils: AuthoringWebUtils,
  workspaceService: WorkspaceService,
  nodeService: AssetNodeService,
  attachmentService: AttachmentWebService,
) extends ApiRootComponent
    with ComponentImplementation:

  @RequestMapping(path = "{project}", method = Method.GET)
  def getFiles(@PathVariable("project") project: Long, query: ApiQuery): ApiQueryResults[DropboxFileDto] =
    queryDropboxFinders[ViewAllProjectsRight](project, query).map(DropboxFileDto.apply)

  @RequestMapping(path = "{project}/{id}", method = Method.GET)
  def getFile(@PathVariable("project") project: Long, @PathVariable("id") id: Long): Option[DropboxFileDto] =
    queryDropboxFinder[ViewAllProjectsRight](project, id).map(DropboxFileDto.apply)

  @RequestMapping(path = "{project}", method = Method.POST)
  def uploadFile(
    @PathVariable("project") projectId: Long,
    @RequestBody dto: NewDropboxItem
  ): ErrorResponse \/ DropboxFileDto =
    val workspace =
      workspaceService.requireReadWorkspace(dto.branch, AccessRestriction.projectMemberOr[EditContentAnyProjectRight])

    for
      _      <- (workspace.projectInfo.id == projectId) \/> ErrorResponse.badRequest("Branch not in project")
      _      <- nodeService.load(workspace).byName(dto.asset) \/> (e => ErrorResponse.badRequest(e.getMessage))
      folder <- dto.folder.traverse(loadForEdit(projectId, _))
      upload <- getUpload(dto)
    yield
      val file = authoringFolder.addAttachment(upload).finder[AttachmentFinder]
      upload.destroy() // let's just make sure we delete these files promptly as they upload so much

      val finder = authoringFolder.addChild[DropboxFileFinder] { finder =>
        finder.project = projectId
        finder.branch = dto.branch
        finder.asset = dto.asset
        finder.archived = false
        finder.folder = folder.orNull
        finder.file = file
      }

      DropboxFileDto(finder)
    end for
  end uploadFile

  private def getUpload(dto: NewDropboxItem): ErrorResponse \/ UploadInfo = \|/(dto.file, dto.directory) match
    case This(upload) =>
      // deliberately no filetype restrictions, authors need to be able to upload anything...
      upload.right
    case That(dir)    =>
      new UploadInfo(dir, "application/x-directory", null, false).right
    case _            =>
      ErrorResponse.badRequest("File xor directory needed").left

  @RequestMapping(path = "{project}/{id}/archive", method = Method.POST)
  def archiveFile(
    @PathVariable("project") project: Long,
    @PathVariable("id") id: Long,
    @RequestBody archive: Archive,
  ): ErrorResponse \/ DropboxFileDto = updateFile(project, id) { finder =>
    (finder.archived = archive.archived).right
  }

  @RequestMapping(path = "{project}/{id}/rename", method = Method.POST)
  def renameFile(
    @PathVariable("project") project: Long,
    @PathVariable("id") id: Long,
    @RequestBody rename: Rename,
  ): ErrorResponse \/ DropboxFileDto = updateFile(project, id) { finder =>
    (finder.file.fileName = rename.fileName).right
  }

  @RequestMapping(path = "{project}/{id}/move", method = Method.POST)
  def moveFile(
    @PathVariable("project") project: Long,
    @PathVariable("id") id: Long,
    @RequestBody move: Move,
  ): ErrorResponse \/ DropboxFileDto =
    updateFile(project, id) { finder =>
      for
        folder <- move.folder.traverse(loadForEdit(project, _))
        _      <- folder.traverse(checkAncestry(_, id))
      yield finder.folder = folder.orNull
    }

  // Can't move id into itself or a descendant
  private def checkAncestry(folder: DropboxFileFinder, id: Long): ErrorResponse \/ Unit =
    for
      _ <- (folder.id == id) \/>! ErrorResponse.badRequest
      _ <- Option(folder.folder).traverse(checkAncestry(_, id))
    yield ()

  private def updateFile(project: Long, id: Long)(
    f: DropboxFileFinder => ErrorResponse \/ Unit
  ): ErrorResponse \/ DropboxFileDto =
    for
      finder <- loadForEdit(project, id)
      _      <- f(finder)
    yield DropboxFileDto(finder)

  private def loadForEdit(project: Long, id: Long): ErrorResponse \/ DropboxFileFinder =
    queryDropboxFinder[EditContentAnyProjectRight](project, id) \/> ErrorResponse.notFound

  @RequestMapping(path = "{project}/{id}/download", method = Method.GET)
  def downloadFile(
    @PathVariable("project") project: Long,
    @PathVariable("id") id: Long,
  ): ErrorResponse \/ WebResponse =
    for finder <- queryDropboxFinder[ViewAllProjectsRight](project, id) \/> ErrorResponse.notFound
    yield
      val attachment = finder.file.component[AttachmentComponent]
      attachment.view(true, false, null)

  @RequestMapping(path = "{project}/download", method = Method.GET)
  def downloadFiles(
    @PathVariable("project") project: Long,
    @QueryParam(value = "id", decodeAs = classOf[Long]) ids: List[Long],
  ): ErrorResponse \/ WebResponse =
    val files = queryDropboxFinders[ViewAllProjectsRight](
      project,
      ApiQuery.byIds(ids.mkString(",")).sublet[DropboxFileFinder]
    ).asScala
    if files.length == 1 && (files.head.file.digest ne null) then downloadFile(project, files.head.id)
    else
      val zipName =
        if files.length == 1 then cleanFilename(files.head.file.fileName + ".zip")
        else "Project Dropbox Files.zip"

      DirectResponse { resp =>
        resp.setContentType(MimeUtils.MIME_TYPE_APPLICATION_ZIP)
        resp.setHeader(
          HttpUtils.HTTP_HEADER_CONTENT_DISPOSITION,
          HttpUtils.getDisposition(HttpUtils.DISPOSITION_ATTACHMENT, zipName)
        )

        Using.resource(new ZipOutputStream(resp.getOutputStream)) { zos =>
          def writeFiles(files: Iterable[DropboxFileFinder], prefix: String): Unit =
            files foreach { file =>
              val fileName = cleanFilename(file.file.fileName)
              if file.file.digest eq null then // directory
                val directory = s"$prefix$fileName/"
                zos.putNextEntry(new ZipEntry(directory))
                zos.closeEntry()

                val subQuery = new ApiQuery.Builder()
                  .addPrefilter("folder", PredicateOperator.EQUALS, file.id.toString)
                  .addPrefilter("archived", PredicateOperator.EQUALS, "false")
                  .build
                val children = queryDropboxFinders[ViewAllProjectsRight](project, subQuery).asScala
                writeFiles(children, directory)
              else
                zos.putNextEntry(new ZipEntry(s"$prefix$fileName"))
                val blob = attachmentService.getAttachmentBlob(file.file.id)
                Using.resource(blob.getBlob.getPayload.openStream) { is =>
                  IOUtils.copy(is, zos)
                }
                zos.closeEntry()
              end if
            }

          writeFiles(files, "")
        }
      }.right[ErrorResponse]
    end if
  end downloadFiles

  private def queryDropboxFinder[A <: Right: ClassTag](projectId: Long, id: Long): Option[DropboxFileFinder] =
    queryDropboxFinders[A](projectId, ApiQuery.byId(id).sublet[DropboxFileFinder]).asOption

  private def queryDropboxFinders[A <: Right: ClassTag](
    projectId: Long,
    query: ApiQuery
  ): ApiQueryResults[DropboxFileFinder] =
    val project      = webUtils.projectOrThrow404(projectId, AccessRestriction.projectMemberOr[A])
    val projectQuery = new ApiQuery.Builder(query)
      .addPrefilter("project", PredicateOperator.EQUALS, project.id.toString)
      .build
    // disable caching because authors don't do this a lot and else we have to invalidating caches above
    val queryFiles   = authoringFolder.queryChildren[DropboxFileFinder].setCacheQuery(false)
    ApiQueries.queryFinder[DropboxFileFinder](queryFiles, projectQuery)
  end queryDropboxFinders

  private def authoringFolder: AuthoringFolderFacade =
    AuthoringFolderFacade.Identifier.facade[AuthoringFolderFacade]
end DropboxWebController

final case class NewDropboxItem(
  branch: Long,
  asset: UUID,
  folder: Option[lang.Long],
  directory: Option[String],
  file: Option[UploadInfo],
)

final case class Archive(archived: Boolean)

final case class Rename(fileName: String)

final case class Move(folder: Option[lang.Long])

final case class DropboxFileDto(
  id: Long,
  project: Long,
  branch: Long,
  asset: UUID,
  archived: Boolean,
  created: Date,
  creator: Option[ProfileDto],
  fileName: String,
  size: Long,
  directory: Boolean,
  folder: Option[Long],
)

object DropboxFileDto:
  def apply(file: DropboxFileFinder)(implicit hs: HandleService, is: ItemService): DropboxFileDto =
    new DropboxFileDto(
      id = file.id,
      project = file.project,
      branch = file.branch,
      asset = file.asset,
      archived = file.archived,
      created = file.file.createTime,
      creator = file.file.creator.finder_?[UserFinder].map(ProfileDto.apply),
      fileName = file.file.fileName,
      size = file.file.size,
      directory = file.file.digest eq null,
      folder = Option(file.folder).map(_.id)
    )
end DropboxFileDto

final case class ProfileDto(
  id: Long,
  handle: String,
  givenName: String,
  fullName: String,
  thumbnailId: Option[Long],
)

object ProfileDto:
  def apply(user: UserFinder)(implicit hs: HandleService): ProfileDto =
    new ProfileDto(
      id = user.id,
      handle = hs.mask(user),
      givenName = user.givenName,
      fullName = FormattingUtils.userStr(user.userName, user.givenName, user.middleName, user.familyName),
      thumbnailId = Option(user.image).unboxMap(_.generation),
    )
