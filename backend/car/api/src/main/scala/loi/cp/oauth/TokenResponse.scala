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

package loi.cp.oauth

import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonProperty}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

import java.lang as jl

/** Access token response, as defined by [[https://tools.ietf.org/html/rfc6750 RFC6750]].
  */
@JsonIgnoreProperties(ignoreUnknown = true)
case class TokenResponse(
  @JsonProperty("access_token") accessToken: String,
  @JsonProperty("token_type") tokenType: String,
  @JsonDeserialize(contentAs = classOf[jl.Integer])
  @JsonProperty("expires_in") expiresIn: Option[Int],
  @JsonProperty("refresh_token") refreshToken: Option[String]
)
