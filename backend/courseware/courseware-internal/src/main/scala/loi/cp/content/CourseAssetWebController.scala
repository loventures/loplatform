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

package loi.cp.content

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.exception.HttpApiException.{badRequest, notFound}
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.de.authorization.securedByImplementation
import loi.asset.blob.SourceProperty
import loi.asset.contentpart.ContentPart
import loi.asset.external.CourseLink
import loi.asset.file.image.model.Image
import loi.asset.lti.Lti
import loi.asset.resource.model.{Resource1, ResourceType}
import loi.authoring.asset.Asset
import loi.authoring.attachment.service.exception.AssetHasNoAttachment
import loi.authoring.blob.BlobService
import loi.authoring.blob.exception.NoSuchBlobRef
import loi.authoring.commit.exception.NoSuchCommitException
import loi.authoring.edge.{AssetEdge, EdgeService, Group}
import loi.authoring.node.AssetNodeService
import loi.authoring.render.RenderFailures.UnrenderableAssetType
import loi.authoring.render.{LtiLinkRenderer, RenderService}
import loi.authoring.workspace.exception.NoSuchNodeInWorkspaceException
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.reference.EdgePath
import scaloi.syntax.option.*
import scaloi.syntax.ʈry.*

import scala.util.Try

@securedByImplementation
@Controller(root = true)
trait CourseAssetWebController extends ApiRootComponent:

  @RequestMapping(path = "lwc/{context}/banner", method = Method.GET)
  def banner(
    @PathVariable("context") context: Long,
  ): Try[WebResponse]

  @RequestMapping(path = "lwc/{context}/asset/{path}/render", method = Method.GET)
  def render(
    @PathVariable("context") context: Long,
    @PathVariable("path") path: EdgePath,
    @QueryParam(value = "cache", required = false, decodeAs = classOf[Boolean]) cache: Option[Boolean],
    @QueryParam(value = "cdn", required = false, decodeAs = classOf[Boolean]) cdn: Option[Boolean],
  ): Try[WebResponse]

  @RequestMapping(path = "lwc/{context}/asset/{path}/info", method = Method.GET)
  def info(
    @PathVariable("context") context: Long,
    @PathVariable("path") path: EdgePath,
  ): Try[AssetInfo]
end CourseAssetWebController

@Component
class CourseAssetWebControllerImpl(val componentInstance: ComponentInstance)(implicit
  contentAccessService: ContentAccessService,
  renderService: RenderService,
  nodeService: AssetNodeService,
  edgeService: EdgeService,
  blobService: BlobService,
  user: UserDTO,
) extends CourseAssetWebController
    with ComponentImplementation:

  override def banner(
    context: Long,
  ): Try[WebResponse] =
    // Should only be called on a course with a banner
    // This should be delivered via CDN but too hard...
    for
      course     <- contentAccessService.getCourseAsLearner(context, user)
      ws          = course.getWorkspace
      bannerName <- ws.outEdgeAttrs(ws.homeName, Group.Image).headOption.map(_.tgtName) <@~* badRequest()
      banner     <- nodeService.loadA[Image](ws).byName(bannerName)
      blob       <- blobService.getBlobInfo(banner) <@~* badRequest()
    yield FileResponse(blob)
  end banner

  override def render(
    context: Long,
    path: EdgePath,
    cache: Option[Boolean],
    cdn: Option[Boolean],
  ): Try[WebResponse] =
    val attempt = for
      (course, content) <- contentAccessService.useContent(context, path, user)
      html              <- renderService.getRenderedAsset(
                             content.asset,
                             course.getWorkspace,
                             cache.isFalse,
                             cdn.isTrue,
                           )
    yield HtmlResponse(html)

    attempt
      .recover {
        /* A new HTML node will have no attachment and should return No Content. */
        case _: AssetHasNoAttachment => NoContentResponse
        case _: NoSuchBlobRef        => NoContentResponse
      }
      .mapExceptions {
        // obviously there may be more but these are the "expected" ones
        case ex: NoSuchCommitException          => notFound(ex)
        case ex: NoSuchNodeInWorkspaceException => notFound(ex)
        case ex: UnrenderableAssetType          => badRequest(ex)
      }
  end render

  override def info(
    context: Long,
    path: EdgePath,
  ): Try[AssetInfo] =
    for
      (course, content) <- contentAccessService.useContent(context, path, user)
      ws                 = course.getWorkspace
      rendered           = renderService.render(ws, content.asset)
      info              <- PartialFunction.condOpt(rendered) {
                             case Lti.Asset(lti)             =>
                               LtiInfo(course, content, lti)
                             case CourseLink.Asset(crs)      =>
                               CourseLinkInfo(course, content, crs)
                             case Resource1.Asset(resource1) =>
                               val inSystemResource = edgeService.loadOutEdges(ws, resource1, Group.InSystemResource).headOption
                               LegacyResourceInfo(course, content, resource1, inSystemResource)
                           } <@~* badRequest()
    yield info

end CourseAssetWebControllerImpl

private[content] sealed trait AssetInfo

/** LtiInfo for delivery to the front end. */
private[content] final case class LtiInfo(
  instructions: ContentPart,
) extends AssetInfo

private object LtiInfo:
  def apply(course: LightweightCourse, content: CourseContent, lti: Asset[Lti]): LtiInfo =
    LtiInfo(LtiLinkRenderer.rewriteContentPart(lti.data.instructions, courseAssetInstructionsUrl(course, content)))

private[content] final case class CourseLinkInfo(
  instructions: ContentPart,
  newWindow: Boolean,
  branch: Option[Long],
) extends AssetInfo

private object CourseLinkInfo:
  def apply(course: LightweightCourse, content: CourseContent, crs: Asset[CourseLink]): CourseLinkInfo =
    CourseLinkInfo(
      LtiLinkRenderer.rewriteContentPart(crs.data.instructions, courseAssetInstructionsUrl(course, content)),
      crs.data.newWindow,
      crs.data.branch,
    )

/** Resource1Info for delivery to the front end. cf. ResourceContentItemApi */
private[content] final case class LegacyResourceInfo(
  resourceType: ResourceType,
  embedCode: Option[String],
  instructions: ContentPart,
  resourceFileName: Option[String],
  resourceUrl: Option[String],
) extends AssetInfo

private object LegacyResourceInfo:
  def apply(
    course: LightweightCourse,
    content: CourseContent,
    resource1: Asset[Resource1],
    inSystemResource: Option[AssetEdge.Any]
  ): LegacyResourceInfo =

    LegacyResourceInfo(
      resourceType = resource1.data.resourceType,
      embedCode = resource1.data.embedCode,
      instructions =
        LtiLinkRenderer.rewriteContentPart(resource1.data.instructions, courseAssetInstructionsUrl(course, content)),
      resourceFileName = getResource1FileName(inSystemResource),
      resourceUrl = getResource1Url(inSystemResource)
    )

  private def getResource1FileName(
    inSystemResource: Option[AssetEdge.Any]
  ): Option[String] = for
    edge <- inSystemResource
    src  <- SourceProperty.fromNode(edge.target)
  yield src.filename

  private def getResource1Url(inSystemResource: Option[AssetEdge.Any]): Option[String] =
    inSystemResource
      .map(usage => s"/api/v2/authoring/nodes/${usage.target.info.id}/serve")
end LegacyResourceInfo
