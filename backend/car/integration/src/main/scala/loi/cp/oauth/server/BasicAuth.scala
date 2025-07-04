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

import com.learningobjects.cpxp.scala.cpxp.Service.*
import jakarta.servlet.http.HttpServletRequest
import loi.cp.apikey.ApiKeySystem
import loi.cp.integration.IntegrationService
import loi.cp.oauth.server.AuthErrors.*
import org.apache.commons.codec.binary.Base64
import scalaz.*
import scalaz.syntax.either.*
import scalaz.syntax.std.option.*

import java.nio.charset.StandardCharsets.UTF_8
import scala.util.Try

object BasicAuth:
  def validateAuthorization(request: HttpServletRequest): AuthError \/ ApiKeySystem =
    request.getHeader("Authorization") match
      case BasicAuthorization(username, password) =>
        Option(
          service[IntegrationService].getApiKeyByIdAndSecret(username, password, request.getRemoteAddr)
        ) \/> AccessDenied

      case _ =>
        NoAuth.left[ApiKeySystem]
end BasicAuth

object BasicAuthorization:
  def unapplySeq(h: String): Option[Seq[String]] = h match
    case BasicB64(b64) if Base64.isBase64(b64) =>
      Try(
        new String(Base64.decodeBase64(b64), UTF_8) match
          case UnPw(un, pw) => Some(List(un, pw))
          case _            => None
      ).toOption.flatten

    case _ => None

  private val BasicB64 = """^Basic\s+(\S+)$""".r
  private val UnPw     = """^([^:]*):(.*)$""".r
end BasicAuthorization
