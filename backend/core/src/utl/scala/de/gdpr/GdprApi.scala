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

package de.gdpr

import cats.effect.{Async, Sync}
import cats.syntax.all.*
import de.common.{Host, Http, Klient}
import io.circe.syntax.*
import org.http4s.Uri
import org.http4s.circe.*

object GdprApi:
  private final val logger = org.log4s.getLogger

  def lookup[F[_]: Async](emails: Emails, host: Host): Klient[F, Emails] =
    val uri = host.uri.withPath(Uri.Path.unsafeFromString("/sys/gdpr/lookup"))

    def doPost(http: Http[F]): F[Emails] =
      http.client.expect[Emails](http.post(uri, host.cred).withEntity(emails.asJson))(using jsonOf[F, Emails])

    for
      http   <- Klient.ask[F]
      emails <- Klient.liftF(logA(doPost(http))(s"performing lookup for $uri", _.mkString(", ")))
    yield emails
  end lookup

  def logA[F[_]: Sync, A](fa: F[A])(name: String, f: A => String): F[A] =
    for
      a <- fa.onError({ case th: Throwable =>
             Sync[F].delay(logger.warn(s"Error $name(${th.getMessage})")) *> Sync[F].raiseError(th)
           })
      _ <- Sync[F].delay(logger info s"$name: ${f(a)}")
    yield a

  def obfuscate[F[_]: Async](emails: Emails, host: Host): Klient[F, Emails] =
    val uri                              = host.uri.withPath(Uri.Path.unsafeFromString("/sys/gdpr/obfuscate"))
    def doPost(http: Http[F]): F[Emails] =
      http.client.expect[Emails](http.post(uri, host.cred).withEntity(emails.asJson))(using jsonOf[F, Emails])
    for
      http   <- Klient.ask[F]
      emails <- logA(Klient.liftF(doPost(http)))(s"performing process $uri", _.mkString(", "))
    yield emails

  type Emails = List[String]
end GdprApi
