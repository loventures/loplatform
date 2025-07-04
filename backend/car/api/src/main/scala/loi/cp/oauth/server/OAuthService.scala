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

package loi.cp.oauth.server

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.web.HttpResponse
import jakarta.servlet.http.HttpServletRequest
import loi.cp.user.UserComponent

@Service
trait OAuthService:
  // Process any bearer authorization in the HTTP request. If valid
  // credentials are presented then the corresponding user is logged
  // in. If invalid credentials are presented then returns failure response.
  // This API is a bit questionable.
  def bearerAuthorize(request: HttpServletRequest): Option[HttpResponse]

  /** Validates a request for a token and returns the user which the token corresponds to Returns None if the request's
    * token cannot be validated
    * @return
    */
  def getUserForRequestWithOauthToken(request: HttpServletRequest): Option[UserComponent]
end OAuthService
