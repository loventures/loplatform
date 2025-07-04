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

import java.lang as jl
import java.util.Date
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method, WebResponse}
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.service.domain.DomainState
import com.learningobjects.de.authorization.Secured
import loi.cp.overlord.*
import loi.cp.right.RightMatch

import scalaz.\/

/** Root controller for managing domains. */
@Controller(value = "domains", root = true)
@RequestMapping(path = "domains")
@Secured(value = Array(classOf[OverlordRight]), `match` = RightMatch.ANY)
trait OverlordDomainRootApi extends ApiRootComponent:
  @RequestMapping(method = Method.GET)
  @Secured(Array(classOf[SupportRight]))
  def get(q: ApiQuery): ApiQueryResults[OverlordDomain]

  @RequestMapping(path = "{id}", method = Method.GET)
  @Secured(Array(classOf[OverlordRight]))
  def get(@PathVariable("id") id: Long): Option[OverlordDomain]

  @RequestMapping(method = Method.POST)
  @Secured(Array(classOf[OverlordRight]))
  def create(@RequestBody t: OverlordDomain): OverlordDomain

  @RequestMapping(path = "validate", method = Method.POST)
  @Secured(Array(classOf[UnderlordRight]))
  def validate(@RequestBody request: DomainProperties): Unit

  @RequestMapping(path = "provision", method = Method.POST, async = true)
  @Secured(Array(classOf[UnderlordRight]))
  def provision(@RequestBody request: ProvisionRequest): ProvisionResponse

  /** After creation, this initializes a new domain. */
  @RequestMapping(path = "{id}/init", method = Method.POST, async = true)
  @Secured(Array(classOf[OverlordRight]))
  def initialize(@PathVariable("id") id: Long): Unit

  @RequestMapping(path = "{id}/requestDns", method = Method.POST, async = true)
  @Secured(Array(classOf[OverlordRight]))
  def requestDns(@PathVariable("id") id: Long): String \/ String

  @RequestMapping(path = "{id}/bootstrap/{profile}", method = Method.POST, async = true)
  @Secured(Array(classOf[OverlordRight]))
  def bootstrap(
    @PathVariable("id") id: Long,
    @PathVariable("profile") identifier: String,
    @RequestBody config: JsonNode
  ): Unit

  @RequestMapping(path = "{id}", method = Method.PUT)
  @Secured(Array(classOf[OverlordRight]))
  def updateDomain(@PathVariable("id") id: Long, @RequestBody d: OverlordDomain): OverlordDomain

  @RequestMapping(path = "{id}/state", method = Method.POST)
  @Secured(Array(classOf[OverlordRight]))
  def transitionDomain(@PathVariable("id") id: Long, @RequestBody s: StateChange): Unit

  @RequestMapping(path = "{id}", method = Method.DELETE)
  @Secured(Array(classOf[OverlordRight]))
  def deleteDomain(
    @PathVariable("id") id: Long,
    @QueryParam(required = false) hard: jl.Boolean,
  ): Unit

  @RequestMapping(path = "{id}/manage", method = Method.POST)
  @Secured(Array(classOf[SupportRight]))
  def manageDomain(@PathVariable("id") id: Long, request: HttpServletRequest, response: HttpServletResponse): String

  @RequestMapping(path = "it/{did}", method = Method.POST)
  @Secured(Array(classOf[OverlordRight]))
  def itDomain(@PathVariable("did") did: String, response: HttpServletResponse): WebResponse

  @RequestMapping(path = "profiles", method = Method.GET)
  @Secured(Array(classOf[OverlordRight]))
  def profiles: OverlordProfiles

  @RequestMapping(path = "redshiftSchemaNames", method = Method.GET)
  @Secured(Array(classOf[OverlordRight]))
  def redshiftSchemaNames: List[String]

  @RequestMapping(path = "maintenance", method = Method.PUT)
  @Secured(Array(classOf[OverlordRight]))
  def maintenance(@RequestBody m: MaintenanceMode): MaintenanceMode
end OverlordDomainRootApi

case class ProvisionRequest(domain: DomainProperties, account: AccountProperties, appearance: AppearanceProperties)

/** Pretty crappy but.. DNS and password should be folded into the properties. */
case class ProvisionResponse(
  domain: DomainProperties,
  account: AccountProperties,
  appearance: AppearanceProperties,
  password: String,
  dns: Boolean
)

case class DomainProperties(domainId: String, name: String, shortName: String, hostName: String)

case class AccountProperties(
  userName: String,
  givenName: String,
  middleName: String,
  familyName: String,
  emailAddress: String
)

case class AppearanceProperties(
  primaryColor: String,
  secondaryColor: String,
  accentColor: String,
  favicon: Option[UploadInfo],
  logo: Option[UploadInfo]
)

case class StateChange(state: DomainState, message: Option[String])

case class OverlordProfiles(dnsSupported: Boolean, profiles: Seq[OverlordProfile])

case class OverlordProfile(identifier: String, name: String, configs: Seq[ProfileConfig])

case class MaintenanceMode(enabled: Boolean, end: Option[Date])

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(name = "Boolean", value = classOf[BooleanConfig]),
    new JsonSubTypes.Type(name = "String", value = classOf[StringConfig]),
    new JsonSubTypes.Type(name = "Choice", value = classOf[ChoiceConfig]),
    new JsonSubTypes.Type(name = "Select", value = classOf[SelectConfig])
  )
)
sealed trait ProfileConfig:
  val id: String
  val name: String
  val default: Any
end ProfileConfig

case class BooleanConfig(id: String, name: String, default: Boolean) extends ProfileConfig

case class StringConfig(id: String, name: String, default: String) extends ProfileConfig

case class ChoiceConfig(id: String, name: String, yesName: String, noName: String, default: Boolean)
    extends ProfileConfig

case class SelectConfig(id: String, name: String, choices: Seq[Selection], default: String) extends ProfileConfig

case class Selection(id: String, name: String)
