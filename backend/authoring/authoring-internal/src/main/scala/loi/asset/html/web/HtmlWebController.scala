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

package loi.asset.html.web

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.util.ComponentUtils.resourceUrl
import com.learningobjects.cpxp.component.util.RawHtml
import com.learningobjects.cpxp.component.web.{ApiRootComponent, HtmlResponse, Method}
import com.learningobjects.cpxp.component.{BaseComponent, ComponentInstance}
import com.learningobjects.cpxp.controller.upload.Uploads
import com.learningobjects.cpxp.service.exception.HttpApiException.*
import com.learningobjects.de.authorization.Secured
import com.learningobjects.de.web.{MediaType, UncheckedMessageException}
import loi.asset.html.model.Html
import loi.asset.html.service.HtmlService
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.asset.{Asset, AssetInfo}
import loi.authoring.blob.BlobRef
import loi.authoring.blob.exception.NoSuchBlobRef
import loi.authoring.render.LtiLinkRenderer
import loi.authoring.security.right.AccessAuthoringAppRight
import loi.authoring.web.AuthoringWebUtils
import loi.authoring.workspace.AttachedReadWorkspace
import scalaz.syntax.either.*
import scalaz.{-\/, \/, \/-}
import scaloi.syntax.ʈry.*

import java.time.Instant
import java.util.{Date, UUID}
import scala.util.Try

/** Controller that can serve a templated HTML asset
  */
@Component
@Controller(root = true, value = "assets/html")
class HtmlWebController(
  ci: ComponentInstance,
  htmlService: HtmlService,
  authoringWebUtils: AuthoringWebUtils,
) extends BaseComponent(ci)
    with ApiRootComponent:

  /** Serves the html asset
    *
    * @param name
    *   name of the html asset
    * @return
    *   [[HtmlResponse]]
    */
  @RequestMapping(path = "assets/{branch}/html.1/{name}/serve", method = Method.GET, csrf = false)
  @Secured(Array(classOf[AccessAuthoringAppRight]))
  def serve(
    @PathVariable("branch") branchId: Long,
    @PathVariable("name") name: String,
    @QueryParam("provider") providerOpt: Option[String],
    @QueryParam("name") attachmentNameOpt: Option[String],
  ): Try[HtmlResponse[RawHtml]] =
    serve(branchId, None, name, providerOpt, attachmentNameOpt)

  @RequestMapping(path = "assets/{branch}/commits/{commit}/html.1/{name}/serve", method = Method.GET, csrf = false)
  @Secured(Array(classOf[AccessAuthoringAppRight]))
  def serve(
    @PathVariable("branch") branchId: Long,
    @PathVariable("commit") commitId: Long,
    @PathVariable("name") name: String,
    @QueryParam("provider") providerOpt: Option[String],
    @QueryParam("name") attachmentNameOpt: Option[String],
  ): Try[HtmlResponse[RawHtml]] =
    serve(branchId, Some(commitId), name, providerOpt, attachmentNameOpt)

  private def serve(
    branchId: Long,
    commitId: Option[Long],
    name: String,
    providerOpt: Option[String],
    attachmentNameOpt: Option[String],
  ): Try[HtmlResponse[RawHtml]] =
    // providerOpt and attachmentNameOpt are necessary for previewing a restored version of
    // an HTML asset (so head commit, old asset data) but the narrative editor will always
    // send these parameters, even viewing the vanilla head
    val workspace = commitId match
      case Some(commitId) => authoringWebUtils.workspaceAtCommitOrThrow404(branchId, commitId)
      case None           => authoringWebUtils.workspaceOrThrow404(branchId)
    val html      = authoringWebUtils.nodeOrThrow404Typed[Html](workspace, name)
    val blobRef   = for
      provider       <- providerOpt
      attachmentName <- attachmentNameOpt
      // Only serve a specific blob ref if it doesn't match the current state of the asset
      if !html.data.source.exists(ref => ref.provider == provider && ref.name == attachmentName)
    yield BlobRef(provider, attachmentName, html.data.title, MediaType.TEXT_HTML, -1L)
    serve(html, workspace, blobRef.map(_.right[String]))
  end serve

  /** Serves the html asset, but using uploaded content as the replacement body of the asset. This applies all of the
    * Web resources associated with the course and asset to the uploaded content.
    *
    * @param name
    *   name of the html asset
    * @param upload
    *   uploaded edited HTML
    * @return
    *   [[HtmlResponse]]
    */
  @RequestMapping(path = "assets/{branch}/html.1/{name}/serve/{upload}", method = Method.GET, csrf = false)
  @Secured(Array(classOf[AccessAuthoringAppRight]))
  def serveEdited(
    @PathVariable("branch") branchId: Long,
    @PathVariable("name") name: String,
    @PathVariable("upload") upload: String,
  ): Try[HtmlResponse[RawHtml]] =
    val workspace = authoringWebUtils.workspaceOrThrow404(branchId, cache = false)
    val html      = authoringWebUtils.nodeOrThrow404Typed[Html](workspace, name)
    serve(html, workspace, Some(upload.left[BlobRef]))
  end serveEdited

  /** Serves the staged content of an unsaved asset from the front end. This applies all of the course default Web
    * resources to the content so it renders as it ought.
    */
  @RequestMapping(path = "assets/{branch}/html.1/serve/{upload}", method = Method.GET, csrf = false)
  @Secured(Array(classOf[AccessAuthoringAppRight]))
  def serveEdited(
    @PathVariable("branch") branchId: Long,
    @PathVariable("upload") upload: String,
  ): Try[HtmlResponse[RawHtml]] =
    val workspace = authoringWebUtils.workspaceOrThrow404(branchId, cache = false)
    serve(HtmlWebController.FakeHtml, workspace, Some(upload.left[BlobRef]))

  /** Currently only used in content designer blocks.
    */
  @RequestMapping(path = "assets/{branch}/html.1/id/{id}/serve", method = Method.GET, csrf = false)
  def serveById(
    @PathVariable("branch") branchId: Long,
    @PathVariable("id") nodeId: Long
  ): Try[HtmlResponse[RawHtml]] =
    val workspace = authoringWebUtils.workspaceOrThrow404(branchId, cache = false)
    val html      = authoringWebUtils.nodeOrThrow404Typed[Html](workspace, nodeId)
    serve(html, workspace, None)

  /** @param either the upload guid or a historic blob ref or none */
  private def serve(
    html: Asset[Html],
    ws: AttachedReadWorkspace,
    edit: Option[String \/ BlobRef]
  ): Try[HtmlResponse[RawHtml]] =
    val ltiPrefix = edit match
      case None            => s"/api/v2/assets/${ws.bronchId}/html.1/${html.info.name}/"
      case Some(-\/(guid)) => s"/api/v2/assets/${ws.bronchId}/html.1/upload/$guid/"
      case Some(\/-(ref))  => s"/api/v2/assets/${ws.bronchId}/html.1/ref/${ref.provider}/${encode(ref.name)}/"

    val attempt = for
      dto         <- htmlService.createHtml(html, ws, edit.map(_.leftMap(guid => Uploads.retrieveUpload(guid).toLocalFileInfo)))
      rewritten    = LtiLinkRenderer.rewriteLtiUrls(dto.html, ltiPrefix)
      interpolated = interpolateCdn(rewritten)
    yield HtmlResponse(interpolated)

    attempt mapExceptions {
      case ex: NoSuchBlobRef             => notFound(ex)
      case ex: UncheckedMessageException => unprocessableEntity(ex)
    }
  end serve

  private def interpolateCdn(html: String): String =
    val component  = ci.getEnvironment.getComponent(loi.authoring.authoringComponentIdentifier)
    val baseCdnUrl = resourceUrl("", component)
    html.replace("$$cdn/", baseCdnUrl)

  // names are paths with / so have to be radically encoded.
  private def encode(name: String): String = name.replace('/', '_')
end HtmlWebController

object HtmlWebController:

  /** Sometimes a fake HTML asset is needed to pacify a new, poorly designed API. */
  final val FakeHtml: Asset[Html] = loi.authoring.asset.Asset[Html](
    AssetInfo(
      id = 0L,
      name = new UUID(0L, 0L),
      typeId = AssetTypeId.Html,
      created = Date `from` Instant.EPOCH,
      createdBy = None,
      modified = Date `from` Instant.EPOCH,
      archived = false
    ),
    Html("Untitled")
  )
end HtmlWebController
