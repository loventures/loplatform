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

package loi.cp.password

import com.learningobjects.cpxp.component.annotation.{Component, Messages}
import com.learningobjects.cpxp.component.web.HttpContext
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.filter.CurrentFilter
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.domain.{DomainDTO, DomainWebService}
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.login.LoginWebService
import com.learningobjects.cpxp.service.query.*
import com.learningobjects.cpxp.service.session.SessionService
import com.learningobjects.cpxp.service.token.{TokenFinder, TokenType}
import com.learningobjects.cpxp.service.user.{UserConstants, UserDTO, UserFolderFacade, UserState}
import jakarta.servlet.http.HttpServletRequest
import loi.cp.admin.right.AdminRight
import loi.cp.analytics.entity.ExternallyIdentifiableEntity
import loi.cp.analytics.event.SessionEvent
import loi.cp.analytics.{AnalyticsConstants, AnalyticsService}
import loi.cp.password.ChangePasswordReceipt.{EmailAddressNotFound, EmailFailedReason, MessagingError}
import loi.cp.presence.{LogoutEvent, PresenceService}
import loi.cp.right.{RightMatch, RightService}
import loi.cp.user.UserComponent
import scalaz.std.list.*
import scalaz.syntax.either.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scalaz.{\/, \/-}
import scaloi.std.ju.*
import scaloi.syntax.any.*
import scaloi.syntax.boolean.*
import scaloi.syntax.option.*
import scaloi.syntax.seq.*

import java.util.{Date, UUID}
import scala.compat.java8.OptionConverters.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

@Component
@Messages(
  Array(
    "$$email_recover_subject={domain.name} - Password reset",
    "$$email_recover_body=Please follow this link to change your password:\n\n  {url}\n\nThanks.",
    "$$email_invite_subject={domain.name} - Account invitation",
    "$$email_invite_body={user.givenName} -\n\nAn account was created for you in {domain.name}.\nYour username is {user.userName}.\n\nPlease follow this link to activate your account:\n\n  {url}\n\nThanks."
  )
)
class RecoverPasswordRootApiImpl(val componentInstance: ComponentInstance, loginWebService: LoginWebService)(implicit
  fs: FacadeService,
  ups: UserPasswordService,
  dws: DomainWebService,
  pr: PasswordRootApi,
  as: AnalyticsService,
  qs: QueryService,
  rs: RightService,
  ss: SessionService,
  ps: PresenceService,
  date: Date,
  domain: DomainDTO,
  user: UserDTO,
  cs: ComponentService
) extends RecoverPasswordRootApi
    with ComponentImplementation:

  import AccountLookup.*
  import RecoverPasswordRootApi.*
  import RecoverPasswordRootApiImpl.*

  // When throttling password recovery I have to apply backoff and failure to successful and
  // unsuccessful attempts alike; otherwise the attacker can just launch a storm of concurrent
  // attempts and any successes will resolve quickly

  override def recoverPassword(
    search: String,
    properties: Seq[String],
    redirect: String
  ): Future[PasswordError \/ Unit] =
    for recover <- runThrottled(recoverImpl(search, properties, redirect))
    yield recover getOrElse PasswordError(PasswordReason.Unavailable).left

  override def validateToken(tokenId: String): PasswordError \/ ResetValidity =
    for
      token <- findToken(tokenId)
      user  <- tokenUser(token)
    yield ResetValidity(user.getUserName, token.ttype)

  override def resetPassword(tokenId: String, password: String, context: HttpContext): PasswordError \/ Unit =
    for
      token <- findToken(tokenId)
      user  <- tokenUser(token)
      _     <- validatePassword(user, password)
    yield
      token.accepted = true
      ps.deliverToUsers(LogoutEvent)(user.id)
      ss.invalidateUserSessions(user.getId)
      CurrentFilter.login(context.request, context.response, user.toDTO, false)
      // do this after logging in so this is considered a non-expiring self-change
      user.setPassword(password)
      emitLoginEvent(user, context.request)

  override def rejectAccount(tokenId: String): PasswordError \/ Unit =
    for
      token <- findToken(tokenId)
      user  <- tokenUser(token)
    yield
      token.accepted = true
      user.transition(UserState.Suspended)

  /** Actually perform password recovery. */
  private def recoverImpl(search: String, properties: Seq[String], redirect: String): PasswordError \/ Unit =
    for
      settings <- isRecoveryEnabled
      _        <- validateRedirect(redirect)
      user     <- findUser(search, properties, settings)
      _        <- mayRecover(settings, user)
      _        <- sendRecoveryEmail(user, redirect)
    yield ()

  /** Validate a token reset redirect URL. */
  private def validateRedirect(redirect: String): PasswordError \/ Unit =
    RedirectRE.matches(redirect) \/> PasswordError(PasswordReason.BadRequest)

  private final val RedirectRE = "^/[-_a-zA-Z0-9/#]*$".r // restrict it to a local relative url

  /** Get the recovery settings or return an error if not enabled. */
  private def isRecoveryEnabled: PasswordError \/ PasswordRecoverySettings =
    pr.settingsAdmin.recovery.rightWhen(_.enabled) {
      PasswordError(PasswordReason.NotEnabled)
    }

  /** Find the user that matches the search criteria. */
  private def findUser(
    search: String,
    properties: Seq[String],
    settings: PasswordRecoverySettings
  ): PasswordError \/ UserComponent =
    for
      users <- findUsers(search, properties.toList)
      _     <- users.nonEmpty \/> PasswordError(PasswordReason.NotFound)
      _     <- users.hasSize(1) \/> PasswordError(PasswordReason.Ambiguous)
      user   = users.head
      _     <- (UserState.Pending != user.getUserState) \/> PasswordError(PasswordReason.Pending)
      _     <- (UserState.Active == user.getUserState) \/> PasswordError(PasswordReason.NotFound)
      _     <- hasPassword(user, settings)
    yield user

  private def hasPassword(user: UserComponent, settings: PasswordRecoverySettings): PasswordError \/ Unit =
    if settings.requireExistingPassword then
      loginWebService.hasPassword(user.getId) \/> PasswordError(PasswordReason.DirectAccessProhibited)
    else \/-(())

  /** Find users that match the search criteria. */
  private def findUsers(search: String, properties: Seq[String]): PasswordError \/ Seq[UserComponent] =
    for disjunction <- properties.toList.traverseU(propertySearch(search))
    yield UserConstants.ID_FOLDER_USERS
      .facade[UserFolderFacade]
      .queryUsers
      .addDisjunction(disjunction)
      .setLimit(2)
      .getComponents[UserComponent]

  /** Translate a search property into a query condition. */
  private def propertySearch(search: String)(property: String): PasswordError \/ Condition =
    PropertyMap
      .get(property)
      .map(dt => BaseCondition.getInstance(dt, Comparison.eq, search, Function.LOWER))
      .toRightDisjunction(PasswordError(PasswordReason.BadRequest))

  /** Test whether a user may recovery their password. */
  private def mayRecover(recovery: PasswordRecoverySettings, user: UserComponent): PasswordError \/ Unit =
    (recovery.adminAllowed || !rs.getUserHasRight(domain, user, classOf[AdminRight], RightMatch.ANY)) \/>
      PasswordError(PasswordReason.AdminDisallowed)

  /** Send a password recovery email. */
  private def sendRecoveryEmail(
    user: UserComponent,
    redirect: String,
  ): PasswordError \/ Unit =
    ups
      .resetPassword(user, initiate = false, redirect.concat)
      .emailError
      .map(emailError) <\/ (())

  /** Map an email error to a password recovery error. */
  private def emailError(reason: EmailFailedReason): PasswordError = reason match
    case MessagingError(e)    =>
      logger.warn(e)("Error sending recovery email")
      PasswordError(PasswordReason.EmailError)
    case EmailAddressNotFound =>
      PasswordError(PasswordReason.EmailError)

  /** Validate a new password. */
  private def validatePassword(user: UserComponent, password: String): PasswordError \/ Unit =
    OptionNZ(pr.getPasswordErrors(user, password))
      .map(errors => PasswordError(PasswordReason.InvalidPassword, errors.asScala.toSeq)) <\/ (())

  /** Emit a login event. */
  private def emitLoginEvent(user: UserComponent, request: HttpServletRequest): Unit =
    as `emitEvent` SessionEvent(
      id = UUID.randomUUID,
      time = date,
      source = domain.hostName,
      sessionId = Option(Current.getSessionPk).map(_.longValue()), // omg
      actionType = AnalyticsConstants.EventActionType.START,
      user = ExternallyIdentifiableEntity(id = user.getId, externalId = user.getExternalId.asScala),
      requestUrl = request.getRequestURL.toString,
      ipAddress = request.getRemoteAddr,
      referrer = request.getHeader("referer"),
      acceptLanguage = request.getHeader("accept-language"),
      userAgent = request.getHeader("user-agent"),
      authMethod = Some(AnalyticsConstants.SessionAuthenticationMethod.DIRECT)
    )

  /** Find a recovery token. */
  private def findToken(token: String): PasswordError \/ TokenFinder =
    getTokenByTokenId(token).filter(tokenIsValid) \/> PasswordError(PasswordReason.NotFound)

  /** Test whether a token is valid. */
  private def tokenIsValid(token: TokenFinder): Boolean =
    ((token.ttype == TokenType.Recover) || (token.ttype == TokenType.Register)) &&
      !Option(token.accepted).isTrue && date.before(token.expires)

  /** Get the user associated with a recovery token. */
  private def tokenUser(token: TokenFinder): PasswordError \/ UserComponent =
    for
      user <- token.getParent.component[UserComponent].right
      _    <- !user.getUserState.getDisabled \/> PasswordError(PasswordReason.NotFound)
    yield user

  private def getTokenByTokenId(token: String): Option[TokenFinder] =
    domain
      .queryAll[TokenFinder]
      .addCondition(TokenFinder.DATA_TYPE_TOKEN_ID, Comparison.eq, token)
      .getFinders[TokenFinder]
      .headOption
end RecoverPasswordRootApiImpl

object RecoverPasswordRootApiImpl:
  private final val logger = org.log4s.getLogger

  private final val PropertyMap =
    Map("userName" -> UserConstants.DATA_TYPE_USER_NAME, "emailAddress" -> UserConstants.DATA_TYPE_EMAIL_ADDRESS)
