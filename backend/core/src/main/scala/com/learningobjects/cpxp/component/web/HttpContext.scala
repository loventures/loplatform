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

package com.learningobjects.cpxp.component.web

import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse, HttpSession}

/** A simple wrapper for the HTTP request/response context for use in request mappings.
  *
  * @param webRequest
  *   the web request
  * @param response
  *   the HTTP servlet response
  */
case class HttpContext(webRequest: WebRequest, response: HttpServletResponse):

  /** Get the raw HTTP request.
    *
    * @return
    *   the raw HTTP servlet request
    */
  def request: HttpServletRequest = webRequest.getRawRequest

  /** Get the underlying HTTP session.
    *
    * @return
    *   the HTTP session
    */
  def session: HttpSession = request.getSession
end HttpContext
