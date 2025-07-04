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

package loi.cp.limiter

import com.google.common.collect.ConcurrentHashMultiset
import com.learningobjects.cpxp.component.AbstractComponent
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.{FilterBinding, FilterComponent, FilterInvocation}
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}

object RequestLimitFilter:
  val getRequestSet        = ConcurrentHashMultiset.create[RequestPair]()
  // http://tools.ietf.org/html/rfc6585
  val SC_TOO_MANY_REQUESTS = 429

  case class RequestPair(sessionId: String, url: String)

@Component
@FilterBinding(priority = 0, system = true)
class RequestLimitFilter extends AbstractComponent with FilterComponent:
  import RequestLimitFilter.*

  override def filter(
    request: HttpServletRequest,
    response: HttpServletResponse,
    invocation: FilterInvocation
  ): Boolean =
    val requestPair =
      RequestPair(request.getRequestedSessionId, request.getRequestURI)

    def noSession               = requestPair.sessionId == null
    def tooManyRequests         = getRequestSet.add(requestPair, 1) > 4
    def handleTooManyRequests() = response.sendError(SC_TOO_MANY_REQUESTS)
    def finishRequest(): Unit   =
      getRequestSet.remove(requestPair, 1)
      ()

    if noSession then true
    else
      try
        if tooManyRequests then // If you already have three of these requests in flight, reject
          handleTooManyRequests()
        else invocation.proceed(request, response)
      finally
        finishRequest()
      false
  end filter
end RequestLimitFilter
