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

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method, Mode}
import com.learningobjects.de.authorization.Secured
import enumeratum.{Enum, EnumEntry}
import loi.cp.accountRequest.AccountRequestRootComponent
import loi.cp.admin.right.AdminRight
import loi.cp.i18n.Translatable
import loi.cp.user.UserComponent
import scalaz.\/

import java.util.Date

@Service
@Controller(value = "passwords", root = true)
@RequestMapping(path = "passwords")
trait PasswordRootApi extends ApiRootComponent:

  @Secured(allowAnonymous = true)
  @RequestMapping(path = "settings", method = Method.GET)
  def settings: PasswordSettingsSummary

  @Secured(Array(classOf[AdminRight]))
  @RequestMapping(path = "settingsAdmin", method = Method.GET)
  def settingsAdmin: PasswordSettings

  @Secured(Array(classOf[AdminRight]))
  @RequestMapping(path = "settings", method = Method.POST)
  @Deprecated // use the configuration root now.
  def settings(@RequestBody `new`: JsonNode): Translatable.Any \/ Unit

  @RequestMapping(path = "change", method = Method.POST)
  def changePassword(
    @QueryParam("currentPassword") currentPassword: Password,
    @QueryParam("password") password: Password
  ): PasswordError \/ Unit

  @Secured(allowAnonymous = true)
  @RequestMapping(path = "validate", method = Method.POST, mode = Mode.READ_ONLY)
  def validatePassword(@QueryParam("password") password: Password): PasswordError \/ Unit

  // Helper methods to work with Validation from Java.
  def getPasswordErrors(user: UserComponent, password: String): java.util.List[String]
  def recordPasswordChange(user: UserComponent, password: Password, changed: Date): Unit
end PasswordRootApi

case class PasswordRecoverySettings(
  @JsonProperty enabled: Boolean,
  @JsonProperty adminAllowed: Boolean,
  @JsonProperty requireExistingPassword: Boolean
)

case class AccountRequestSettings(
  @JsonProperty status: String,
  @JsonProperty usernameIsEmailAddress: Boolean,
  @JsonProperty notifyUsers: String
):
  def statusEnum = AccountRequestRootComponent.Status.valueOf(status)

case class SelfRegistrationSettings(@JsonProperty enabled: Boolean)

case class PasswordSettings(
  @JsonProperty policy: PasswordPolicy,
  @JsonProperty recovery: PasswordRecoverySettings,
  @JsonProperty accountRequests: AccountRequestSettings,
  @JsonProperty selfRegistration: SelfRegistrationSettings
)

case class PasswordSettingsSummary(
  recovery: Boolean,
  accountRequests: Boolean,
  validation: Seq[String]
)

sealed trait PasswordReason extends EnumEntry

object PasswordReason extends Enum[PasswordReason]:
  val values = findValues

  case object NotFound               extends PasswordReason
  case object Ambiguous              extends PasswordReason
  case object Pending                extends PasswordReason
  case object DirectAccessProhibited extends PasswordReason
  case object EmailError             extends PasswordReason
  case object InvalidToken           extends PasswordReason
  case object IncorrectPassword      extends PasswordReason
  case object InvalidPassword        extends PasswordReason
  case object NotEnabled             extends PasswordReason
  case object AdminDisallowed        extends PasswordReason
  case object BadRequest             extends PasswordReason
  case object Unavailable            extends PasswordReason
end PasswordReason

case class PasswordError(reason: PasswordReason, messages: Seq[String] = Seq.empty)
