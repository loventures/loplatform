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

package loi.cp.session

import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.*
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.{ErrorResponse, RedirectResponse, WebResponse}
import com.learningobjects.cpxp.controller.login.LoginController
import com.learningobjects.cpxp.filter.CurrentFilter
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.util.HttpServletRequestOps.*
import com.learningobjects.cpxp.scala.util.HttpSessionOps.*
import com.learningobjects.cpxp.scala.util.Misc.*
import com.learningobjects.cpxp.service.{Current, ServiceException}
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.login.LoginException
import com.learningobjects.cpxp.service.login.LoginWebService.{Login as LWSLogin, LoginStatus as LWSStatus}
import com.learningobjects.cpxp.service.replay.ReplayService
import com.learningobjects.cpxp.service.user.{LoginRecord, UserDTO, UserFacade, UserWebService}
import com.learningobjects.cpxp.util.SessionUtils
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import loi.authoring.security.right.AccessAuthoringAppRight
import loi.cp.admin.right.AdminRight
import loi.cp.analytics.*
import loi.cp.analytics.entity.ExternallyIdentifiableEntity
import loi.cp.analytics.event.SessionEvent
import loi.cp.integration.{LoginSystemComponent, PasswordLoginSystem}
import loi.cp.password.*
import loi.cp.password.PasswordPolicyUtils.*
import loi.cp.right.{RightMatch, RightService}
import loi.cp.user.UserComponent
import scalaz.syntax.either.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scalaz.{-\/, \/, \/-}
import scaloi.misc.TimeSource
import scaloi.syntax.AnyOps.*
import scaloi.syntax.BooleanOps.*
import scaloi.syntax.DisjunctionOps.*

import java.lang as jl
import java.util.{Date, Properties, UUID}
import scala.collection.immutable.List
import scala.compat.java8.OptionConverters.*
import scala.concurrent.duration.*

@Component
class SessionRoot(val componentInstance: ComponentInstance)(
  analyticsService: Option[AnalyticsService],
  passwordRoot: PasswordRootApi,
  replayService: ReplayService,
  rightService: RightService,
  timeSource: TimeSource,
  userPasswordService: UserPasswordService,
  userWebService: UserWebService,
  sm: ServiceMeta,
  user: => UserDTO,
)(implicit
  cd: ComponentDescriptor,
  cs: ComponentService,
  fs: FacadeService,
) extends SessionRootComponent
    with ComponentImplementation:
  import SessionRoot.*

  override def session: Option[SessionComponent] =
    Current.isAnonymous.noption(component[SessionComponent])

  override def login(
    username: String,
    password: String,
    remember: jl.Boolean,
    mechanism: jl.Long,
    request: HttpServletRequest,
    response: HttpServletResponse
  ): LoginError \/ Login =
    for
      _     <- cookieTest(request) either true or CookiesBlocked().widen
      login <- authenticate(username, password, mechanism) -<| logError(username)
    yield
      loginCurrent(Option(remember).exists(_.booleanValue), request, response, login, mechanism)
      login

  private def logError(username: String)(error: LoginError): Unit =
    logger.warn(s"Login error for user: $username: $error")

  // Returns whether either cookies were included in the request or this is a non-interactive request.
  // Interactive login requests must present a cookie or we presume browser-based cookie blocking.
  private def cookieTest(request: HttpServletRequest): Boolean =
    Option(request.getCookies)
      .exists(_.nonEmpty) || !request.header(CurrentFilter.HTTP_HEADER_X_INTERACTIVE).contains("true")

  override def authenticate(username: String, password: String, mechanism: jl.Long): LoginError \/ Login =
    for
      policy     <- simplePolicy(passwordRoot)
      lwsLogin   <- loginAttempt(username, password, mechanism)
      user       <- findUser(lwsLogin.userId)
      login       = Login(user, lwsLogin.status, timeSource.date)
      history     = saveLoginHistory(user, login, HISTORY_SIZE) // Should I only record invalid?
      attempts   <- checkAttempts(history, policy, lwsLogin)
      validLogin <- mapLwsLoginStatus(lwsLogin)
      lastChanged = passwordLastChanged(user).getOrElse(synthesizePasswordChange(user, password, timeSource.date))
      expired    <- checkExpired(user, lwsLogin.external, lastChanged, policy)
    yield login

  private def synthesizePasswordChange(user: UserComponent, password: String, changed: Date): Date =
    passwordRoot.recordPasswordChange(user, password, changed)
    changed

  override def loginRedirect(
    username: String,
    pathOpt: String,
    request: HttpServletRequest
  ): ErrorResponse \/ RedirectResponse =
    for
      _       <- (request.isSecure || !sm.isProdLike) \/> ErrorResponse.unauthorized
      _       <- !user.isAnonymous \/> ErrorResponse.badRequest(new ServiceException("loginFailure"))
      _       <- user.userName.equalsIgnoreCase(username) \/> ErrorResponse.badRequest(new ServiceException("loginFailure"))
      path     = Option(pathOpt) | "/"
      admin    = rightService.getUserHasRight(classOf[AdminRight], RightMatch.ANY)
      author   = rightService.getUserHasRight(classOf[AccessAuthoringAppRight], RightMatch.ANY)
      redirect = if admin && path == "/" then "/Administration/"
                 else if !admin && path.startsWith("/Administration") then "/"
                 else if author && path == "/" then "/Authoring/"
                 else path
    yield RedirectResponse.temporary(redirect)

  override def logout(request: HttpServletRequest, response: HttpServletResponse): String =
    val user    = Current.getUserDTO
    val session = Option(Current.getSessionPk).map(_.longValue())
    if !Current.isAnonymous && session.isDefined then
      analyticsService foreach {
        val e = SessionEvent(
          id = UUID.randomUUID(),
          time = Current.getTime,
          source = Current.getDomainDTO.hostName,
          sessionId = session,
          actionType = AnalyticsConstants.EventActionType.END,
          user = ExternallyIdentifiableEntity(id = user.getId, user.externalId),
          requestUrl = null,
          ipAddress = null,
          referrer = null,
          acceptLanguage = null,
          userAgent = null,
          authMethod = None
        )
        _.emitEvent(e)
      }
    end if

    systemLogout(request) orElse {
      // Wondrous redirect url precedence.. this ought to be moved to the responsibility
      // of the system by which the user logged in, rather than only allowing login systems
      // to also logout.
      val url = Option(request.getSession(false))
        .flatMap(s =>
          s.attr[String](SessionUtils.SESSION_ATTRIBUTE_LOGOUT_RETURN_URL)
            .orElse(s.attr[String](SessionUtils.SESSION_ATTRIBUTE_RETURN_URL))
        )
      CurrentFilter.logout(request, response)
      // Store a record of this logout for a subsequent login required page to know why
      SessionUtils.setInfoCookie(response, SessionUtils.COOKIE_INFO_LOGGED_OUT, false)
      url
    } getOrElse ""
  end logout

  private def systemLogout(request: HttpServletRequest): Option[String] =
    for
      session   <- Option(request.getSession(false))
      mechanism <- session.attr[String](SessionUtils.SESSION_ATTRIBUTE_LOGIN_MECHANISM)
      system     = mechanism.toLong.component[LoginSystemComponent[?, ?]]
      url       <- Option(system.logout())
    yield url

  override def clearLoggedOut(request: HttpServletRequest, response: HttpServletResponse): Unit =
    SessionUtils.clearInfoCookie(request, response)

  // TODO: I am bad. But I work. Bring sudo and logback into this session controller's purview.
  override def exit(request: HttpServletRequest, response: HttpServletResponse): String =
    ComponentSupport.newInstance(classOf[LoginController]).logBack()

  private def simplePolicy(
    passwordRootComponent: PasswordRootApi
  ): LoginError \/ PasswordPolicy =
    passwordRootComponent.settingsAdmin.policy.right

  override def resurrect(
    sessionId: String,
    redirectUrl: String,
    resurrectToken: String,
    request: HttpServletRequest,
    response: HttpServletResponse
  ): WebResponse =
    if replayService.validateNonce(resurrectToken) then
      SessionUtils.setSessionCookie(response, true, sessionId, true)
      RedirectResponse.temporary(redirectUrl)
    else ErrorResponse(401)

  private def findUser(user: jl.Long): LoginError \/ UserComponent =
    Option(user.component[UserComponent])
      .toRightDisjunction(InvalidCredentialsError())

  private def passwordLastChanged(user: UserComponent): Option[Date] =
    Option(
      user.getId
        .facade[UserFacade]
        .getOrCreateHistory
        .getPasswordLastChanged
    )

  private def userLogins(user: UserComponent): Iterable[LoginRecord] =
    tryNullable {
      user.getId.facade[UserFacade].getOrCreateHistory.getLogins
    } { List.empty }

  private def saveLoginHistory(user: UserComponent, login: Login, historySize: Int): List[LoginRecord] =
    import loi.cp.password.updateHistory

    val loginHistory = user.facade[UserFacade].getOrCreateHistory
    updateHistory(userLogins(user), (ls: List[LoginRecord]) => loginHistory.setLogins(ls), historySize)(
      LoginRecord(login.status, login.timestamp)
    )

  // TODO: Kill LoginWebService and our myriad login error representations. Unify on this.
  // TODO: LWS.Login is a crappy structure to be returning, this should become some form of either
  private def loginAttempt(userName: String, password: String, mechanism: jl.Long): LoginError \/ LWSLogin =
    try
      val login = new LWSLogin()
      val mech  = loginMechanism(mechanism)
      login.userId = mech.login(userName, password)
      login.status = LWSStatus.OK
      login.external = mech.externalPassword()
      login.right[LoginError]
    catch
      case e: LoginException if e.getFailure == LWSStatus.InvalidCredentials =>
        // record a right on invalid credentials so the login attempt machinery happens.
        // this is later turned into a left. this is a bit uncool.
        logger info s"Login error: ${e.getFailure}"
        val login = new LWSLogin()
        login.userId = e.getUser
        login.status = LWSStatus.InvalidCredentials
        login.right[LoginError]

      case e: LoginException =>
        logger info s"Login error: ${e.getFailure}"
        statusError(e.getFailure).left[LWSLogin]

  // TODO: Kill the degenerate direct login once all user interfaces are upgraded to be mechanism-aware
  // so that a user can't bypass the configured login mechanisms?
  def loginMechanism(mechanism: jl.Long): PasswordLoginSystem[?, ?] =
    Option(mechanism) match
      case None =>
        ComponentSupport
          .get(classOf[FallbackLoginProviderComponent])
          .fallbackLoginSystem

      case Some(systemId) =>
        systemId.tryComponent[PasswordLoginSystem[?, ?]] match
          case None                                            =>
            throw new LoginException("Invalid system", LWSStatus.ServerError, null)
          case Some(system) if system.getDisabled.booleanValue =>
            throw new LoginException("Disabled system", LWSStatus.ServerError, null)
          case Some(system)                                    => system

  private def loginCurrent(
    remember: Boolean,
    request: HttpServletRequest,
    response: HttpServletResponse,
    login: Login,
    mechanism: jl.Long
  ): Unit =
    val userDTO = userWebService.getUserDTO(login.user.getId)
    val props   = new Properties() <| { p =>
      Option(mechanism) foreach { m =>
        p.setProperty(SessionUtils.SESSION_ATTRIBUTE_LOGIN_MECHANISM, m.toString)
      }
    }
    CurrentFilter.login(request, response, userDTO, remember, props)

    // TODO: remove this after a while since the DeanAnalyticsService should be turned ON by default (instead of off from before)
    val user = ComponentSupport.get(userDTO.getId, classOf[UserComponent])
    analyticsService foreach {
      val e = SessionEvent(
        id = UUID.randomUUID(),
        time = Current.getTime,
        source = Current.getDomainDTO.hostName,
        sessionId = Option(Current.getSessionPk).map(_.longValue()),
        actionType = AnalyticsConstants.EventActionType.START,
        user = ExternallyIdentifiableEntity(id = user.getId, user.getExternalId.asScala),
        requestUrl = request.getRequestURL.toString,
        ipAddress = request.getRemoteAddr,
        referrer = request.getHeader("referer"),
        acceptLanguage = request.getHeader("accept-language"),
        userAgent = request.getHeader("user-agent"),
        authMethod = Some(AnalyticsConstants.SessionAuthenticationMethod.DIRECT)
      )
      _.emitEvent(e)
    }
  end loginCurrent

  private def mapLwsLoginStatus(login: LWSLogin): LoginError \/ LWSLogin =
    (login.status == LWSStatus.OK).either(login).or(statusError(login.status))

  private def statusError(status: LWSStatus): LoginError = status match
    case LWSStatus.InvalidCredentials => InvalidCredentialsError()
    case LWSStatus.Suspended          => InvalidCredentialsError()
    case LWSStatus.Locked             => AccountLockedError()
    case LWSStatus.Pending            => AccountPendingError()
    case LWSStatus.Unconfirmed        => AccountUnconfirmedError()
    case LWSStatus.ServerError        => ServerError()
    case LWSStatus.OK                 => ServerError() // can't happen but

  private def checkExpired(
    user: UserComponent,
    externalPassword: Boolean,
    passwordLastChanged: Date,
    policy: PasswordPolicy
  ): LoginError \/ Date =
    policy.expireDuration match
      case Some(expireAge) if !externalPassword => expired(user, passwordLastChanged, expireAge.seconds)
      case _                                    => passwordLastChanged.right

  private def expired(user: UserComponent, passwordLastChanged: Date, expireAge: Duration): LoginError \/ Date =
    (diff(passwordLastChanged, timeSource.date) <= expireAge) either passwordLastChanged or PasswordExpiredError(
      userPasswordService.generateChangePasswordToken(user)
    )

  private def checkAttempts(
    history: Iterable[LoginRecord],
    policy: PasswordPolicy,
    login: LWSLogin
  ): LoginError \/ LWSLogin =
    val loginStatus                                                       = mapLwsLoginStatus(login)
    def foldAttempts: PartialFunction[(Int, Int), LoginError \/ LWSLogin] = { case (failedAttempts, interval) =>
      attempts(history)(HISTORY_SIZE, timeSource.date, interval.seconds, failedAttempts)
        .leftMap(error => TooManyFailedLoginsError(error, interval)) match
        case \/-(0) if loginStatus.isLeft  => loginStatus
        case \/-(0) if loginStatus.isRight =>
          TooManyFailedLoginsError(lockoutErrorMessage(interval.seconds), interval).left
        case \/-(_)                        => login.right
        case -\/(error)                    => error.left
    }

    val config: Option[(Int, Int)] = for
      failedAttempts <- policy.failedAttempts
      interval       <- policy.attemptInterval
    yield (failedAttempts, interval)

    config.fold(login.right[LoginError])(foldAttempts)
  end checkAttempts
end SessionRoot

object SessionRoot:
  private final val logger       = org.log4s.getLogger
  private final val HISTORY_SIZE = 50
