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

package com.learningobjects.cpxp.service.exception

import scala.util.{Failure, Success, Try}

/** Utilities for converting scala objects to REST exceptions.
  */
object RestExceptionOps:
  implicit class OptionExceptionOps[T](t: Option[T]):

    /** Returns either the given element as {{Success}} or a {{Failure}} of a {{ResourceNotFoundException}} if a
      * {{None}}.
      *
      * @param msg
      *   the message for the failure if {{None}}
      * @return
      *   a {{Try}} of the given {{Option}}
      */
    def getOrNotFound(msg: String): Try[T] =
      t.map(Success(_)).getOrElse(Failure(new ResourceNotFoundException(msg)))
  end OptionExceptionOps
end RestExceptionOps
