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

import com.learningobjects.cpxp.BaseServiceMeta
import com.learningobjects.cpxp.scala.cpxp.Service.*
import com.learningobjects.cpxp.service.session.{SessionService, SessionSupport}
import com.learningobjects.cpxp.util.{HttpUtils, SessionUtils}
import jakarta.servlet.http.HttpServletRequest

import scala.language.implicitConversions

/** A thread-safe initializer SessionSupport Services.
  */
trait SessionEnvironment[V, R] extends Environment[V, R]:
  implicit def requestEvidence(input: V): HttpServletRequest

  abstract override def before(input: V): V =
    val superBefore                 = super.before(input)
    val request: HttpServletRequest = input
    val sessionId                   = HttpUtils.getCookieValue(request, SessionUtils.COOKIE_NAME_SESSION)

    if SessionSupport.isPersistentId(sessionId) then
      service[SessionService]
        .pingSession(sessionId, HttpUtils.getRemoteAddr(request, BaseServiceMeta.getServiceMeta))
    superBefore
end SessionEnvironment
