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

package loi.cp.lti

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.util.HtmlTemplate
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{
  ComponentDescriptor,
  ComponentImplementation,
  ComponentInstance,
  ComponentInterface
}
import com.learningobjects.cpxp.scala.util.I18nMessage
import com.learningobjects.cpxp.service.group.GroupConstants
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import loi.cp.integration.BasicLtiSystemComponent
import org.apache.commons.lang3.exception.ExceptionUtils
import scalaz.\/
import scalaz.syntax.either.*
import scaloi.syntax.disjunction.*

/** Main web entry point for LTI launch into LO.
  */
@Component
@ServletBinding(path = LightweightLtiServlet.Path)
class LightweightLtiServlet(val componentInstance: ComponentInstance)(implicit
  ltiDanceService: LtiDanceService,
  ltiLaunchService: LtiLaunchService,
  ltiValidationService: LtiValidationService
) extends ServletComponent
    with ServletDispatcher
    with ComponentImplementation
    with LtiErrorHandling:

  import LightweightLtiServlet.*
  import ServletDispatcher.*
  import GroupConstants.{ID_FOLDER_COURSES as SectionFolder, ID_FOLDER_TEST_SECTIONS as TestSectionFolder}
  import LtiLaunchService.{Offering, Section}

  protected override val logger = org.log4s.getLogger

  /** Handle different incoming launch requests. */
  override protected def handler: RequestHandler = {

    // /lwlti/offering/{offering}(/{path})
    case RequestMatcher(GetOrPost(), OfferingPath(offering, path), req, resp) =>
      lti(req, resp, ltiLaunchService.launchCourse(Offering(offering), Option(path))(using _, _, _))

    // /lwlti/section/{externalId}(/{path})
    case RequestMatcher(GetOrPost(), SectionPath(externalId, path), req, resp) =>
      lti(req, resp, ltiLaunchService.launchCourse(Section(SectionFolder, externalId), Option(path))(using _, _, _))

    // /lwlti/testSection/{externalId}(/{path})
    case RequestMatcher(GetOrPost(), TestSectionPath(externalId, path), req, resp) =>
      lti(req, resp, ltiLaunchService.launchCourse(Section(TestSectionFolder, externalId), Option(path))(using _, _, _))

    // /lwlti/testSection/{externalId}(/{path})
    case RequestMatcher(GetOrPost(), UrlPath(pi), req, resp) =>
      lti(req, resp, ltiLaunchService.launchUrl(Option(pi))(using _, _, _))

    case RequestMatcher(Method.GET, LtiDanceService.Dance(dance), req, resp) =>
      dance(this, req, resp) `leftFlatMap` errorHandler

    case RequestMatcher(_, unknown, _, _) =>
      errorHandler(GenericLtiError("lti_unknown_launch_url", unknown))
  }

  /** Invokes an LTI request handler after validating the LTI launch signature, then transforms the result into an
    * appropriate web response.
    */
  private def lti(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handle: LtiHandler
  ): EitherResponse =
    val result = for
      system   <- ltiValidationService.validateLtiRequest(using request)
      redirect <- handle(request, response, system)
    yield ltiDanceService.cookieDance(this, redirect)
    result `leftFlatMap` errorHandler
  end lti
end LightweightLtiServlet

trait LtiErrorHandling:
  self: ComponentInterface =>
  import ServletDispatcher.*

  protected val logger: org.log4s.Logger

  private implicit def componentDescriptor: ComponentDescriptor = getComponentInstance.getComponent

  /** Transform an LTI error into a web response. */
  def errorHandler(e: LtiError): EitherResponse =
    e match
      case InternalLtiError(msg, th) =>
        logger.warn(th)(s"Internal error: $msg")
        HtmlResponse(
          errorTemplate(e, ExceptionUtils.getRootCauseStackTrace(th)),
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        ).left

      case e @ FriendlyLtiError(_, _, sc) =>
        logger warn s"Launch error: $e"
        HtmlResponse(friendlyTemplate(e), sc).widen.right

      case ltiError =>
        logger warn s"Launch error: $ltiError"
        HtmlResponse(errorTemplate(ltiError), HttpServletResponse.SC_BAD_REQUEST).left

  /** Construct an error template from an LTI error. */
  private def errorTemplate(e: LtiError, st: Array[String] = Array.empty): HtmlTemplate =
    HtmlTemplate(this, "ltiError.html")
      .bind(
        "msg"        -> e.msg,
        "message"    -> I18nMessage(e.msg).i18n(e),
        "detail"     -> I18nMessage(e.detailMsg).i18n(e),
        "stackTrace" -> st
      )

  /** Construct an error template from an LTI error. */
  private def friendlyTemplate(e: FriendlyLtiError): HtmlTemplate =
    HtmlTemplate(this, "friendlyError.html")
      .bind("msg" -> e.msg, "message" -> I18nMessage(e.msg).i18n(e), "detail" -> I18nMessage(e.detailMsg).i18n(e))
end LtiErrorHandling

object LightweightLtiServlet:
  private final val token    = "/([^/]+)"
  private final val tokenOpt = "(?:/([^/]+))?"

  /** Launch paths. */
  final val Path            = "/lwlti"
  final val UrlPath         = s"$Path/url(?:/(.*))?".r
  final val OfferingPath    = s"$Path/offering$token$tokenOpt".r
  final val SectionPath     = s"$Path/section$token$tokenOpt".r
  final val TestSectionPath = s"$Path/testSection$token$tokenOpt".r

  /** An LTI request handler accepts a request, response and LTI system and generates a redirect URL. */
  type LtiHandler = (HttpServletRequest, HttpServletResponse, BasicLtiSystemComponent) => LtiError \/ String
end LightweightLtiServlet
