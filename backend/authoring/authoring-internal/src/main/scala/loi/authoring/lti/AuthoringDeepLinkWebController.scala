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
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.util.ToString
import com.learningobjects.cpxp.scala.util.URIBuilderOps.*
import com.learningobjects.cpxp.service.CurrentUrlService
import com.learningobjects.de.authorization.Secured
import loi.authoring.security.right.AccessAuthoringAppRight
import loi.authoring.web.AuthoringApiWebUtils
import loi.cp.lti.LtiWebUtils
import loi.cp.ltitool.{LtiToolComponent, LtiToolLaunchService}
import scalaz.std.option.*
import scalaz.syntax.traverse.*
import scaloi.syntax.option.*

import java.util.UUID
import scala.util.Try

@Component
@Controller(root = true)
@Secured(Array(classOf[AccessAuthoringAppRight]))
private[lti] class AuthoringDeepLinkWebController(
  val componentInstance: ComponentInstance,
  authoringWebUtils: AuthoringApiWebUtils,
  ltiUtils: LtiWebUtils,
  launchService: LtiToolLaunchService,
  cus: CurrentUrlService,
)(implicit
  cs: ComponentService,
) extends ApiRootComponent
    with ComponentImplementation:

  import AuthoringDeepLinkWebController.*

  // https://www.imsglobal.org/spec/lti-dl/v2p0/#deep-linking-request-message
  @RequestMapping(path = "authoring/{branch}/lti/{tool}/selectContent", method = Method.GET)
  def selectContent(
    @PathVariable("branch") branchId: Long,
    @PathVariable("tool") toolId: Long,
    @QueryParam("parent") parent: UUID,
    @QueryParam("after") after: Option[UUID],
    @QueryParam("before") before: Option[UUID],
  ): Try[HtmlResponse[RawHtml]] =

    authoringWebUtils.branchOrFakeBranchOrThrow404(branchId)

    // send before/after/parent in data field of request
    val returnUrl =
      cus.getUrl(s"/lti/adv/deep/$branchId/$toolId") ?
        ("parent" -> parent) & after.strengthL("after") & before.strengthL("before")
    val retUrl    = Some(returnUrl.toString)

    for
      tool     <- toolId.component_![LtiToolComponent]
      config    = tool.getLtiConfiguration.defaultConfiguration
      loginUrl <- config.loginUrl <@~* new IllegalStateException(s"No login URL")

      launchInfo = ltiUtils.defaultLaunchInfo()
      resourceId = toolId.toString
      params    <- launchService.get1p3LoginParameters(branchId, None, tool, launchInfo, resourceId, config, true, retUrl)
    yield ServletDispatcher.autopost(loginUrl, params.toMap)
  end selectContent

  object AuthoringDeepLinkWebController:
    private final val logger = org.log4s.getLogger

    implicit val toStringUUID: ToString[UUID] = _.toString
end AuthoringDeepLinkWebController
