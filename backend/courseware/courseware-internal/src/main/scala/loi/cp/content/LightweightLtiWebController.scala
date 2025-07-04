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

import argonaut.Argonaut.*
import argonaut.Json
import com.google.common.annotations.VisibleForTesting
import com.learningobjects.cpxp.WebContext
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.util.{HtmlTemplate, RawHtml}
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.exception.ResourceNotFoundException
import com.learningobjects.cpxp.service.session.SessionDTO
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.de.authorization.{Secured, securedByImplementation}
import de.tomcat.juli.LogMeta
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import loi.asset.lti.Lti
import loi.authoring.asset.Asset
import loi.authoring.render.{LtiUrl, RenderService}
import loi.cp.analytics.AnalyticsService
import loi.cp.analytics.entity.ContentId
import loi.cp.analytics.event.LtiLaunchEvent
import loi.cp.course.preview.{PreviewRole, PreviewService}
import loi.cp.lti.outcomes.LtiOutcomesParser.GradeSourcedId
import loi.cp.lti.{LTIConstants, LtiRoles, LtiWebUtils}
import loi.cp.ltitool.LtiToolLaunchService.*
import loi.cp.ltitool.*
import loi.cp.reference.EdgePath
import org.apache.http.client.utils.URLEncodedUtils
import scalaz.std.option.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scaloi.misc.TimeSource
import scaloi.misc.TryInstances.*
import scaloi.syntax.collection.*
import scaloi.syntax.option.*

import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.UUID
import scala.jdk.CollectionConverters.*
import scala.util.Try

@securedByImplementation
@Controller(root = true)
trait LightweightLtiWebController extends ApiRootComponent:
  import LightweightLtiWebController.*

  @RequestMapping(path = "lwc/{context}/lti/launch/{path}", method = Method.GET)
  def ltiLaunch(
    @PathVariable("context") context: Long,
    @PathVariable("path") path: EdgePath,
    @QueryParam("role") role: Option[PreviewRole],
  ): Try[LtiLaunch]

  @RequestMapping(path = "lwc/lti/token", method = Method.GET)
  def ltiToken(): LtiToken

  @RequestMapping(path = "lwc/{context}/lti/{path}/style", method = Method.GET)
  def ltiLaunchStyle(@PathVariable("context") context: Long, @PathVariable("path") path: EdgePath): Try[LtiLaunchStyle]

  @RequestMapping(path = "lwc/{context}/lti/redirect/{path}", method = Method.GET)
  def ltiRedirect(
    @PathVariable("context") context: Long,
    @PathVariable("path") path: EdgePath,
    @QueryParam("role") role: Option[PreviewRole],
  ): Try[HtmlResponse[RawHtml]]

  @RequestMapping(path = "lwc/{context}/asset/{path}/lti/{token}", method = Method.GET)
  def inlineLtiRedirect(
    @PathVariable("context") context: Long,
    @PathVariable("path") path: EdgePath,
    @PathVariable("token") token: String,
    webRequest: WebRequest,
  ): Try[HtmlResponse[RawHtml]]

  @RequestMapping(path = "lwc/{context}/asset/{path}/instructions/lti/{token}", method = Method.GET)
  def instructionsLtiRedirect(
    @PathVariable("context") context: Long,
    @PathVariable("path") path: EdgePath,
    @PathVariable("token") token: String,
    webRequest: WebRequest,
  ): Try[HtmlResponse[RawHtml]]

  @VisibleForTesting
  @RequestMapping(path = "lwc/{context}/lti/look/{path}", method = Method.GET)
  def ltiLook(@PathVariable("context") context: Long, @PathVariable("path") path: EdgePath): Try[Unit]

  @RequestMapping(path = "lwc/{context}/lti/return/{path}", method = Method.GET)
  @Secured(allowAnonymous = true)
  def ltiReturn(
    @PathVariable("context") context: Long,
    @PathVariable("path") path: EdgePath,
    req: HttpServletRequest
  ): WebResponse
end LightweightLtiWebController

object LightweightLtiWebController:

  /** LTI Return Parameters. */
  final val LtiReturnLogMessage   = "lti_log"
  final val LtiReturnLogError     = "lti_error"
  final val LtiReturnMessage      = "lti_msg"      // user visible
  final val LtiReturnErrorMessage = "lti_errormsg" // user visible

  final case class LtiLaunch(
    url: String,
    parameters: List[Parameter],
  )

  final case class Parameter(name: String, value: String)

  object Parameter:
    def apply(keyValue: (String, String)): Parameter = Parameter(keyValue._1, keyValue._2)
end LightweightLtiWebController

@Component
class LightweightLtiWebControllerImpl(val componentInstance: ComponentInstance)(implicit
  analyticsService: AnalyticsService,
  contentAccessService: ContentAccessService,
  renderService: RenderService,
  ltiToolService: LtiToolService,
  launchService: LtiToolLaunchService,
  previewService: PreviewService,
  ltiUtils: LtiWebUtils,
  domain: DomainDTO,
  session: SessionDTO,
  ts: TimeSource,
  actualUser: UserDTO,
  wc: WebContext,
  cs: ComponentService
) extends LightweightLtiWebController
    with ComponentImplementation:
  import LightweightLtiWebController.*
  import LightweightLtiWebControllerImpl.*

  /** In an LTI 1.0/1.1 launch, a single "launch" is performed, and this is it. This gathers all the tool and user data
    * in a big pile and sends it.
    *
    * In an LTI 1.3 launch, there are two "launches".
    *   - 'login' - hit the Initiate Login URL. Tell the Tool our platform ID/URL, and declare the Tool URL we need
    *   - 'auth/launch' - Tool requests that we launch forealz, and we pass an encrypted JWT with user data to the Tool
    *
    * The first LTI 1.3 step is covered by `ltiLaunch` here. The second step is in `LtiAdvantageServlet.lti1p3Launch`
    */
  override def ltiLaunch(contextId: Long, path: EdgePath, previewRole: Option[PreviewRole]): Try[LtiLaunch] =
    for
      // Load the context and LTI tool
      (context, lti) <- ltiUtils.loadContentForCourse(contextId, path, actualUser)
      (tool, config) <- ltiUtils.loadLtiTool(lti) <@~* new IllegalStateException(s"No tool for path $path")
      previewUser    <- previewRole.traverse(getPreviewUser(contextId, _))
      // So if you're an admin, not enrolled, then you send no LTI role, which is gross. We
      // ought to automatically preview as Instructor when this is the caes.
      previewLtiRoles = previewRole.toList.flatMap(r => LtiRoles.ContextMappings(r.roleId).list.toList)

      // Assemble launch infos
      launchInfo   = ltiUtils.getLaunchInfo(contextId, path)
      resourceLink = Some(ResourceLink(path, lti.data.title))
      outcomeInfo  = ltiUtils.getOutcomeInfo(tool, config, Some(GradeSourcedId(actualUser.id, context.id, path).encode))

      // Put all the infos into the correct launch service
      isLti1p3 = config.ltiVersion.contains(LTIConstants.LTI_1P3)
      params  <- (if isLti1p3 then
                    launchService.get1p3LoginParameters(context.id, previewRole, tool, launchInfo, path.toString, config)
                  else
                    launchService.getLaunchParameters(
                      context,
                      previewUser | actualUser,
                      tool,
                      launchInfo,
                      resourceLink,
                      outcomeInfo,
                      config,
                      previewLtiRoles
                    )
                 )
      // Choose the wisest URL
      url     <- (if isLti1p3 then config.loginUrl `toTry` new IllegalStateException(s"No login URL")
                  else config.url `toTry` new IllegalStateException(s"No tool URL"))
    yield
      LogMeta.let(params.toMap.asJson) {
        logger.info(s"LTI launch: $url")
      }
      LtiAnalytics.emit(contextId, path, actualUser, lti, tool, config)
      LtiLaunch(url, params.map(Parameter(_)))

  private def getPreviewUser(contextId: Long, role: PreviewRole): Try[UserDTO] =
    for
      course    <- contentAccessService.getCourseAsInstructor(contextId, actualUser)
      previewer <- previewService.findPreviewer(course, role) `toTry` new IllegalStateException("No previewer")
    yield previewer.toDTO

  // TODO: this
  override def ltiToken(): LtiToken =
    LtiToken("todotoken")

  override def ltiLaunchStyle(context: Long, path: EdgePath): Try[LtiLaunchStyle] =
    for
      (_, _, lti) <- contentAccessService.useContentT[Lti](context, path, actualUser)
      (_, config) <- ltiUtils.loadLtiTool(lti) <@~* new IllegalStateException(s"No tool for path $path")
    yield config.launchStyle | LtiLaunchStyle.NEW_WINDOW

  override def ltiRedirect(context: Long, path: EdgePath, role: Option[PreviewRole]): Try[HtmlResponse[RawHtml]] =
    for launch <- ltiLaunch(context, path, role)
    yield ServletDispatcher.autopost(launch.url, launch.parameters.foldToMap(p => p.name -> p.value))

  override def inlineLtiRedirect(
    context: Long,
    path: EdgePath,
    token: String,
    webRequest: WebRequest
  ): Try[HtmlResponse[RawHtml]] =
    inlineLtiRedirect(context, path, token, webRequest, instructions = false)

  override def instructionsLtiRedirect(
    context: Long,
    path: EdgePath,
    token: String,
    webRequest: WebRequest
  ): Try[HtmlResponse[RawHtml]] =
    inlineLtiRedirect(context, path, token, webRequest, instructions = true)

  private def inlineLtiRedirect(
    contextId: Long,
    path: EdgePath,
    token: String,
    webRequest: WebRequest,
    instructions: Boolean
  ): Try[HtmlResponse[RawHtml]] =
    for
      (course, content)           <- contentAccessService.useContent(contextId, path, actualUser)
      context                      = LtiContext.fromCourse(course)
      role                         = previewRole(contextId, webRequest)
      previewUser                 <- role.traverse(getPreviewUser(contextId, _))
      ltiUrl                      <- renderService.findLtiUrlByRenderedDigest(
                                       content.asset,
                                       instructions,
                                       course.getWorkspace,
                                       token,
                                       bypassCache = false,
                                       useCdn = course.getOfferingId.nonEmpty,
                                     )
      LtiUrl(name, suffix, query) <- ltiUrl `toTry` new ResourceNotFoundException(s"Unknown token $token")
      tool                        <- findLtiTool(name) `toTry` new IllegalStateException(s"Unknown tool: $name")
      toolUrl                     <- tool.getLtiConfiguration.defaultConfiguration.url `toTry`
                                       new IllegalStateException(s"No tool URL $name")
      baseUrl                      = s"$toolUrl$suffix"
      parameters                   = URLEncodedUtils.parse(new URI(query), StandardCharsets.UTF_8).asScala
      parameterMap                 = parameters.foldToMap(k => k.getName -> k.getValue)
      base                         = LtiLaunchConfiguration(url = Some(baseUrl), customParameters = parameterMap)
      config                       = base.applyDefaultLtiConfig(tool.getLtiConfiguration)
      roles                        = role.toList.flatMap(r => LtiRoles.ContextMappings(r.roleId).list.toList)
      params                      <- launchService.getLaunchParameters(
                                       context,
                                       previewUser | actualUser,
                                       tool,
                                       ltiUtils.defaultLaunchInfo(),
                                       Some(ResourceLink(path, content.title)),
                                       OutcomeInfo(graded = false, None),
                                       config,
                                       roles
                                     )
      url                         <- config.url `toTry` new IllegalStateException(s"No tool URL")
    yield
      LogMeta.let(params.toMap.asJson) {
        logger.info(s"LTI launch: $url")
      }
      LtiAnalytics.emit(context.id, path, actualUser, content.asset, tool, config)
      ServletDispatcher.autopost(url, params.toMap)

  import com.learningobjects.cpxp.scala.util.HttpServletRequestOps.*

  private def previewRole(context: Long, webRequest: WebRequest): Option[PreviewRole] =
    webRequest.getRawRequest
      .cookie("X-PreviewRole")
      .map(_.split(':'))
      .filter(a => a(0) == context.toString)
      .map(a => PreviewRole.withName(a(1)))

  private def findLtiTool(name: String): Option[LtiToolComponent] =
    ltiToolService.queryLtiToolByName(name).getComponent[LtiToolComponent]

  /** This is a hack for integration testing that tries to access the content in read-only mode. */
  override def ltiLook(context: Long, path: EdgePath): Try[Unit] =
    contentAccessService.readContentT[Lti](context, path, actualUser).void

  override def ltiReturn(context: Long, path: EdgePath, req: HttpServletRequest): WebResponse =
    def encodeValues(as: Array[String]) =
      if as.length == 1 then Json.jString(as(0))
      else Json.jArray(as.map(Json.jString).toList)

    val ltiParams = req.getParameterMap.asScala.view
      .filterKeys(_.startsWith("lti_"))
      .map(e => e._1 -> encodeValues(e._2))
      .toList

    LogMeta.let(ltiParams*) {
      logger.info(s"LTI return received for tool $context:$path")
    }

    HtmlResponse(
      HtmlTemplate(this, "ltiReturn.html")
        .bind(
          "msg"   -> req.getParameter(LtiReturnMessage),
          "error" -> req.getParameter(LtiReturnErrorMessage),
          "mode"  -> ltiLaunchStyle(context, path).fold(_ => "UNKNOWN", _.toString)
        ),
      HttpServletResponse.SC_OK
    )
  end ltiReturn
end LightweightLtiWebControllerImpl

object LightweightLtiWebControllerImpl:
  private final val logger = org.log4s.getLogger

object LtiAnalytics:

  def emit(
    courseId: Long,
    path: EdgePath,
    user: UserDTO,
    lti: Asset[?],
    tool: LtiToolComponent,
    config: LtiLaunchConfiguration
  )(implicit analyticsService: AnalyticsService, session: SessionDTO, domain: DomainDTO, ts: TimeSource): Unit =
    val toolConfig = tool.getLtiConfiguration
    analyticsService `emitEvent` LtiLaunchEvent(
      id = UUID.randomUUID(),
      time = ts.date,
      source = domain.hostName,
      session = session.id,
      user = user,
      toolId = tool.getToolId,
      url = config.url,
      content = Some(ContentId(path.toString, lti.info.name)),
      course = analyticsService.courseId(courseId),
      isGraded = config.isGraded,
      ltiVersion = config.ltiVersion,
      ltiMessageType = config.ltiMessageType,
      customParameters =
        Some(config.customParameters.view.filterKeys(!toolConfig.customParamIsUneditable(_)).toMap).filter(_.nonEmpty),
    )
  end emit
end LtiAnalytics
