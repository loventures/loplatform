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

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.component.annotation.Schema
import loi.cp.integration.SystemComponent

/** Configure a client credentials-based OAuth connector used for fetching tokens from remote authorization server.
  *
  * To perform OAuth 2-based client requests, see: [[OAuthClientService]]
  */
@Schema(value = "oAuthClientCredentialsConnector")
trait OAuthClientCredentialsSystem extends SystemComponent[OAuthClientCredentialsSystem]:

  @JsonProperty
  def getTokenRequestUrl: String

  @JsonProperty
  def getClientId: String

  @JsonProperty
  def getClientSecret: String
end OAuthClientCredentialsSystem

object OAuthClientCredentialsSystem:

  case class ClientCredentialsConfig(
    tokenRequestUrl: String,
    clientId: String,
    clientSecret: String
  )

  object ClientCredentialsConfig:
    def apply(system: OAuthClientCredentialsSystem): ClientCredentialsConfig =
      ClientCredentialsConfig(
        tokenRequestUrl = system.getTokenRequestUrl,
        clientId = system.getClientId,
        clientSecret = system.getClientSecret
      )
end OAuthClientCredentialsSystem
