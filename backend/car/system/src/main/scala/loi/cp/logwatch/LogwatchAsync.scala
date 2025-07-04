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

package loi.cp.logwatch

import com.learningobjects.cpxp.BaseServiceMeta
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.{AbstractComponentServlet, ServletBinding}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.util.MimeUtils
import com.learningobjects.cpxp.util.logging.StandardLogFormatter
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import jakarta.servlet.{AsyncContext, AsyncEvent, AsyncListener}
import loi.apm.Apm
import loi.cp.overlord.EnforceOverlordAuth
import scalaz.std.string.*
import scalaz.syntax.std.boolean.*

import java.io.PrintWriter
import java.util.Date
import java.util.logging.{Handler, LogRecord, Logger}
import scala.concurrent.duration.*
import scala.util.Try

@Component
@EnforceOverlordAuth
@ServletBinding(path = "/sys/logwatch/stream")
class LogwatchAsync(val componentInstance: ComponentInstance)
    extends AbstractComponentServlet
    with ComponentImplementation:
  override def get(request: HttpServletRequest, response: HttpServletResponse): Unit =
    response.setContentType(MimeUtils.MIME_TYPE_TEXT_EVENT_STREAM)
    response.setCharacterEncoding("UTF-8")
    Apm.ignoreTransaction()
    val context = request.startAsync()
    context.addListener(new LogwatchAsyncListener(context))
end LogwatchAsync

object LogwatchAsyncListener:
  private final val logger     = org.log4s.getLogger
  private final val rootLogger = Logger.getLogger("")

class LogwatchAsyncListener(context: AsyncContext) extends AsyncListener:
  import LogwatchAsyncListener.*

  val response = context.getResponse.asInstanceOf[HttpServletResponse]
  val writer   = response.getWriter
  val st       =
    val serviceMeta = BaseServiceMeta.getServiceMeta
    val das         = serviceMeta.isDas ?? " (DAS)"
    s"/sys/logwatch cpxp-${serviceMeta.getBuild}/${serviceMeta.getNode}$das on ${new Date}"

  val handler = new LogwatchLogHandler(this, response, writer)

  context.setTimeout(10.minutes.toMillis)

  writer.println(s"data: $st")
  writer.println()
  response.flushBuffer()

  logger.info("Starting logwatch")

  rootLogger.addHandler(handler)

  override def onComplete(event: AsyncEvent): Unit =
    logger.info("Logwatch stopped.")
    stop()

  override def onError(event: AsyncEvent): Unit =
    logger.warn(event.getThrowable)("An Async Error occured.")
    stop()

  override def onStartAsync(event: AsyncEvent): Unit =
    event.getAsyncContext.addListener(this)

  override def onTimeout(event: AsyncEvent): Unit =
    logger.debug("Timeout")
    stop()

  def stop(): Unit =
    rootLogger.removeHandler(handler)
end LogwatchAsyncListener

class LogwatchLogHandler(listener: LogwatchAsyncListener, response: HttpServletResponse, writer: PrintWriter)
    extends Handler:
  private final val formatter = new StandardLogFormatter()

  override def close(): Unit = ()

  override def flush(): Unit = ()

  override def publish(record: LogRecord): Unit =
    Try {
      writer.println(formatter.format(record).replaceAll("(.+)", "data: $1"))
      writer.flush()
      response.flushBuffer()
    } getOrElse {
      listener.stop()
    }
end LogwatchLogHandler
