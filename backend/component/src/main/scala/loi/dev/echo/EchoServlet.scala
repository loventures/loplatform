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

package loi.dev.echo

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.*
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import java.nio.charset.StandardCharsets
import org.apache.commons.io.IOUtils
import scalaz.std.list.*
import scalaz.syntax.functor.*

import scala.jdk.CollectionConverters.*

/** This servlet echos any request to it to the log.
  */
@Component
@ServletBinding(path = "/sys/echo")
class EchoServlet extends AbstractComponentServlet:
  import EchoServlet.*

  override def service(request: HttpServletRequest, response: HttpServletResponse): WebResponse =
    val headers =
      request.getHeaderNames.asScala.toList.fproduct(request.getHeader)

    val msg =
      s"""Request from host: ${request.getRemoteHost} ${request.getRemoteAddr}
         |
         |Request headers:
         |${headers.map { case (name, value) => s"  $name: $value" }}
         |
         |Request body:
         |${IOUtils.toString(request.getInputStream, StandardCharsets.UTF_8.name)}
       """.stripMargin

    logger info msg

    NoContentResponse
  end service
end EchoServlet

object EchoServlet:
  private val logger = org.log4s.getLogger
