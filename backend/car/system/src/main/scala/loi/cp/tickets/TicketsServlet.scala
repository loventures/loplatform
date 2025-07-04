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

package loi.cp.tickets

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentEnvironment, ComponentImplementation, ComponentInstance}
import scalaz.syntax.either.*

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import scala.compat.java8.OptionConverters.*
import scala.compat.java8.StreamConverters.*
import scala.jdk.CollectionConverters.*
import scala.util.Try
@Component
@ServletBinding(path = TicketsServlet.Path)
class TicketsServlet(val componentInstance: ComponentInstance)(
  env: ComponentEnvironment
) extends ServletComponent
    with ServletDispatcher
    with ComponentImplementation:
  import ServletDispatcher.*
  import TicketsServlet.*

  override protected def handler: RequestHandler = {
    case RequestMatcher(Method.GET, TicketsServlet.Path, _, _) =>
      HtmlResponse(this, "tickets.html").right
    case RequestMatcher(Method.GET, _, _, _)                   =>
      ErrorResponse.notFound.left
    case _                                                     =>
      ErrorResponse.methodNotAllowed.left
  }

  def tickets: Seq[TicketRow] =
    (for
      arch             <- env.getArchives.asScala
      path             <- Try(arch.getSource.getResource("tickets.ug").asScala).toOption.flatten.toSeq
      if Files.exists(path)
      line             <- try Files.lines(path, StandardCharsets.UTF_8).toScala[Iterable].map(_.trim)
                          catch
                            case ioe: IOException =>
                              logger.warn(ioe)(s"While reading from: ${arch.getIdentifier}")
                              Nil
      if line.nonEmpty
      Array(ticket, ts) = line.split('=')
    yield ticket -> ts.toLong)
      .groupBy(_._1)
      .map { case (id, tss) => id -> tss.map(_._2).max }
      .toSeq
      .sortBy(_._2)(using Ordering[Long].reverse)
      .map { case (id, ts) => TicketRow(id, ts) }

  val prefix = "https://tickets.example.org/"
end TicketsServlet

object TicketsServlet:
  final val logger = org.log4s.getLogger
  final val Path   = "/sys/tickets"

  case class TicketRow(id: String, timestamp: Long)
