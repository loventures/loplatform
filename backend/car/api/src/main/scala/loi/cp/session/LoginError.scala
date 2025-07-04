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

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonProperty, JsonSubTypes, JsonTypeInfo}
import loi.cp.Widen
import loi.cp.password.Error

import scala.beans.BeanProperty

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME, // MINIMAL_CLASS has a leading period
  include = JsonTypeInfo.As.PROPERTY,
  property = "reason"
)
@JsonSubTypes(
  Array(
    new Type(name = "SystemError", value = classOf[SystemError]),
    new Type(name = "ServerError", value = classOf[ServerError]),
    new Type(name = "CookiesBlocked", value = classOf[CookiesBlocked]),
    new Type(name = "InvalidCredentials", value = classOf[InvalidCredentialsError]),
    new Type(name = "AccountLocked", value = classOf[AccountLockedError]),
    new Type(name = "AccountPending", value = classOf[AccountPendingError]),
    new Type(name = "AccountUnconfirmed", value = classOf[AccountUnconfirmedError]),
    new Type(name = "TooManyFailedLogins", value = classOf[TooManyFailedLoginsError]),
    new Type(name = "PasswordExpired", value = classOf[PasswordExpiredError])
  )
)
sealed trait LoginError extends Widen[LoginError]

case class SystemError(@BeanProperty @JsonProperty message: Error) extends LoginError

/** Error with remote authentication server. */
case class ServerError() extends LoginError

/** Cookies appear blocked. */
case class CookiesBlocked() extends LoginError

case class InvalidCredentialsError() extends LoginError

case class AccountLockedError() extends LoginError

case class AccountPendingError() extends LoginError

case class AccountUnconfirmedError() extends LoginError

case class TooManyFailedLoginsError(
  @BeanProperty @JsonProperty message: Error,
  @BeanProperty @JsonProperty interval: Int
) extends LoginError

case class PasswordExpiredError(@BeanProperty @JsonProperty token: String) extends LoginError
