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

import com.learningobjects.cpxp.component.annotation.{Component, Configuration}
import loi.cp.integration.AbstractSystem
import loi.cp.oauth.client.OAuthClientCredentialsSystem.ClientCredentialsConfig

@Component(name = "OAuth Client Credentials Connector")
class OAuthClientCredentialsSystemImpl
    extends AbstractSystem[OAuthClientCredentialsSystem]
    with OAuthClientCredentialsSystem:

  override def update(system: OAuthClientCredentialsSystem): OAuthClientCredentialsSystem =
    val newConfig = ClientCredentialsConfig(system)
    setConfig(newConfig)
    super.update(system)

  private def setConfig(config: ClientCredentialsConfig): Unit = _self.setJsonConfig(config)

  private def getConfig: ClientCredentialsConfig = _self.getJsonConfig(classOf[ClientCredentialsConfig])

  @Configuration(label = "$$field_token_request_url=Token Request URL", order = 10)
  override def getTokenRequestUrl: String = getConfig.tokenRequestUrl

  @Configuration(label = "$$field_client_id=Client ID", order = 12)
  override def getClientId: String = getConfig.clientId

  @Configuration(label = "$$field_client_secret=Client Secret", `type` = "Password", order = 13)
  override def getClientSecret: String = getConfig.clientSecret
end OAuthClientCredentialsSystemImpl
