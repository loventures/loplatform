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

package loi.authoring.attachment.web

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{BaseComponent, ComponentInstance}
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.service.attachment.Disposition
import com.learningobjects.cpxp.service.blob.BlobPutLocation
import com.learningobjects.cpxp.service.exception.{AccessForbiddenException, HttpApiException}
import com.learningobjects.cpxp.util.BlobInfo
import com.learningobjects.de.authorization.Secured
import com.learningobjects.de.web.MediaType
import loi.asset.html.service.BaseHtmlService
import loi.authoring.AssetType
import loi.authoring.asset.Asset
import loi.authoring.asset.exception.NoSuchAssetType
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.blob.exception.{IllegalBlobName, IllegalMediaType}
import loi.authoring.blob.{BaseBlobService, BlobRef}
import loi.authoring.configuration.AuthoringConfigurationService
import loi.authoring.edge.EdgeService
import loi.authoring.index.TextExtractor
import loi.authoring.project.AccessRestriction
import loi.authoring.web.AuthoringWebUtils
import org.apache.http.HttpStatus
import scalaz.syntax.std.option.*
import scaloi.misc.TimeSource
import scaloi.syntax.any.*
import scaloi.syntax.date.*
import scaloi.syntax.option.*

import java.io.BufferedInputStream
import java.net.URI
import java.nio.charset.StandardCharsets
import scala.concurrent.duration.*
import scala.util.{Try, Using}

@Component
@Controller(value = "attachment-web-controller", root = true)
class AttachmentWebController(
  blobService: BaseBlobService,
  ci: ComponentInstance,
  webUtils: AuthoringWebUtils,
  now: TimeSource
)(implicit configService: AuthoringConfigurationService, edgeService: EdgeService)
    extends BaseComponent(ci)
    with ApiRootComponent:

  /** Same as `createBlobstorePutUrl`, but with a supported mediatype validation check based on the asset type of the
    * asset that is getting a new attachment.
    */
  @RequestMapping(path = "authoring/{branch}/nodes/{name}/attachmentUrl", method = Method.POST)
  def createBlobstorePutUrlWithSupportedMediaTypeValidation(
    @PathVariable("branch") branchId: Long,
    @PathVariable("name") nodeName: String,
    @RequestBody info: AttachmentUploadInfo
  ): Try[BlobPutLocation] =
    val workspace = webUtils.workspaceOrThrow404(branchId, cache = false)
    val node      = webUtils.nodeOrThrow404(workspace, nodeName)
    createPutUrl(info, node.assetType)

  /** Creates a PUT URL that a client can use to add the file directly to a blobstore (s3, local filesystem), rather
    * than having the file need to pass through the application server. The client is then expected to store the
    * relevant blobstore metadata (url, etag, digest) to the asset's data.
    *
    * browser -GET-> authoring/{branch}/nodes/{name}/attachmentUrl browser <- https://.../lo-dev-scratch?sig=12388
    * browser -PUT-> https://.../lo-dev-scratch?sig=12388 browser <- blobstore object browser -PUT-> blobstore object
    * onto asset
    */
  @RequestMapping(path = "authoring/attachmentUrl", method = Method.POST)
  def createBlobstorePutUrl(
    @RequestBody info: AttachmentUploadInfo
  ): Try[BlobPutLocation] =
    blobService
      .createPutUrl(info.blobName, info.mediaType, info.contentLength, None)
      .recover { case ex: IllegalBlobName =>
        throw HttpApiException.unprocessableEntity(ex)
      }

  /** Creates a PUT URL for uploading objects to a blobstore (s3, local filesystem), validating that the file type is
    * allowed for the asset type id
    *
    * @param typeId
    *   - the asset type id to validate against
    * @param info
    *   - metadata about the file
    * @return
    *   the url
    */
  @RequestMapping(path = "authoring/{typeId}/attachmentUrl", method = Method.POST)
  def createBlobstorePutUrl(
    @PathVariable("typeId") typeId: String,
    @RequestBody info: AttachmentUploadInfo
  ): Try[BlobPutLocation] =
    for
      assetType <- AssetType.types.get(AssetTypeId.withName(typeId)).toTry(NoSuchAssetType(typeId))
      url       <- createPutUrl(info, assetType)
    yield url

  // For when we care about checking if an asset's attachment supports a given mediatype
  private def createPutUrl(info: AttachmentUploadInfo, assetType: AssetType[?]): Try[BlobPutLocation] =
    blobService
      .createPutUrl(info.blobName, info.mediaType, info.contentLength, assetType)
      .recover {
        case ex: IllegalBlobName  => throw HttpApiException.unprocessableEntity(ex)
        case ex: IllegalMediaType => throw HttpApiException.unprocessableEntity(ex)
      }

  /** Blobstore: Checks if objectKey in the blobstore exists.
    *
    * @return
    *   a 204 response if the objectKey already exists or a 404 if the objectKey does not already exist
    */
  @RequestMapping(path = "authoring/objectKeyExists", method = Method.POST)
  def doHeadObjectCheck(
    @RequestBody objectKey: ObjectKey
  ): WebResponse =
    val provider   = blobService.getDefaultProvider
    val blobExists = blobService.blobExists(objectKey.objectKey, provider)
    if blobExists then NoContentResponse
    else ErrorResponse.notFound

  /** Blobstore: Puts a file attachment into the local blobstore.
    *
    * @param uploadInfo
    *   the file
    * @param name
    *   the /authoring + md5hash blob name
    */
  @RequestMapping(path = "authoring/attachment", method = Method.PUT, csrf = false)
  def putLocalBlobAttachment(@RequestBody uploadInfo: UploadInfo, @QueryParam(required = true) name: String): Unit =
    val provider = blobService.getDefaultProvider
    blobService.putBlob(provider, name, uploadInfo)

  /** Serves a file by usually issuing an S3 redirect. */
  @Secured(allowAnonymous = true)
  @RequestMapping(path = "authoring/nodes/{id}/serve", method = Method.GET, csrf = false)
  def serveById(@PathVariable("id") id: Long): WebResponse =
    // This will serve any asset to any logged in user. Really there ought to be a
    // course route to ensure it's an asset in the course, and
    // that the student is enrolled, but that was not considered worth doing.
    val node = webUtils.nodeOrThrow404ByGuessing(id)
    createFileServeResponse(node)

  /** Serves a file with caching headers and without issuing an S3 redirect, to allow access via CDN. Anonymous for web
    * assets okay as they have a low degree of sensitivity.
    *
    * This endpoint is intended for use in preview and test sections where putting a CDN on top of the vendor S3 bucket
    * would cause them content development tribulations.
    */
  @Secured(allowAnonymous = true)
  @RequestMapping(path = "authoring/nodes/{id}/webAsset", method = Method.GET, csrf = false)
  def serveWebAsset(@PathVariable("id") id: Long): WebResponse =
    val node = webUtils.nodeOrThrow404ByGuessing(id)
    if !ScriptTypes.contains(node.info.typeId) then throw new AccessForbiddenException()
    createFileServeResponse(node) ∂<| { case FileResponse(fileInfo, _, _) =>
      fileInfo.setNoRedirect(true)
      fileInfo.setExpires(7.days.toMillis)
    }
  end serveWebAsset

  /** Serves a file with caching headers after performing any configured CDN rewrites on the file content. Anonymous for
    * web assets okay as they have a low degree of sensitivity.
    *
    * This endpoint is intended for use in course sections where a CDN on top of the S3 bucket will give the students
    * the best experience.
    */
  @Secured(allowAnonymous = true)
  @RequestMapping(path = "authoring/nodes/{id}/cdnAsset", method = Method.GET, csrf = false)
  def serveCdnAsset(@PathVariable("id") id: Long): WebResponse =
    val node = webUtils.nodeOrThrow404ByGuessing(id)
    if !ScriptTypes.contains(node.info.typeId) then throw new AccessForbiddenException()
    createFileServeResponse(node) ∂|> { case FileResponse(fileInfo, _, _) =>
      Using.resource(fileInfo.openInputStream()) { in =>
        import com.google.common.net.MediaType.parse
        val script       = TextExtractor.extract(new BufferedInputStream(in))
        val mappedScript = BaseHtmlService.mapCdnUrls(script)
        val mediaType    = parse(fileInfo.getContentType).withCharset(StandardCharsets.UTF_8)
        TextResponse(mappedScript, mediaType, HttpStatus.SC_OK).cached(now.instant, 7.days)
      }
    }
  end serveCdnAsset

  private final val ScriptTypes: Set[AssetTypeId] = Set(AssetTypeId.Stylesheet, AssetTypeId.Javascript)

  /** Returns the ultimate target URL to serve the attachment itself. This exists because Safari will not render an
    * embedded PDF if the source issues a redirect which matters because we redirect to S3.
    *
    * @param id
    *   the id of the asset whose attachment is served
    * @return
    *   the ultimate target from which to serve the file
    */
  @RequestMapping(path = "authoring/nodes/{id}/url", method = Method.GET)
  def urlById(
    @PathVariable("id") id: Long,
  ): WebResponse =
    urlByResponse(serveById(id), s"/api/v2/authoring/nodes/$id/serve")

  @RequestMapping(path = "authoring/{branch}/nodes/{name}/url", method = Method.GET)
  def urlByBranchAndNodeName(
    @PathVariable("branch") branchId: Long,
    @PathVariable("name") nodeName: String
  ): WebResponse =
    urlByResponse(serve(branchId, nodeName), s"/api/v2/authoring/$branchId/nodes/$nodeName/serve")

  private def urlByResponse(response: WebResponse, orElse: String): WebResponse =
    response match
      case FileResponse(blob, _, _) =>
        ArgoResponse(
          Try(blob.getDirectUrl(Method.GET.name, (now.date + 1.minute).getTime))
            .getOrElse(orElse)
        )
      case otherResponse            => otherResponse

  @RequestMapping(
    path = "authoring/{branch}/commits/{commitId}/nodes/{nodeName}/serve",
    method = Method.GET,
    csrf = false
  )
  def serveByBranchCommitIdAndNodeName(
    @PathVariable("branch") branchId: Long,
    @PathVariable("commitId") commitId: Long,
    @PathVariable("nodeName") nodeName: String
  ): WebResponse =
    val workspace = webUtils.workspaceAtCommitOrThrow404(branchId, commitId, AccessRestriction.none, cache = false)
    val node      = webUtils.nodeOrThrow404(workspace, nodeName)
    createFileServeResponse(node)
  end serveByBranchCommitIdAndNodeName

  /** Serves the attachment itself, not our attachment object.
    *
    * @param nodeName
    *   the name of the asset whose attachment is served
    * @param branchId
    *   the name of the branch
    * @return
    *   a web response containing the attachment bytes
    */
  @RequestMapping(path = "authoring/{branch}/nodes/{name}/serve", method = Method.GET)
  def serve(
    @PathVariable("branch") branchId: Long,
    @PathVariable("name") nodeName: String,
    @QueryParam("provider") provider: Option[String] = None,
    @QueryParam("name") attachmentName: Option[String] = None,
    @QueryParam("filename") filename: Option[String] = None,
    @QueryParam("contentType") contentType: Option[String] = None,
    @QueryParam(value = "size", decodeAs = classOf[Long]) size: Option[Long] = None,
    @QueryParam(value = "download", decodeAs = classOf[Boolean]) download: Option[Boolean] = None,
  ): WebResponse =
    val workspace   = webUtils.workspaceOrThrow404(branchId, cache = false)
    lazy val node   = webUtils.nodeOrThrow404(workspace, nodeName)
    val disposition = if download.isTrue then Disposition.attachment else Disposition.inline

    // I trust all these values because this is access controlled to an accredited author
    val blobRef = for
      provider       <- provider
      attachmentName <- attachmentName
      filename       <- filename
      contentType    <- contentType
      size           <- size
      ref             = BlobRef(provider, attachmentName, filename, MediaType.valueOf(contentType), size)
    yield blobService.ref2Info(ref, disposition)

    blobRef.cata(FileResponse(_), createFileServeResponse(node))
  end serve

  @RequestMapping(path = "authoring/defaultProvider", method = Method.GET)
  def getDefaultProvider: BlobProviderResponse =
    val provider = blobService.getDefaultProvider
    BlobProviderResponse(provider.name, provider.container)

  private def createFileServeResponse(node: Asset[?]): WebResponse =
    blobService.getBlobInfo(node) match
      case Some(blob: BlobInfo) => FileResponse(blob)
      case None                 => NoContentResponse
end AttachmentWebController

/** @param blobName
  *   the jclouds blob name (aka s3 object key)
  * @param contentLength
  *   size in bytes
  */
case class AttachmentUploadInfo(
  blobName: String,
  mediaType: MediaType,
  contentLength: Long
)

final case class CmsUploadInfo(
  fileName: String,
  mediaType: MediaType,
  contentLength: Long
)

final case class CmsPutLocation(
  location: Option[URI],
  url: URI,
)

/** @param name
  *   our provider name
  * @param container
  *   s3 bucket name
  */
case class BlobProviderResponse(
  name: String,
  container: String
)

case class ObjectKey(
  objectKey: String
)
