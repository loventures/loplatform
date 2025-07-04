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

package loi.cp.oauth.client

import org.apache.pekko.util.Timeout
import com.fasterxml.jackson.databind.ObjectMapper
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.actor.TaggedActors.*
import com.learningobjects.cpxp.util.HttpUtils
import loi.cp.integration.ConnectorRootComponent
import loi.cp.oauth.TokenResponse
import loi.cp.oauth.server.TokenActor.{AddOrUpdateToken, GetToken}
import loi.cp.oauth.server.{TokenActor, TokenRequestResponse, TokenState}
import org.apache.http.HttpHeaders
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.URIBuilder
import org.apache.http.util.EntityUtils
import scalaz.syntax.either.*
import scalaz.{-\/, \/, \/-}

import java.util.Base64
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.*

@Service
class OAuthClientServiceImpl(
  val mapper: ObjectMapper,
  val connectors: ConnectorRootComponent,
) extends OAuthClientService:
  implicit val timeout: Timeout = Timeout(3.seconds)

  @Override
  def getToken(connector: OAuthClientCredentialsSystem): Future[String \/ TokenResponse] =
    val tokenActorRef = TokenActor.clusterActor
    for futureToken <- tokenActorRef.askFor[TokenRequestResponse](GetToken(connector.getId))
    yield futureToken.token match
      case Some(ts) if !ts.hasExpired => ts.token.right
      case _                          => fetchNewCachedToken(tokenActorRef, connector)

  private def fetchNewCachedToken(
    tokenActorRef: TokenActor.Ref,
    connector: OAuthClientCredentialsSystem
  ): String \/ TokenResponse =
    fetchToken(connector) match
      case \/-(t)     =>
        tokenActorRef ! AddOrUpdateToken(connector.getId, t)
        t.token.right
      case -\/(error) => error.left

  private def fetchToken(connector: OAuthClientCredentialsSystem): String \/ TokenState =
    val uri = new URIBuilder(connector.getTokenRequestUrl)
      .addParameter("grant_type", "client_credentials")
      .build

    val apiCredentials =
      Base64.getEncoder.encodeToString(s"${connector.getClientId}:${connector.getClientSecret}".getBytes("utf-8"))

    val request = new HttpPost(uri)
    request.addHeader(HttpHeaders.AUTHORIZATION, s"Basic $apiCredentials")

    \/.attempt(HttpUtils.getHttpClient.execute(request))(_.getMessage)
      .flatMap { response =>
        response.getStatusLine.getStatusCode match
          case 401 => "Unauthorized request against OAuth 2.0 token API".left
          case 200 =>
            TokenState(
              mapper.readValue(EntityUtils.toString(response.getEntity, "UTF-8"), classOf[TokenResponse])
            ).right
          case _   => "Unknown HTTP error".left
      }
  end fetchToken
end OAuthClientServiceImpl
