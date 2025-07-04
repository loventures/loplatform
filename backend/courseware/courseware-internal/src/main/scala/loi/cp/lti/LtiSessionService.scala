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

import java.util.UUID

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.filter.CurrentFilter
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.session.SessionService
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.cpxp.util.SessionUtils
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import loi.cp.analytics.AnalyticsConstants.EventActionType
import loi.cp.analytics.event.SessionEvent
import loi.cp.analytics.{AnalyticsConstants, AnalyticsService}
import loi.cp.integration.BasicLtiSystemComponent
import loi.cp.lti.spec.LtiTarget
import loi.cp.user.UserComponent
import scalaz.\/
import scalaz.std.option.*
import scalaz.syntax.traverse.*
import scaloi.misc.TimeSource
import scaloi.syntax.OptionOps.*

/** Responsible for logging the user in and setting up session attributes from the launch.
  */
@Service
class LtiSessionService(
  currentUser: () => UserDTO,
  domain: () => DomainDTO,
  sessionService: SessionService,
  analyticsService: AnalyticsService,
  ts: TimeSource
):
  import LtiSessionService.*
  import SessionUtils.{
    SESSION_ATTRIBUTE_CUSTOM_TITLE as CustomTitleAttribute,
    SESSION_ATTRIBUTE_LOGOUT_RETURN_URL as LogoutReturnUrlAttribute,
    SESSION_ATTRIBUTE_RETURN_URL as ReturnUrlAttribute
  }

  /** Log in as the specified user and set up appropriate session attributes.
    */
  def login(user: UserComponent)(implicit
    request: HttpServletRequest,
    response: HttpServletResponse,
    system: BasicLtiSystemComponent
  ): LtiError \/ Unit =
    for
      attributes   <- sessionAttributes
      reuseSession <- ltiParamT[Boolean](CustomReuseSessionParameter)
    yield
      val properties = attributes.toProperties
      if reuseSession.isTrue && (user.getId == currentUser().getId) then
        attributes foreach (request.getSession.setAttribute).tupled
        sessionService.setProperties(Current.getSessionId, properties)
      else CurrentFilter.login(request, response, user.toDTO, false, properties)
      analyticsService.emitEvent(
        SessionEvent(
          id = UUID.randomUUID(),
          time = ts.date,
          source = domain.hostName,
          sessionId = Some(Current.getSessionPk),
          user = user,
          actionType = EventActionType.START,
          requestUrl = request.getRequestURL.toString,
          ipAddress = request.getRemoteAddr,
          referrer = request.getHeader("referer"),
          acceptLanguage = request.getHeader("accept-language"),
          userAgent = request.getHeader("user-agent"),
          authMethod = Some(AnalyticsConstants.SessionAuthenticationMethod.LTI),
          lastActive = None,
          becameActive = None
        )
      )

  /** Gather a list of launch parameters to store as session attributes.
    */
  private def sessionAttributes(implicit
    request: HttpServletRequest,
    system: BasicLtiSystemComponent
  ): LtiError \/ List[(String, String)] =
    for
      returnUrl         <- ltiParam(LaunchPresentationReturnUrlParameter)
      logoutReturnUrl   <- ltiParam(CustomLogoutReturnUrlParameter)
      customCourseTitle <- ltiParam(CustomCourseTitleParameter)
      target            <- ltiParamT[LtiTarget](CustomTargetParameter, LaunchPresentationDocumentTargetParameter)
    yield List(LtiIntegratedAttribute -> True) ++
      returnUrl.strengthL(ReturnUrlAttribute) ++
      logoutReturnUrl.strengthL(LogoutReturnUrlAttribute) ++
      customCourseTitle.strengthL(CustomTitleAttribute) ++
      target.map(_.entryName).strengthL(LtiTargetAttribute)
end LtiSessionService

object LtiSessionService:
  private final val LaunchPresentationDocumentTargetParameter = "launch_presentation_document_target"
  private final val LaunchPresentationReturnUrlParameter      = "launch_presentation_return_url"
  private final val CustomTargetParameter                     = "custom_target"
  private final val CustomLogoutReturnUrlParameter            = "custom_logout_return_url"
  private final val CustomReuseSessionParameter               = "custom_reuse_session"
  private final val CustomCourseTitleParameter                = "custom_course_title"

  // used by [[loi.cp.session.Session]]
  private final val LtiIntegratedAttribute = "ltiLaunch"
  private final val LtiTargetAttribute     = "ltiTarget"

  private final val True = "true"
end LtiSessionService
