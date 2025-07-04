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

package loi.cp.notification.test

import com.fasterxml.jackson.databind.ObjectMapper
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.Method.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.util.HttpServletRequestOps.*
import scalaz.syntax.either.*

import scala.jdk.CollectionConverters.*
import scala.collection.mutable

@Component(enabled = false)
@ServletBinding(path = TestNotificationReceiver.Path)
class TestNotificationReceiver(val componentInstance: ComponentInstance)(
  om: ObjectMapper
) extends ServletComponent
    with ServletDispatcher
    with ComponentImplementation:
  import ServletDispatcher.*
  import TestNotificationReceiver.*

  override protected def handler: RequestHandler = {
    case RequestMatcher(GET, _, _, _)     =>
      TextResponse.json(om `writeValueAsString` received).right
    case RequestMatcher(POST, pi, req, _) =>
      received += Received(
        pathInfo = pi.stripPrefix(Path),
        query = req.getQueryString,
        headers = req.getHeaderNames.asScala.map(name => name -> req.getHeader(name)).toList,
        body = req.body
      )
      NoResponse.right
    case RequestMatcher(DELETE, _, _, _)  =>
      received.clear()
      NoResponse.right
    case _                                => ErrorResponse.methodNotAllowed.left
  }
end TestNotificationReceiver

object TestNotificationReceiver:
  final val Path = "/notifyTest"

  case class Received(pathInfo: String, query: String, headers: List[(String, String)], body: String)

  private val received = mutable.Buffer.empty[Received]
