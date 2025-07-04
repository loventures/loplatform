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

package com.learningobjects.cpxp.filter
import com.learningobjects.cpxp.util.ThreadTerminator
import jakarta.servlet.FilterChain
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}

/* Times out read-only threads that take longer than the ELB timeout. */
class RequestTimeoutFilter extends AbstractFilter:
  override protected def filterImpl(
    httpRequest: HttpServletRequest,
    httpResponse: HttpServletResponse,
    chain: FilterChain
  ): Unit =
    if RequestTimeoutFilter.timeoutMethods contains httpRequest.getMethod then ThreadTerminator.register()
    else ThreadTerminator.unregister()

    try chain.doFilter(httpRequest, httpResponse)
    catch
      case _: ThreadTerminator.Terminated =>
        ThreadTerminator.unregister()
      case ex: Throwable                  =>
        ThreadTerminator.unregister()
        throw ex
  end filterImpl
end RequestTimeoutFilter

object RequestTimeoutFilter:
  val timeoutMethods = Set("GET", "HEAD", "OPTIONS")
