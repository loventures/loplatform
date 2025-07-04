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

package loi.cp.overlord

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.WebRequest
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.exception.AccessForbiddenException
import com.learningobjects.cpxp.service.user.UserType
import com.learningobjects.de.authorization.{SecurityContext, SecurityGuard}

// TODO: See OverlordAuthEnforcer and consider digest auth. Fix the SecurityGuard API
// to return a WebResponse and not extend ComponentInterface.

@Component
class OverlordSecurityGuard(
  val componentInstance: ComponentInstance
) extends SecurityGuard
    with ComponentImplementation:
  def checkAccess(request: WebRequest, securityContext: SecurityContext): Unit =
    if !Option(Current.getUserDTO).exists(_.userType == UserType.Overlord) then
      throw new AccessForbiddenException("Overlord")
