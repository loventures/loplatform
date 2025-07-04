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

package com.learningobjects.cpxp.scala.environment

import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.user.UserDTO
import org.log4s.Logger

import java.util.Date

/** A thread-safe(?) initializer user identity
  */
trait UserEnvironment[V, R] extends Environment[V, R]:
  def user(input: V): UserDTO

  def logger: Logger

  abstract override def before(input: V): V =
    val superBefore = super.before(input)
    val u           = user(input)
    Current.setTime(new Date())
    Current.setUserDTO(u)
    logger info s"Setting Current User: ${u.getId}"
    superBefore
end UserEnvironment
