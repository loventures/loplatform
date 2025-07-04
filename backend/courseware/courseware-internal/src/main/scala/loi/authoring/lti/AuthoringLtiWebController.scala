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

package loi.authoring.lti

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.util.RawHtml
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.service.CurrentUrlService
import com.learningobjects.cpxp.service.exception.ResourceNotFoundException
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.de.authorization.Secured
import com.learningobjects.de.web.MediaType
import loi.asset.html.model.Html
import loi.asset.lti.Lti
import loi.asset.util.Assex.*
import loi.authoring.asset.Asset
import loi.authoring.blob.{BlobRef, BlobService}
import loi.authoring.edge.EdgeService
import loi.authoring.render.RenderService
import loi.authoring.security.right.AccessAuthoringAppRight
import loi.authoring.web.AuthoringApiWebUtils
import loi.authoring.workspace.AttachedReadWorkspace
import loi.cp.course.preview.PreviewRole
import loi.cp.lti.{LTIConstants, LtiRoles, LtiWebUtils}
import loi.cp.ltitool.LtiToolLaunchService.{LtiContext, ResourceLink}
import loi.cp.ltitool.{LtiLaunchConfiguration, LtiToolComponent, LtiToolLaunchService, LtiToolService}
import org.apache.commons.io.{FileUtils, IOUtils}
import org.apache.http.client.utils.URLEncodedUtils
import scalaz.syntax.traverse.*
import scaloi.syntax.collection.*
import scaloi.syntax.option.*

import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.UUID
import scala.jdk.CollectionConverters.*
import scala.util.Try

@Component
@Controller(root = true)
@Secured(Array(classOf[AccessAuthoringAppRight]))
private[lti] class AuthoringLtiWebController(
  val componentInstance: ComponentInstance,
  webUtils: AuthoringApiWebUtils,
  ltiUtils: LtiWebUtils,
  currentUrlService: CurrentUrlService,
  renderService: RenderService,
  ltiToolService: LtiToolService,
  launchService: LtiToolLaunchService,
  blobService: BlobService,
  user: UserDTO,
)(implicit
  cs: ComponentService,
  edgeService: EdgeService,
) extends ApiRootComponent
    with ComponentImplementation:

  @RequestMapping(path = "authoring/{branch}/asset/{name}/lti/launch", method = Method.GET)
  def ltiLaunch(
    @PathVariable("branch") branchId: Long,
    @PathVariable("name") name: UUID,
    @QueryParam("role") role: Option[PreviewRole],
  ): Try[HtmlResponse[RawHtml]] =
    val ws  = webUtils.workspaceOrThrow404(branchId)
    val lti = webUtils.nodeOrThrow404Typed[Lti](ws, name.toString)

    for
      (tool, config) <- ltiUtils.loadLtiTool(lti) <@~* new IllegalStateException(s"No tool ${lti.data.lti.toolId}")
      context         = ltiUtils.ltiContextForAuthoring(ws)
      resLink         = ltiResourceLink(ws, lti)
      launch         <- ltiLaunchImpl(context, tool, resLink, config, role)
    yield launch
  end ltiLaunch

  // When viewing an original asset in authoring
  @RequestMapping(path = "assets/{branch}/html.1/{name}/lti/{token}", method = Method.GET, csrf = false)
  def inlineLtiAssetLaunch(
    @PathVariable("branch") branchId: Long,
    @PathVariable("name") name: String,
    @PathVariable("token") token: String,
  ): Try[HtmlResponse[RawHtml]] =
    val ws   = webUtils.workspaceOrThrow404(branchId)
    val html = webUtils.nodeOrThrow404Typed[Html](ws, name)

    for
      ltiUrlOpt <- renderService.findLtiUrlByRenderedDigest(
                     html,
                     instructions = false,
                     ws,
                     token,
                     bypassCache = false,
                     useCdn = false,
                   )
      ltiUrl    <- ltiUrlOpt <@~* new ResourceNotFoundException(s"Unknown token $token")
      context    = ltiUtils.ltiContextForAuthoring(ws)
      resLink    = ltiResourceLink(ws, html)
      launch    <- inlineLtiLaunchImpl(ltiUrl.name, ltiUrl.suffix, ltiUrl.query, context, resLink)
    yield launch
    end for
  end inlineLtiAssetLaunch

  // When viewing an edited asset in authoring, looks to the uploaded HTML
  @RequestMapping(path = "assets/{branch}/html.1/upload/{upload}/lti/{token}", method = Method.GET, csrf = false)
  def inlineLtiUploadLaunch(
    @PathVariable("branch") branchId: Long,
    @PathVariable("upload") upload: UploadInfo,
    @PathVariable("token") token: String,
  ): Try[HtmlResponse[RawHtml]] =
    val ws = webUtils.workspaceOrThrow404(branchId)

    val html      = FileUtils.readFileToString(upload.getFile, StandardCharsets.UTF_8)
    val ltiUrlOpt = renderService.findLtiUrlByRenderedDigest(html, token)
    for
      ltiUrl <- ltiUrlOpt <@~* new ResourceNotFoundException(s"Unknown token $token")
      context = ltiUtils.ltiContextForAuthoring(ws)
      launch <- inlineLtiLaunchImpl(ltiUrl.name, ltiUrl.suffix, ltiUrl.query, context, ResourceLink.Fake)
    yield launch
  end inlineLtiUploadLaunch

  // When viewing a historic asset in authoring, looks to the blob ref
  @RequestMapping(path = "assets/{branch}/html.1/ref/{provider}/{name}/lti/{token}", method = Method.GET, csrf = false)
  def inlineLtiBlobRefLaunch(
    @PathVariable("branch") branchId: Long,
    @PathVariable("provider") provider: String,
    @PathVariable("name") name: String,
    @PathVariable("token") token: String,
  ): Try[HtmlResponse[RawHtml]] =
    val ws = webUtils.workspaceOrThrow404(branchId)

    val is        = blobService.ref2Stream(BlobRef(provider, decode(name), "Unknown.html", MediaType.TEXT_HTML_UTF_8, 0L))
    val html      = IOUtils.toString(is, StandardCharsets.UTF_8)
    val ltiUrlOpt = renderService.findLtiUrlByRenderedDigest(html, token)
    for
      ltiUrl <- ltiUrlOpt <@~* new ResourceNotFoundException(s"Unknown token $token")
      context = ltiUtils.ltiContextForAuthoring(ws)
      launch <- inlineLtiLaunchImpl(ltiUrl.name, ltiUrl.suffix, ltiUrl.query, context, ResourceLink.Fake)
    yield launch
  end inlineLtiBlobRefLaunch

  private def ltiResourceLink(ws: AttachedReadWorkspace, asset: Asset[?]) =
    ResourceLink(asset.info.name.toString, asset.title.getOrElse("Untitled"))

  private def decode(name: String): String = name.replace('_', '/')

  def inlineLtiLaunchImpl(
    name: String,
    suffix: String,
    query: String,
    context: LtiContext,
    resLink: ResourceLink,
  ): Try[HtmlResponse[RawHtml]] =
    for
      tool        <- findLtiTool(name) <@~* new IllegalStateException(s"Unknown tool: $name")
      cf           = tool.getLtiConfiguration
      toolUrl     <- cf.defaultConfiguration.url <@~* new IllegalStateException(s"No tool URL $name")
      baseUrl      = s"$toolUrl$suffix"
      parameters   = URLEncodedUtils.parse(new URI(query), StandardCharsets.UTF_8).asScala
      parameterMap = parameters.foldToMap(k => k.getName -> k.getValue)
      base         = LtiLaunchConfiguration(url = Some(baseUrl), customParameters = parameterMap)
      config       = base.applyDefaultLtiConfig(cf)
      launch      <- ltiLaunchImpl(context, tool, resLink, config, Some(PreviewRole.Learner))
    yield launch

  private def findLtiTool(name: String): Option[LtiToolComponent] =
    ltiToolService.queryLtiToolByName(name).getComponent[LtiToolComponent]

  private def ltiLaunchImpl(
    context: LtiContext,
    tool: LtiToolComponent,
    resLink: ResourceLink,
    config: LtiLaunchConfiguration,
    role: Option[PreviewRole],
  ) =

    val launchInfo  = ltiUtils.defaultLaunchInfo()
    val outcomeInfo = ltiUtils.getOutcomeInfo(tool, config, None)
    val isLti1p3    = config.ltiVersion.contains(LTIConstants.LTI_1P3)

    for
      // Get params, but use the authoring specific role
      params <- (if isLti1p3 then
                   launchService.get1p3LoginParameters(
                     context.id,
                     role,
                     tool,
                     launchInfo,
                     resLink.identifier,
                     config
                   )
                 else
                   // One has to wonder how real tools will react to the same user
                   // switching roles. We send the transient role flag, but do they
                   // understand it...
                   val roles = ltiRoles(role.map(_.roleId).getOrElse("instructor"))
                   launchService.getLaunchParameters(
                     context,
                     user,
                     tool,
                     launchInfo,
                     Some(resLink),
                     outcomeInfo,
                     config,
                     roles
                   )
                )
      // Get url
      url    <- (if isLti1p3 then config.loginUrl <@~* new IllegalStateException(s"No login URL")
                 else config.url <@~* new IllegalStateException(s"No tool URL"))
    yield ServletDispatcher.autopost(url, params.toMap)
    end for
  end ltiLaunchImpl

  private def ltiRoles(loRole: String) =
    LtiRoles.ContextMappings(loRole).toList ++ LtiRoles.GlobalMappings(loRole).toList
end AuthoringLtiWebController
