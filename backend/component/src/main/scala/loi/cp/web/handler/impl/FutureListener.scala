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

package loi.cp.web.handler.impl

import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.{AsyncEvent, AsyncListener}
import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.component.ComponentEnvironment
import com.learningobjects.cpxp.component.web.{HttpResponseException, WebRequest}
import com.learningobjects.cpxp.scala.util.Stopwatch
import com.learningobjects.cpxp.util.TomcatUtils
import de.tomcat.juli.LogMeta

import scala.util.Failure

/** Listener that watches the progress of a future SRS response handler and handles timeouts.
  * @param request
  *   the web request
  */
class FutureListener(env: ComponentEnvironment, request: WebRequest, logMeta: LogMeta) extends AsyncListener:
  import FutureListener.*

  private val stopWatch  = new Stopwatch
  private val requestUri = request.getRawRequest.getRequestURI
  private var timedOut   = false

  def isTimedOut = timedOut

  override def onStartAsync(event: AsyncEvent): Unit = logMeta {
    logger.info(s"Starting async for $requestUri.")
  }

  override def onComplete(event: AsyncEvent): Unit = logMeta {
    val facade = TomcatUtils.getResponseFacade(event.getSuppliedResponse)
    logger.info(
      s"Future response for $requestUri completed in ${stopWatch.elapsed.toMillis} ms, ${facade.getContentWritten} bytes, status code ${facade.getStatus}."
    )
  }

  override def onError(event: AsyncEvent): Unit = logMeta {
    logger.warn(event.getThrowable)(s"Future response for $requestUri failed")
  }

  override def onTimeout(event: AsyncEvent): Unit = logMeta {
    logger.info(s"Future response for $requestUri timed out.")
    timedOut = true
    FutureMethodHandler.writeResponse(RequestTimedOut)(env, event.getAsyncContext, request)
  }
end FutureListener

object FutureListener:
  private val logger = org.log4s.getLogger

  private val RequestTimedOut =
    new Failure[Nothing](new FutureTimeoutException)

class FutureTimeoutException extends HttpResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Timeout"):
  @JsonProperty def getStatus: String = "timeout"
