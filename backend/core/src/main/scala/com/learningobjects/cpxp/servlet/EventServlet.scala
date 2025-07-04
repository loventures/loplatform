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

package com.learningobjects.cpxp.servlet

import com.learningobjects.cpxp.component.ComponentSupport
import com.learningobjects.cpxp.component.web.AsyncEventComponent
import com.learningobjects.cpxp.scala.util.Misc.*
import jakarta.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import loi.apm.Apm

class EventServlet extends HttpServlet:
  override def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit =
    if !doAsync(req, resp) then resp.sendError(HttpServletResponse.SC_NOT_FOUND)

  def doAsync(req: HttpServletRequest, resp: HttpServletResponse): Boolean =
    val requestPath = req.getRequestURI.replace(req.getServletPath, "")
    val component   = ComponentSupport.lookup(classOf[AsyncEventComponent], requestPath)
    (component ne null) && truly {
      Apm.ignoreTransaction()
      component.start(req, resp)
    }
end EventServlet
