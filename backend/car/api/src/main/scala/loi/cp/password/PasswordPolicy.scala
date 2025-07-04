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

package loi.cp.password

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

import java.lang as jl

object PasswordPolicy:
  val none: PasswordPolicy =
    PasswordPolicy(None, None, None, None, None, None, None)

  val standard: PasswordPolicy = PasswordPolicy(
    minSize = Some(8),
    alphaNumeric = Some(true),
    hasNonAlpha = Some(true),
    uniquePasswords = Some(8),
    failedAttempts = Some(8),
    attemptInterval = Some(300),
    expireDuration = Some(157788000)
  ) // 5 years, give or take
end PasswordPolicy

@JsonIgnoreProperties(ignoreUnknown = true)
case class PasswordPolicy(
  @JsonDeserialize(contentAs = classOf[jl.Integer])
  minSize: Option[Int],
  @JsonDeserialize(contentAs = classOf[jl.Boolean])
  alphaNumeric: Option[Boolean],
  @JsonDeserialize(contentAs = classOf[jl.Boolean])
  hasNonAlpha: Option[Boolean],
  @JsonDeserialize(contentAs = classOf[jl.Integer])
  uniquePasswords: Option[Int],
  @JsonDeserialize(contentAs = classOf[jl.Integer])
  failedAttempts: Option[Int],
  @JsonDeserialize(contentAs = classOf[jl.Integer])
  attemptInterval: Option[Int],
  @JsonDeserialize(contentAs = classOf[jl.Long])
  expireDuration: Option[Long]
)
