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

package com.learningobjects.cpxp.component.web

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

trait HttpResponse:
  val statusCode: Int // not clear why this is the definition of a HTTP response
  val headers: Map[String, String]

/** I am so awful. */
@JsonIgnoreProperties(
  Array("statusCode", "headers", "stackTrace", "message", "cause", "localizedMessage", "suppressed")
)
class HttpResponseException(val statusCode: Int, error: String) extends RuntimeException(error) with HttpResponse:
  override val headers: Map[String, String] = Map.empty
