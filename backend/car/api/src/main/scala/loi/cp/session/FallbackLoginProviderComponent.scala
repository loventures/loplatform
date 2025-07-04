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

import com.learningobjects.cpxp.component.ComponentInterface
import com.learningobjects.cpxp.service.login.LoginComponent
import loi.cp.integration.PasswordLoginSystem

/** This component provides a fallback login system if none is configured in the domain. Primarily this supports
  * synthetic LDAP login in production overlord environments.
  */
trait FallbackLoginProviderComponent extends ComponentInterface:
  def fallbackLoginSystem: PasswordLoginSystem[?, ? <: LoginComponent]
