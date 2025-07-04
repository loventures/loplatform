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

package de.notify

import cats.Applicative
import cats.effect.{Async, Resource}
import org.http4s.*
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client
import org.http4s.headers.Authorization

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*

final class SlackHttp[F[_]](val slackuri: Uri, val slackToken: String, val client: Client[F]):

  private val auth = Authorization(
    Credentials.Token(AuthScheme.Bearer, slackToken)
  )

  def get(uri: Uri): Request[F] =
    Request(Method.GET, uri, headers = Headers(auth))

  def post(uri: Uri): Request[F] =
    Request(Method.POST, uri, headers = Headers(auth))
end SlackHttp

object SlackHttp:

  def apply[F[_]: Applicative](
    slackuri: Uri,
    slackToken: String,
    client: Resource[F, Client[F]]
  ): Resource[F, SlackHttp[F]] =
    client.map(cl => new SlackHttp[F](slackuri, slackToken, cl))

  private val trustAllCerts = Array[TrustManager](new X509TrustManager():
    override def getAcceptedIssuers: Array[X509Certificate]                                = null
    override def checkClientTrusted(certs: Array[X509Certificate], authType: String): Unit = ()
    override def checkServerTrusted(certs: Array[X509Certificate], authType: String): Unit = ())

  private val hv = new HostnameVerifier():
    override def verify(urlHostName: String, session: SSLSession): Boolean = true

  private val sc = SSLContext.getInstance("SSL")

  HttpsURLConnection.setDefaultHostnameVerifier(hv)
  sc.init(null, trustAllCerts, new SecureRandom)
  HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory)

  private def sslContext = sc

  def defaultClient[F[_]: Async]: Resource[F, Client[F]] =
    BlazeClientBuilder[F]
      .withExecutionContext(ExecutionContext.global)
      .withSslContext(sslContext)
      .withRequestTimeout(1.minute)
      .resource
end SlackHttp
