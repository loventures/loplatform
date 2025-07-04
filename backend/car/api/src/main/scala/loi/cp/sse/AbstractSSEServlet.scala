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

package loi.cp.sse

import org.apache.pekko.actor.{ActorPath, ActorSystem, Props}
import com.learningobjects.cpxp.async.{MessageReader, MessageReaderSupervisor}
import com.learningobjects.cpxp.component.ComponentImplementation
import com.learningobjects.cpxp.component.web.{AsyncEventBinding, AsyncEventComponent}
import com.learningobjects.cpxp.scala.actor.CpxpActorSystem
import com.learningobjects.cpxp.util.{GuidUtil, MimeUtils}
import jakarta.servlet.AsyncEvent
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import scalaz.syntax.std.option.*
import scaloi.syntax.StringOps.*

import java.util.logging.Logger
import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

/** Abstract implementation for components wishing to implement a comet servlet for subscribing a request to Server-Sent
  * Events.
  *
  * AbstractCometComponent creates a MessageReader actor using channels parsed from the comet event the the protected
  * channels
  */
abstract class AbstractSSEServlet[S <: AbstractSSEServlet[S]](implicit
  ct: ClassTag[S],
  ec: ExecutionContext
) extends AsyncEventComponent
    with SSEServletComponent
    with ComponentImplementation:
  servlet: S =>

  implicit val actorSystem: ActorSystem = CpxpActorSystem.system

  def logger: Logger

  override def onStartAsync(event: AsyncEvent): Unit =
    logger.info(s"On start async $eventReaderPath")

  override def onComplete(event: AsyncEvent): Unit =
    logger.info(s"On complete async $eventReaderPath")
    end()

  override def onTimeout(event: AsyncEvent): Unit =
    logger.info(s"On timeout async $eventReaderPath")
    end()

  override def onError(event: AsyncEvent): Unit =
    logger.info(s"On error async $eventReaderPath")
    end()

  override def start(request: HttpServletRequest, response: HttpServletResponse): Unit =
    logger.info(s"Creating new MessageReader named $eventReaderPath")
    val context = request.startAsync()
    context.setTimeout(0)
    context.addListener(this)

    val replay = request.getHeader("X-REPLAY-EVENT").toBoolean_? | true
    val lastId = request.getHeader("Last-Event-ID").toLong_?

    response.setStatus(HttpServletResponse.SC_OK)
    response.setContentType(MimeUtils.MIME_TYPE_TEXT_EVENT_STREAM + MimeUtils.CHARSET_SUFFIX_UTF_8)
    response.addHeader("Access-Control-Allow-Origin", "*")
    response.flushBuffer()

    val readerProps = Props(
      new MessageReader(replay, lastId, response.getWriter, routers, channels(request), primaryChannel, context)
    )
    MessageReaderSupervisor.localActor ! MessageReaderSupervisor.Create(readerProps, eventReaderPath)
  end start

  private def end(): Unit =
    logger.info(s"Shutting down reader: $eventReaderPath")
    MessageReaderSupervisor.localActor ! MessageReaderSupervisor.Stop(eventReaderPath)

  private val eventReaderPath =
    s"messageReader_${GuidUtil.longGuid}"

  override def routers: Iterable[ActorPath]

  override def primaryChannel: String = "event" // TODO: Stub

  override def channels(request: HttpServletRequest): List[String] =
    def fromPath: List[String] =
      val servletPath   = request.getServletPath
      val apiPath       =
        ct.runtimeClass.getAnnotation(classOf[AsyncEventBinding]).path()
      val remainingPath = request.getRequestURI.replace(servletPath + apiPath, "") // TODO: Make less fragile with Regex
      List(remainingPath)
    def fromQueryParams        = Option(request.getParameterValues("channel")) match
      case Some(array) => array.toList
      case None        => List.empty

    fromPath ++ fromQueryParams
  end channels
end AbstractSSEServlet
