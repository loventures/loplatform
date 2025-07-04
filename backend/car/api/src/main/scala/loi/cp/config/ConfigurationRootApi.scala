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

package loi.cp.config

import argonaut.EncodeJson
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.learningobjects.cpxp.component.annotation.{Controller, PathVariable, RequestBody, RequestMapping}
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.de.authorization.Secured
import loi.cp.config.ConfigurationService.ConfigDetail
import loi.jackson.syntax.any.*
import scalaz.\/

import scala.collection.SortedMap

@Controller(value = "config", root = true, category = Controller.Category.CORE)
@RequestMapping(path = "config")
@Secured(allowAnonymous = true) // we do our own security
trait ConfigurationRootApi extends ApiRootComponent:
  import ConfigurationRootApi.*

  @RequestMapping(method = Method.GET)
  def keys: SortedMap[String, JsonSchema]

  @RequestMapping(path = "{key}", method = Method.GET)
  def getDomain(@PathVariable("key") key: String): ErrorResponse \/ ConfigOut

  @RequestMapping(path = "{key}", method = Method.PUT)
  def setDomain(@PathVariable("key") key: String, @RequestBody value: Option[JsonNode]): ErrorResponse \/ Unit

  @RequestMapping(path = "{key}/item/{item}", method = Method.GET)
  def getItem(@PathVariable("key") key: String, @PathVariable("item") item: Long): ErrorResponse \/ ConfigOut

  // TODO: Evict me to a dedicated endpoint for the use case
  // and what use case is that?
  @RequestMapping(path = "{key}/{item}", method = Method.GET)
  def getItemNonAdmin(@PathVariable("key") key: String, @PathVariable("item") item: Long): ErrorResponse \/ JsonNode

  @RequestMapping(path = "{key}/{item}", method = Method.PUT)
  def setItem(
    @PathVariable("key") key: String,
    @PathVariable("item") item: Long,
    @RequestBody value: Option[JsonNode]
  ): ErrorResponse \/ Unit
end ConfigurationRootApi

object ConfigurationRootApi:

  /* A mapified JSON schema */
  type JsonSchema = Map[String, AnyRef]

  case class ConfigOut(
    effective: JsonNode,
    defaults: JsonNode,
    overrides: JsonNode
  )

  object ConfigOut:
    /* if I call this apply it's recursive -_- */
    def create(effective: Any, defaults: Any, overrides: Any)(implicit mapper: ObjectMapper): ConfigOut =
      ConfigOut(
        effective = mapper `valueToTree` effective,
        defaults = mapper `valueToTree` defaults,
        overrides = mapper `valueToTree` overrides
      )

    def create[A](detail: ConfigDetail[A]): ConfigOut = ConfigOut(
      detail.value.finatraEncoded,
      detail.parent.finatraEncoded,
      detail.overrides
    )

    import com.learningobjects.cpxp.scala.json.JacksonCodecs.*
    implicit val encode: EncodeJson[ConfigOut] = EncodeJson.derive[ConfigOut]
  end ConfigOut
end ConfigurationRootApi
