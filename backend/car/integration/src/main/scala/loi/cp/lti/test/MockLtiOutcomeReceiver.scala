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

package loi.cp.lti.test

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.{AbstractComponentServlet, ServletBinding}
import com.learningobjects.cpxp.scala.util.HttpServletRequestOps.*
import jakarta.servlet.http.HttpServletResponse.*
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}

import scala.collection.mutable

@Component(enabled = false)
@ServletBinding(path = "/test/lti/outcomes")
class MockLtiOutcomeReceiver extends AbstractComponentServlet:
  import MockLtiOutcomeReceiver.*

  override def get(request: HttpServletRequest, response: HttpServletResponse): Unit =
    request.getRequestURI match
      case ResourceId(id) =>
        outcomes get id.toLong match
          case Some(result) => response.getWriter.write(result)
          case None         => response.sendError(SC_NOT_FOUND)
      case _              => response.sendError(SC_BAD_REQUEST)

  override def post(request: HttpServletRequest, response: HttpServletResponse): Unit =
    request.getRequestURI match
      case ResourceId(id) =>
        outcomes.put(id.toLong, request.body)
        response.setStatus(SC_OK)
        response.setContentType("application/json")
        response.getWriter.write("""{"status":"accepted"}""") // We require a response body from the LTI post
      case _ => response.sendError(SC_BAD_REQUEST)

  override def delete(request: HttpServletRequest, response: HttpServletResponse): Unit =
    request.getRequestURI match
      case ResourceId(id) =>
        if outcomes contains id.toLong then response.setStatus(SC_NO_CONTENT)
        else response.sendError(SC_NOT_FOUND)
        outcomes.remove(id.toLong)
        ()
      case _              => response.sendError(SC_BAD_REQUEST)
end MockLtiOutcomeReceiver

object MockLtiOutcomeReceiver:
  private val outcomes = mutable.Map[Long, String]()

  private val ResourceId = s"/test/lti/outcomes/(\\d+)".r
