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

package loi.cp.cdn

import jakarta.servlet.http.HttpServletRequest

import scala.util.matching.Regex

/** Utility class for pattern matching against http request URIs. This matches against the request URI which is the
  * absolute local path of the request; e.g. /static/foo/bar.
  *
  * @param re
  *   the regex to apply to the request URI.
  */
class HttpServletRequestMatcher(re: Regex):

  /** Pattern match a request.
    *
    * @param request
    *   the request
    * @return
    *   the matching regex groups, if any
    */
  def unapplySeq(request: HttpServletRequest): Option[List[String]] =
    re.unapplySeq(request.getRequestURI)
end HttpServletRequestMatcher

object HttpServletRequestMatcher:

  /** Create a request matcher from the string form of a regex pattern.
    *
    * @param s
    *   the string pattern
    * @return
    *   the request matcher
    */
  def apply(s: String): HttpServletRequestMatcher =
    new HttpServletRequestMatcher(s.r)
end HttpServletRequestMatcher
