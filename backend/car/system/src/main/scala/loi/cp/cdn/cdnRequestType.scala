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

/** Value type that describes the different support CDN request types.
  */
sealed trait CdnRequestType

/** A CDN request that should result in a browser redirect.
  * @param host
  *   the effective host
  * @param uri
  *   the request URI
  */
case class BrowserRedirect(host: String, uri: String) extends CdnRequestType

/** A CDN request that should result in execution in an effective request on behalf of the CDN.
  * @param host
  *   the effective host
  * @param uri
  *   the request URI
  * @param pathInfo
  *   the path info
  */
case class EffectiveRequest(host: String, uri: String, pathInfo: String) extends CdnRequestType
