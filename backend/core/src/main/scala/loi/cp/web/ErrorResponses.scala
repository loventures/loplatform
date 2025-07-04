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

package loi.cp.web

import com.learningobjects.cpxp.component.web.ErrorResponse
import com.learningobjects.cpxp.component.web.exception.InvalidRequestException
import com.learningobjects.cpxp.service.exception.{AccessForbiddenException, ResourceNotFoundException}
import org.log4s.Logger
import scalaz.\/
import scaloi.syntax.TryOps.*

import scala.util.Try

/** Helpers for working with error responses. */
object ErrorResponses:

  /** Enhancements on [[Try]]. */
  implicit class TryResponseOps[A](private val self: Try[A]) extends AnyVal:

    /** Convert this try into a disjunction with an [[ErrorResponse]] on the left. Common rest exceptions are handled
      * appropriately.
      */
    def orErrorResponse(implicit log: Logger): ErrorResponse \/ A = self \/> errorResponse(log)

    private def errorResponse(log: Logger)(e: Throwable): ErrorResponse = e match
      case rnfe: ResourceNotFoundException =>
        log.debug(rnfe)("Resource not found")
        ErrorResponse.notFound(rnfe.getMessage)

      case afe: AccessForbiddenException =>
        log.debug(afe)("Access forbidden")
        ErrorResponse.notFound(afe.getMessage)

      case ire: InvalidRequestException =>
        log.debug(ire)("Access forbidden")
        ErrorResponse.notFound(ire.getMessage)

      case o =>
        log.warn(e)("Internal error")
        ErrorResponse.serverError(o.getMessage)
  end TryResponseOps
end ErrorResponses
