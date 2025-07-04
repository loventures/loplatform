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

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.util.HtmlTemplate
import com.learningobjects.cpxp.component.web.{HtmlResponse, RedirectResponse, WebResponse}
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.util.HttpServletRequestOps.*
import com.learningobjects.cpxp.scala.util.URIBuilderOps.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.session.{SessionDTO, SessionDomainFacade, SessionFacade, SessionSupport}
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.cpxp.util.{GuidUtil, SessionUtils}
import de.tomcat.juli.LogMeta
import scalaz.\/
import scalaz.syntax.std.option.*
import scaloi.syntax.boolean.*
import scaloi.syntax.date.*

import java.util.Date
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}

import scala.concurrent.duration.*

/** The cookie dance tries to support browsers that block cookies for iframed content that the viewer has never visited.
  */
@Service
class LtiDanceService(
  session: () => SessionDTO,
  user: () => UserDTO,
  domain: () => DomainDTO,
  now: () => Date
)(implicit facadeService: FacadeService):
  import LtiDanceService.*

  /** Launch into the cookie dance. In principle, if I get cookies on this initial request then they should stick and I
    * don't need to dance but that seems unreliable so always do so.
    */
  def cookieDance(self: AnyRef, redirect: String): WebResponse =
    val danceSession = createDanceSession(redirect)
    val uri          = WaltzPath ? (TokenParameter -> danceSession.getSessionId)
    val guid         = GuidUtil.errorGuid()
    LogMeta.put("guid", guid)
    logger.info(s"Cookie waltz")
    HtmlResponse(HtmlTemplate(self, "redirect.html").bind("redirect", uri.toString).bind("guid", guid))

  // put a transient dance session in the database that we can use to resurrect the real
  // session. we don't know what appserver the user will land on so can't use appserver
  // state, and it is considered bad security practice to send a real session id in a url
  private def createDanceSession(redirect: String): SessionFacade =
    domain.addFacade[SessionFacade] { facade =>
      facade.setSessionId(SessionSupport.getTransientId)
      facade.setCreated(now())
      facade.setExpires(now() + 5.minutes)
      facade.setProperties(s"${session.sessionId}:$redirect")
    }

  /** This is rendered in the original frame after an lti launch if the browser presented no cookies If the user is
    * logged in then proceed, else prompt them.
    */
  def waltz(self: AnyRef, request: HttpServletRequest, response: HttpServletResponse): LtiError \/ WebResponse =
    for danceSession <- validateDanceSession(request)
    yield
      if !user.isAnonymous then
        // we are seeing concurrent waltz and tango requests, and the tango is failing
        // because the following line deletes the session so suppress it for now...
        // danceSession.facade.delete()
        RedirectResponse.temporary(danceSession.redirect)
      else
        Option(request.getCookies).filterNot(_.isEmpty) foreach { cs =>
          logger info s"Launch request with cookies but no session: ${cs.map(_.getName).mkString(",")}"
        }
        val uri  = TangoPath ? (TokenParameter -> danceSession.facade.getSessionId)
        val guid = GuidUtil.errorGuid()
        LogMeta.put("guid", guid)
        logger.info(s"Cookie tango")
        HtmlResponse(HtmlTemplate(self, "cookieCheck.html").bind("redirect", uri.toString).bind("guid", guid))

  /** In a new top-level window, try to set some cookies and redirect to next page. */
  def tango(self: AnyRef, request: HttpServletRequest, response: HttpServletResponse): LtiError \/ WebResponse =
    for danceSession <- validateDanceSession(request)
    yield
      SessionUtils.setSessionCookie(response, request.isSecure, danceSession.sessionId, false)
      val uri = FoxtrotPath ? (TokenParameter -> danceSession.facade.getSessionId)
      HtmlResponse(HtmlTemplate(self, "redirect.html").bind("redirect", uri.toString))

  /** This is rendered in the top-level window after setting some cookies. */
  def foxtrot(self: AnyRef, request: HttpServletRequest, response: HttpServletResponse): LtiError \/ WebResponse =
    for
      danceSession <- validateDanceSession(request)
      _            <- user.isAnonymous \/>! GenericLtiError("lti_cookies_blocked").widen
    yield
      val uri = HustlePath ? (TokenParameter -> danceSession.facade.getSessionId)
      HtmlResponse(HtmlTemplate(self, "cookiePass.html").bind("redirect", uri.toString))

  /** This is rendered in the original window after reloading. */
  def hustle(self: AnyRef, request: HttpServletRequest, response: HttpServletResponse): LtiError \/ WebResponse =
    for
      danceSession <- validateDanceSession(request)
      _            <- user.isAnonymous \/>! GenericLtiError("lti_cookies_blocked").widen
    yield
      danceSession.facade.delete()
      RedirectResponse.temporary(danceSession.redirect)

  private def validateDanceSession(request: HttpServletRequest): LtiError \/ DanceSessionData =
    for
      token   <- request.param(TokenParameter) \/> MissingLtiParameter(TokenParameter).widen
      session <- domainSessions.findSessionBySessionId(token) \/> GenericLtiError("lti_token_error").widen
      _       <- session.getExpires.after(now()) \/> GenericLtiError("lti_token_error").widen
    yield
      val DanceSessionPropertiesRE(sessionId, redirect) = session.getProperties: @unchecked
      DanceSessionData(session, sessionId, redirect)

  private def domainSessions: SessionDomainFacade = domain.facade[SessionDomainFacade]
end LtiDanceService

object LtiDanceService:

  import loi.cp.lti.LightweightLtiServlet.Path

  private final val logger = org.log4s.getLogger

  private final val TokenParameter = "token"

  final val WaltzPath   = s"$Path/waltz"
  final val TangoPath   = s"$Path/tango"
  final val FoxtrotPath = s"$Path/foxtrot"
  final val HustlePath  = s"$Path/hustle"

  // strictly limit redirects to test sections, sections, bar (integration tests) and the domain, optionally with #
  final val ValidRedirectRE = "(?:/TestSections/.+|/Courses/.+|/bar(?:/.*)?|/)(?:#.*)?".r

  private final case class DanceSessionData(
    facade: SessionFacade,
    sessionId: String,
    redirect: String
  )

  private final val DanceSessionPropertiesRE = "(.*):(.*)".r

  /** Dispatch to the appropriate step in the cookie dance. No, these are not steps in a dance, they are dances..
    */
  object Dance:
    def unapply(path: String)(implicit
      ltiDanceService: LtiDanceService
    ): Option[(AnyRef, HttpServletRequest, HttpServletResponse) => LtiError \/ WebResponse] =
      PartialFunction.condOpt(path) {
        case WaltzPath   => ltiDanceService.waltz
        case TangoPath   => ltiDanceService.tango
        case FoxtrotPath => ltiDanceService.foxtrot
        case HustlePath  => ltiDanceService.hustle
      }
  end Dance
end LtiDanceService
