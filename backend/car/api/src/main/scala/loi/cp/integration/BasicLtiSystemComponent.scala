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

package loi.cp.integration

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.learningobjects.cpxp.component.annotation.{Configuration, Schema}

import java.lang as jl

@Schema("basicLtiConnector")
trait BasicLtiSystemComponent extends LtiSystemComponent[BasicLtiSystemComponent]:
  @JsonProperty
  @Configuration(label = "$$label_useExternalIdentifier=Use External Identifier", `type` = "Boolean", order = 20)
  def getUseExternalIdentifier: jl.Boolean
  def setUseExternalIdentifier(usernameParameter: jl.Boolean): Unit

  @JsonProperty
  @Configuration(label = "$$label_configuration=Configuration", `type` = "Text", order = 21, size = 160)
  def getConfiguration: String
  def setConfiguration(configuration: String): Unit

  @JsonIgnore
  def getBasicLtiConfiguration: BasicLtiConfiguration

  def getRoleMappings: Seq[BasicLtiRoleMapping]
end BasicLtiSystemComponent

final case class BasicLtiConfiguration(
  roleMappings: Seq[BasicLtiRoleMapping],
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  subtenant: Option[Long],
  autoCreateSubtenant: Option[Boolean],
  usernameFallback: Option[Boolean],
  sectionPerOffering: Option[Boolean],
  parameters: Option[Map[String, String]],
  parameterNames: Option[Map[String, String]],
  preferences: Option[Map[String, Any]],
  forceLti1Outcomes: Option[Boolean], // force downgrade to lti 1 outcomes
  daysToKeepActive: Option[Int],      // days after last login that enrolment is active - n/a if end-date provided
)

object BasicLtiConfiguration:
  def empty = BasicLtiConfiguration(Nil, None, None, None, None, None, None, None, None, None)

final case class BasicLtiRoleMapping(ltiRole: String, roleId: Option[String], domainRoleId: Option[String])
