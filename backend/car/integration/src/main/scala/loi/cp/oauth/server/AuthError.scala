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

package loi.cp.oauth.server

case class AuthError(val error: Option[String])

object AuthErrors:
  val NoAuth = AuthError(None)

  val AccessDenied = AuthError(Some("access_denied"))

  val ServerError = AuthError(Some("server_error"))

  val InvalidGrant = AuthError(Some("invalid_grant"))

  val UnauthorizedClient = AuthError(Some("unauthorized_client"))
end AuthErrors
