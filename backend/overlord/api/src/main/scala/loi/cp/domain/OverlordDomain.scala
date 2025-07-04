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

package loi.cp.domain

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.{ComponentDecorator, RestfulComponent}
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.domain.DomainConstants.*
import com.learningobjects.cpxp.service.domain.{DomainState, SecurityLevel}
import com.learningobjects.de.web.Queryable.Trait
import com.learningobjects.de.web.{Queryable, QueryableProperties}

/** A domain for overlords. */
@Schema("overlordDomain")
@QueryableProperties(
  Array(
    new Queryable(
      name = "meta",
      handler = classOf[OverlordDomainMetaHandler],
      traits = Array(Queryable.Trait.NOT_SORTABLE)
    )
  )
)
trait OverlordDomain extends RestfulComponent[OverlordDomain] with ComponentDecorator:
  @JsonProperty
  @Queryable(dataType = DATA_TYPE_DOMAIN_ID, traits = Array(Trait.CASE_INSENSITIVE))
  def getDomainId: String

  @JsonProperty
  @Queryable(dataType = DATA_TYPE_NAME, traits = Array(Trait.CASE_INSENSITIVE))
  def getName: String

  @JsonProperty
  @Queryable(dataType = DATA_TYPE_DOMAIN_SHORT_NAME, traits = Array(Trait.CASE_INSENSITIVE))
  def getShortName: String

  @JsonProperty
  @Queryable(dataType = DATA_TYPE_DOMAIN_HOST_NAME, traits = Array(Trait.CASE_INSENSITIVE))
  def getPrimaryHostName: String

  @JsonProperty
  @Queryable(dataType = DataTypes.DATA_TYPE_HOST_NAME, traits = Array(Trait.CASE_INSENSITIVE, Trait.NOT_SORTABLE))
  def getHostNames: Seq[String]

  @JsonProperty
  @Queryable(dataType = DATA_TYPE_LOCALE)
  def getLocale: String

  @JsonProperty
  @Queryable(dataType = DATA_TYPE_DOMAIN_TIME_ZONE)
  def getTimeZone: String

  @JsonProperty
  @Queryable(dataType = DATA_TYPE_SECURITY_LEVEL)
  def getSecurityLevel: SecurityLevel

  @JsonProperty
  @Queryable(dataType = DATA_TYPE_DOMAIN_STATE)
  def getState: DomainState
end OverlordDomain
