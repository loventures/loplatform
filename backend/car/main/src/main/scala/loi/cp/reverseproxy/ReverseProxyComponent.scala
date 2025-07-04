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

package loi.cp.reverseproxy

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.component.RestfulComponent
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.site.SiteConstants
import com.learningobjects.de.web.Queryable

/** A reverse proxy. */
@Schema("reverseProxy")
@ItemMapping(value = SiteConstants.ITEM_TYPE_SITE)
trait ReverseProxyComponent extends RestfulComponent[ReverseProxyComponent]:

  /** The name of this job. */
  @Queryable(dataType = DataTypes.DATA_TYPE_NAME, traits = Array(Queryable.Trait.CASE_INSENSITIVE))
  @JsonProperty
  def getName: String

  /** The local URL to which to bind. */
  @JsonProperty
  def getUrl: String

  /** The remote URL to proxy. */
  @JsonProperty
  def getRemoteUrl: String

  /** The cookies to proxy. */
  @JsonProperty
  def getCookieNames: List[String]

  /** The content rewrite rules. */
  @JsonProperty
  def getRewriteRules: List[RewriteRule]

  /** Whether this job is disabled. */
  @JsonProperty
  def isDisabled: Boolean
end ReverseProxyComponent

/** A rule for rewriting proxied content. */
case class RewriteRule(
  /** Regex defining which target URLs match this rewrite URL. */
  pathPattern: String,
  /** Body pattern to match. This is applied line-by-line. */
  bodyPattern: String,
  /** Replacement text. */
  replacementText: String
)
