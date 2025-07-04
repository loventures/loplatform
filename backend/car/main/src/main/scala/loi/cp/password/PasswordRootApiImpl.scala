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

import cats.syntax.option.*
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.{
  ComponentDescriptor,
  ComponentImplementation,
  ComponentInstance,
  ComponentService
}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.util.Misc.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.script.ScriptService
import com.learningobjects.cpxp.service.session.SessionService
import com.learningobjects.cpxp.service.user.{LoginRecord, UserDTO, UserFacade}
import loi.cp.accountRequest.AccountRequestRootComponent
import loi.cp.config.JsonSchema.*
import loi.cp.config.{ConfigurationKey, ConfigurationKeyBinding, ConfigurationService}
import loi.cp.i18n.Translatable
import loi.cp.user.UserComponent
import scalaz.std.option.*
import scalaz.syntax.either.*
import scalaz.syntax.foldable.*
import scalaz.syntax.monad.*
import scalaz.{ValidationNel, \/}

import java.util as ju
import java.util.{Date, Optional}
import scala.jdk.CollectionConverters.*

@Component
class PasswordRootApiImpl(val componentInstance: ComponentInstance)(implicit
  configService: ConfigurationService,
  facadeService: FacadeService,
  mapper: ObjectMapper,
  scriptService: ScriptService,
  sessionService: SessionService,
  user: () => UserDTO,
  cs: ComponentService
) extends PasswordRootApi
    with ComponentImplementation:

  import PasswordRootApiImpl.*
  import loi.cp.password.PasswordPolicyUtils.*

  private implicit val componentDescriptor: ComponentDescriptor = componentInstance.getComponent

  override def settings: PasswordSettingsSummary =
    val cf = settingsAdmin
    PasswordSettingsSummary(
      recovery = cf.recovery.enabled,
      accountRequests = cf.accountRequests.statusEnum != AccountRequestRootComponent.Status.Disabled,
      validation = List(userFieldError) ++
        cf.policy.minSize.filter(_ > 0).map(minLengthError(_)) ++
        policy.alphaNumeric.filter(identity).as(alphaNumericError) ++
        policy.hasNonAlpha.filter(identity).as(nonAlphaError) ++
        policy.uniquePasswords.filter(_ > 0).map(uniqueError(_))
    )
  end settings

  override def settings(`new`: JsonNode): Translatable.Any \/ Unit =
    domainPasswordConfig.setDomain(`new`).void

  override def settingsAdmin: PasswordSettings =
    domainPasswordConfig.getDomain

  override def changePassword(currentPassword: Password, password: Password): PasswordError \/ Unit =
    for
      self <- user.component[UserComponent].right
      // TODO: OMG: ACTUALLY TEST CURRENT PASSWORD!!!
      pass <- validateNewPassword(Some(self), password)
    yield
      self.setPassword(pass)
      // TODO: This ought to send out a LogoutEvent to this user on all sessions except
      // the current session. Not currently possible.
      sessionService.invalidateUserSessions(
        user.id,
        Optional.ofNullable(Current.getSessionPk)
      ) // double, nay, triple shame

  // This deliberately does not check the current user's previous passwords
  // because that would allow an easier attack against a user's password history.
  override def validatePassword(password: Password): PasswordError \/ Unit =
    validateNewPassword(None, password).void

  private def validateNewPassword(user: Option[UserComponent], password: Password): PasswordError \/ Password =
    validatePasswordChange(policy, user, password).toDisjunction
      .leftMap(errors => PasswordError(PasswordReason.InvalidPassword, errors.toList))

  def validatePasswordChange(
    policy: PasswordPolicy,
    user: Option[UserComponent],
    password: Password
  ): ValidationNel[Error, Password] =
    val userCheck = user map { u =>
      notUserField(u)
    }
    val min       = policy.minSize.filter(_ > 0) map { m =>
      minLength(m)
    }
    val alphaNum  = policy.alphaNumeric.filter(identity) as {
      alphaNumeric
    }
    val hasSymbol = policy.hasNonAlpha.filter(identity) as {
      hasNonAlpha
    }

    val userHistory = for
      uniquePasswords <- policy.uniquePasswords
      userComponent   <- user
      userId          <- Option(userComponent.getId) // new user has no PK or history
      passwordHistory  = userPasswordHistory(userComponent)
    yield unique(passwordHistory.to(LazyList), userId)(uniquePasswords)

    reduceValidation(
      List(
        userCheck,
        min,
        alphaNum,
        hasSymbol,
        userHistory
      ),
      password
    )
  end validatePasswordChange

  override def recordPasswordChange(
    user: UserComponent,
    password: Password,
    changed: Date
  ): Unit =
    val loginHistory =
      user.facade[UserFacade].getOrCreateHistory

    loginHistory.setPasswordLastChanged(changed)
    loginHistory.setLogins(List.empty) // clear failed login history so you can login again

    val uniquePasswords = policy.uniquePasswords.getOrElse(1) // always record at least one..
    updateHistory(
      getter = userPasswordHistory(user),
      setter = (ps: List[String]) => loginHistory.setPasswords(ps),
      historySize = uniquePasswords
    ) {
      encodePassword(user.getId, password)
    }
  end recordPasswordChange

  override def getPasswordErrors(user: UserComponent, password: Password): ju.List[String] =
    validatePasswordChange(policy, Some(user), password)
      .fold(_.toList, x => List.empty)
      .asJava

  private def userPasswordHistory(user: UserComponent): Iterable[String] =
    tryNullable {
      user.facade[UserFacade].getOrCreateHistory.getPasswords
    } {
      List.empty
    }

  private def userLogins(user: UserComponent): Iterable[LoginRecord] =
    tryNullable {
      user.getId.facade[UserFacade].getOrCreateHistory.getLogins
    } {
      List.empty
    }

  // policy/settings
  private def policy: PasswordPolicy =
    settingsAdmin.policy
end PasswordRootApiImpl

object PasswordRootApiImpl:

  def defaultSettings: PasswordSettings =
    PasswordSettings(
      policy = PasswordPolicy.standard,
      recovery = PasswordRecoverySettings(enabled = true, adminAllowed = false, requireExistingPassword = true),
      accountRequests = AccountRequestSettings("Disabled", usernameIsEmailAddress = true, notifyUsers = ""),
      selfRegistration = SelfRegistrationSettings(enabled = false)
    )

  def passwordSettings: ConfigurationKey[PasswordSettings] = domainPasswordConfig

  @ConfigurationKeyBinding("password")
  object domainPasswordConfig extends ConfigurationKey[PasswordSettings]:
    override final val schema = Schema(
      title = "Passwords".some,
      properties = List(
        ObjectField(
          name = "accountRequests",
          title = Some("Account Request Settings"),
          properties = List(
            StringField(name = "notifyUsers"),
            StringField(name = "status", description = Some("Disabled, Manual, Verify, Automatic")),
            BooleanField(name = "usernameIsEmailAddress"),
          )
        ),
        ObjectField(
          name = "policy",
          title = Some("Password Policy"),
          properties = List(
            BooleanField(name = "alphaNumeric", description = Some("Require at least one letter and one digit.")),
            NumberField(
              name = "attemptInterval",
              description = Some("Time window (seconds) in which to count failed attempts.")
            ),
            NumberField(name = "expireDuration", description = Some("Password expiration time (seconds).")),
            NumberField(name = "failedAttempts", description = Some("Maximum failed attempts before account lockout.")),
            BooleanField(name = "hasNonAlpha", description = Some("Require at least one symbol character.")),
            NumberField(name = "minSize", description = Some("Minimum password length.")),
            NumberField(
              name = "uniquePasswords",
              description = Some("Number of recent passwords that may not be re-used.")
            ),
          )
        ),
        ObjectField(
          name = "recovery",
          title = Some("Password Recovery Settings"),
          properties = List(
            BooleanField(name = "adminAllowed"),
            BooleanField(name = "enabled"),
            BooleanField(
              name = "requireExistingPassword",
              description = Some("When checked, only users that already have a password can proceed through recovery.")
            )
          )
        ),
        ObjectField(
          name = "selfRegistration",
          title = Some("Self Registration Settings"),
          properties = List(
            BooleanField(name = "enabled")
          )
        ),
      )
    )

    override val init: PasswordSettings = defaultSettings
  end domainPasswordConfig
end PasswordRootApiImpl
