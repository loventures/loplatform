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

package de.common

import cats.effect.Async
import cats.syntax.all.*
import io.circe.generic.auto.*
import org.http4s.circe.*
import org.http4s.headers.Authorization
import org.http4s.{AuthScheme, Credentials, Headers, Method, Request, Uri}
import scaloi.syntax.option.*

object `Aha!`:
  private final val logger = org.log4s.getLogger

  final val TicketRe = "(OS|WORK)-[0-9]{1,6}(?:-[0-9]{1,3})?".r

  final val RequirementRe = "(OS|WORK)-[0-9]{1,6}-[0-9]{1,3}".r

  def getTicket[F[_]: Async](key: String): Klient[F, Option[Ticket]] =
    for
      http       <- Klient.ask[F]
      uri         = ticketUri(key)
      apiKey     <- getenv[Klient[F, *]](AhaApiKeyEnv)
      _          <- Klient.delay(logger info s"GET $uri")
      featureOpt <- Klient.liftF(Jira.expect404(getTicket[F](http, uri, apiKey), uri))
      feature     = featureOpt.flatten
      _          <- Klient.delay(logger info s"$uri: 200 ${feature.map(_.reference_num)}")
    yield feature

  def getTicket[F[_]: Async](http: Http[F], ticketUri: Uri, apiKey: String): F[Option[Ticket]] =
    http.client
      .expect[AhaResponse](get[F](ticketUri, apiKey))(using jsonOf[F, AhaResponse])
      .map(r => r.feature || r.requirement)

  def get[F[_]](uri: Uri, apiKey: String): Request[F] =
    Request[F](Method.GET, uri, headers = Headers(auth(apiKey)))

  def url(key: String): String =
    ahaUri.withPath(Uri.Path.unsafeFromString(s"/${keyType(key)}/$key")).toString

  private def auth(creds: String): Authorization =
    Authorization(Credentials.Token(AuthScheme.Bearer, creds))

  private def ticketUri(key: String): Uri =
    ahaUri.withPath(Uri.Path.unsafeFromString(s"/api/v1/${keyType(key)}/$key"))

  private def keyType(key: String): String =
    if RequirementRe.matches(key) then "requirements" else "features"

  private final val ahaUri: Uri = Uri.unsafeFromString("https://domain.aha.io/")

  final case class AhaResponse(feature: Option[Ticket], requirement: Option[Ticket])

  // both Feature and Requirement look like this...
  final case class Ticket(
    id: String,            // "1023123123"
    name: String,          // "Bob's your uncle"
    reference_num: String, // "OS-123"
  )

  private final val AhaApiKeyEnv = "AHA_API_KEY"
end `Aha!`
