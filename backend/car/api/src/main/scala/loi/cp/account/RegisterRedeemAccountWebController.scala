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

package loi.cp.account

import com.learningobjects.cpxp.component.annotation.{Controller, QueryParam, RequestBody, RequestMapping}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.de.authorization.Secured
import jakarta.servlet.AsyncContext
import loi.cp.user.UserComponent
import loi.cp.web.challenge.ChallengeGuard
import scalaz.\/

import scala.concurrent.Future

@Controller(value = "registerRedeemAccounts", root = true)
trait RegisterRedeemAccountWebController extends ApiRootComponent:
  @Secured(guard = Array(classOf[ChallengeGuard]), allowAnonymous = true)
  @RequestMapping(path = "accounts/registerRedeem", method = Method.POST)
  def registerRedeem(
    @RequestBody user: UserComponent,
    @QueryParam accessCode: String,
    @QueryParam accessCodeSchema: Option[String],
    ac: AsyncContext
  ): Future[RegisterRedeemFailure \/ Long]

  @Secured(allowAnonymous = true)
  @RequestMapping(path = "accounts/selfRegistration", method = Method.GET)
  def selfRegistration: SelfRegistrationStatus
end RegisterRedeemAccountWebController

final case class SelfRegistrationStatus(enabled: Boolean)

final case class RegisterRedeemFailure(
  reason: RegisterRedeemReason,
  messages: Seq[String] = Nil
)

sealed trait RegisterRedeemReason extends enumeratum.EnumEntry

object RegisterRedeemReason extends enumeratum.Enum[RegisterRedeemReason]:
  override def values = findValues

  case object NotEnabled        extends RegisterRedeemReason
  case object Unavailable       extends RegisterRedeemReason
  case object DuplicateUser     extends RegisterRedeemReason
  case object InvalidAccessCode extends RegisterRedeemReason
  case object InvalidPassword   extends RegisterRedeemReason
  case object ValidationError   extends RegisterRedeemReason
