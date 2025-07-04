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

package com.learningobjects.cpxp.component.acl

import com.learningobjects.cpxp.component.annotation.Session
import com.learningobjects.cpxp.service.user.{UserDTO, UserType, UserWebService}
import com.learningobjects.cpxp.util.SessionUtils.SESSION_ATTRIBUTE_SUDOER

import java.lang as jl

/** Require that the current user is an (possibly sudoed) overlord.
  */
class OverlordEnforcer(
  @Session(name = SESSION_ATTRIBUTE_SUDOER)
  sudoers: String,
  user: UserDTO,
  uws: UserWebService,
) extends AccessEnforcer:
  def checkAccess(): Boolean =
    val sudoUsers =
      Option(sudoers) filterNot (_.isEmpty) map {
        _ split ':' map (_.trim.toLong: jl.Long) map uws.getUserDTO
      }
    (user :: sudoUsers.toList.flatten) exists {
      case u: UserDTO => u.userType == UserType.Overlord
      case null       => false
    }
end OverlordEnforcer
