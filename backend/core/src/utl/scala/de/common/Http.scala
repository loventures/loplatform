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

import cats.Applicative
import cats.effect.{Async, Resource}
import org.http4s.*
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client
import org.http4s.headers.Authorization
import org.typelevel.ci.CIString

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*

final class Http[F[_]](
  val user: String,
  val pass: String,
  val jiraUri: Uri,
  val client: Client[F],
  val ioPool: ExecutionContext
):
  private val cred                        = BasicCredentials(user, pass)
  private val atlassianTokenNoCheckHeader = Header.Raw(CIString("X-Atlassian-Token"), "no-check")

  def get(uri: Uri, creds: BasicCredentials = cred): Request[F] =
    Request[F](Method.GET, uri, headers = Headers(auth(creds)))

  def post(uri: Uri, creds: BasicCredentials = cred): Request[F] =
    Request(Method.POST, uri, headers = Headers(auth(creds), atlassianTokenNoCheckHeader))

  def put(uri: Uri, creds: BasicCredentials = cred): Request[F] =
    Request(Method.PUT, uri, headers = Headers(auth(creds), atlassianTokenNoCheckHeader))

  def delete(uri: Uri, creds: BasicCredentials = cred): Request[F] =
    Request(Method.DELETE, uri, headers = Headers(auth(creds), atlassianTokenNoCheckHeader))

  private def auth(creds: BasicCredentials): Authorization =
    Authorization(Credentials.Token(AuthScheme.Basic, creds.token))
end Http

// shamelessly cribbed from WeakHttps
object Http:

  def apply[F[_]: Applicative](client: Resource[F, Client[F]], ioPool: ExecutionContext): Resource[F, Http[F]] =
    client.map(cl => new Http[F](null, null, null, cl, ioPool))

  def apply[F[_]: Applicative](
    user: String,
    pass: String,
    jiraUri: Uri,
    client: Resource[F, Client[F]],
    ioPool: ExecutionContext
  ): Resource[F, Http[F]] =
    client.map(cl => new Http[F](user, pass, jiraUri, cl, ioPool))

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

  def config[F[_]: Async]: BlazeClientBuilder[F] = BlazeClientBuilder[F]
    .withExecutionContext(ExecutionContext.global)
    .withSslContext(sslContext)
    .withRequestTimeout(1.minute)
end Http
