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

import com.learningobjects.cpxp.component.web.HttpContext
import com.learningobjects.cpxp.util.MimeUtils
import jakarta.servlet.http.HttpServletResponse

/** Helper for opening SSE event streams.
  */
object SseResponse:

  /** Open an SSE event stream. This puts the response into async mode with no timeout, sets the content type and
    * returns a print writer.
    *
    * @param http
    *   the http context
    * @return
    *   the SSE print writer
    */
  def openAsyncStream(http: HttpContext): AsyncContextPrintWriter =
    val context = http.request.startAsync
    context.setTimeout(0L)
    http.response.setStatus(HttpServletResponse.SC_OK)
    http.response.setContentType(MimeUtils.MIME_TYPE_TEXT_EVENT_STREAM + MimeUtils.CHARSET_SUFFIX_UTF_8)
    http.response
      .flushBuffer() // forcing a flush eliminates some spurious flushing errors in tomcat..
    new AsyncContextPrintWriter(http.response.getWriter, context)
end SseResponse
