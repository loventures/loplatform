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

package loi.authoring.render
package web

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.util.RawHtml
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.ServiceException
import com.learningobjects.cpxp.service.exception.HttpApiException.*
import com.learningobjects.de.authorization.Secured
import loi.authoring.attachment.service.exception.AssetHasNoAttachment
import loi.authoring.blob.exception.NoSuchBlobRef
import loi.authoring.commit.exception.NoSuchCommitException
import loi.authoring.project.AccessRestriction
import loi.authoring.render.RenderFailures.{AssetCannotBePrinted, UnrenderableAssetType}
import loi.authoring.web.AuthoringWebUtils
import loi.authoring.workspace.exception.NoSuchNodeInWorkspaceException
import scaloi.syntax.option.*
import scaloi.syntax.ʈry.*

import java.util.UUID
import scala.util.Try

@Component
@Controller(value = "authoring/render", root = true)
class RenderWebController(val componentInstance: ComponentInstance)(
  renderService: RenderService,
  webUtils: AuthoringWebUtils,
) extends ApiRootComponent
    with ComponentImplementation:

  /** Renders the asset named `name` in `commit`.
    *
    * "Rendering" is a slightly overloaded concept which is defined differently and also only for certain asset types. I
    * would list the various meanings for various asset types here, but it will doubtlessly wind up getting out of date
    * at some point in the future, so I won't. Instead, if you wish to know, take a look at
    * [[BaseRenderService.getRenderedAsset]].
    *
    * @return
    *   the asset, rendered to HTML.
    */
  @Secured(allowAnonymous = true)
  @RequestMapping(path = "authoring/{branch}/{commit}/asset/{name}/rendered", method = Method.GET)
  def render(
    @PathVariable("branch") branchId: Long,
    @PathVariable("commit") commitId: Long,
    @PathVariable("name") name: UUID,
    @QueryParam(value = "cache", required = false, decodeAs = classOf[Boolean]) cache: Option[Boolean],
    @QueryParam(value = "cdn", required = false, decodeAs = classOf[Boolean]) cdn: Option[Boolean],
  ): Try[WebResponse] =
    val ws      = webUtils.workspaceAtCommitOrThrow404(branchId, commitId, AccessRestriction.none)
    val node    = webUtils.nodeOrThrow404(ws, name)
    val attempt =
      for html <-
          renderService.getRenderedAsset(node, ws, bypassCache = cache.isFalse, cdn.isTrue)
      yield HtmlResponse(html)

    attempt
      .recover {
        /* A new HTML node will have no attachment and should return No Content. */
        case _: AssetHasNoAttachment => NoContentResponse
        case _: NoSuchBlobRef        => NoContentResponse
      }
      .mapExceptions {
        // obviously there may be more but these are the "expected" ones
        case ex: UnrenderableAssetType => badRequest(ex)
      }
  end render

  @RequestMapping(path = "authoring/{branch}/{commit}/asset/{name}/lti/{token}", method = Method.GET)
  def renderInlineLtiLaunch(
    @PathVariable("branch") branchId: Long,
    @PathVariable("commit") commitId: Long,
    @PathVariable("name") name: UUID,
    @PathVariable("token") token: String,
  ) = ErrorResponse.badRequest(new ServiceException("noInlineLti"))

  /** Renders the print friendly asset named `name` in `commit`.
    *
    * @return
    *   the asset, rendered to HTML.
    */
  @RequestMapping(path = "authoring/{branch}/{commit}/asset/{name}/renderedPrintFriendly", method = Method.GET)
  def renderPrintFriendly(
    @PathVariable("branch") branchId: Long,
    @PathVariable("commit") commitId: Long,
    @PathVariable("name") name: UUID,
  ): Try[HtmlResponse[RawHtml]] =
    val ws      = webUtils.workspaceAtCommitOrThrow404(branchId, commitId, AccessRestriction.none)
    val node    = webUtils.nodeOrThrow404(ws, name)
    val attempt =
      for html <- renderService.getPrintFriendlyRenderedAsset(node, ws)
      yield HtmlResponse(html)

    // obviously there may be more but these are the "expected" ones
    attempt.mapExceptions {
      case ex: NoSuchCommitException          => notFound(ex)
      case ex: NoSuchNodeInWorkspaceException => notFound(ex)
      case ex: UnrenderableAssetType          => badRequest(ex)
      case ex: AssetCannotBePrinted           => badRequest(ex)
    }
  end renderPrintFriendly
end RenderWebController
