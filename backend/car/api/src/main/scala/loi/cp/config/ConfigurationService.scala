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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.annotation.Service
import loi.cp.i18n.Translatable
import scalaz.\/

import java.lang

/** A service to access configurations of entities.
  *
  * Every persisted object can have arbitrary data associated with it by any consumer. Data set by different consumers
  * will not interfere with each other; every datum is associated with a (hopefully private) key to avoid collisions.
  *
  * A specific association of a datum onto an entity is described by the `ConfigurationKey[_]` typeclass. It is not
  * intended that this be provided as part of any public API; rather, a specific implementation should be provided in
  * each internal module which needs one.
  *
  * Configurations are merged together from multiple sources, allowing for defaults specified at domain, cluster, and
  * source levels.
  *
  * Specifically, for an entity `t` and a configuration datum `D`, the value of `get(t)` will be the default value
  * specified by the `ConfigurationKey` instance, merged with the overlord persisted configuration, merged with the
  * domain configuration for that key, and merged with the entity's persisted configuration. The merging is done based
  * on a recursive merge of the product tree/JSON document representing the configuration.
  *
  * Currently modifications are done using JsonNodes... this is sub-optimal, and One Day™ we will have a properly
  * serializable lens data type for this. Perhaps even JsonPatch...
  */
@Service(unique = true)
trait ConfigurationService:
  import ConfigurationService.*

  /** Look up a configuration value for a key `key` on entity `t` */
  final def getItem[Data](key: ConfigurationKey[Data])(t: Id): Data = getItemDetail(key)(t).value

  final def getItem[A](key: ConfigurationKey[A], id: Long): A = getItem(key)(() => id)

  /** Look up a raw configuration value for a key `key` on entity `t`. Returned configuration is not merged with the
    * domain and so-on
    */
  def getRawItemJson[Data](key: ConfigurationKey[Data])(t: Id): JsonNode

  /** Look up a configuration value for a key `key` on entity `t`. Includes config for the parent of `t`
    */
  def getItemDetail[Data](key: ConfigurationKey[Data])(t: Id): ConfigDetail[Data]

  /** Look up a domain-level configuration for the key `key` */
  final def getDomain[Data](key: ConfigurationKey[Data]): Data = getDomainDetail(key).value

  /** Look up a domain-level configuration for the key `key` */
  final def getDomainJson[Data](key: ConfigurationKey[Data]): JsonNode = getDomainDetail(key).valueJson

  /** Lookup a domain-level configuration for the key `key`.
    *
    * If the domain is not overlord, includes config for overlord. If the domain is overlord, includes the default
    * config for `key`
    */
  def getDomainDetail[Data](key: ConfigurationKey[Data]): ConfigDetail[Data]

  /** Look up a system-wide configuration for the key `key` */
  def getSystem[Data](key: ConfigurationKey[Data]): Data

  /** Patch a configuration value on an entity `t`. This appends `d` to the existing configuration. */
  def patchItem[Data](key: ConfigurationKey[Data])(t: Id, d: JsonNode): SetResult[Data]

  /** Set `itemJson` at `key` on `item`'s config. If `itemJson` is `None`, remove `key` from `item`'s config
    */
  def setItem[A](key: ConfigurationKey[A], item: Id, itemJson: Option[JsonNode]): SetResult[A]

  final def setItem[A](key: ConfigurationKey[A], itemId: Long, itemJson: Option[JsonNode]): SetResult[A] =
    setItem(
      key,
      new Id:
        override def getId: lang.Long = itemId
      ,
      itemJson
    )

  final def setItem[A](key: ConfigurationKey[A], itemId: Long, itemJson: JsonNode): SetResult[A] =
    setItem(key, itemId, Option(itemJson))

  /** Set the entire configuration value on an entity. */
  def overwriteItem[Data](key: ConfigurationKey[Data])(t: Id, d: Data): SetResult[Data]

  /** Set the configuration value at the domain level for `key`. This may be a subset of the full [[Data]] structure. */
  def setDomain[Data](key: ConfigurationKey[Data])(d: JsonNode): SetResult[Data]

  /** Set the entire configuration value at the domain level for `key`. */
  def overwriteDomain[Data](key: ConfigurationKey[Data])(d: Data): SetResult[Data]

  /** Remove all local configuration information for the key `key` on entity `t` */
  def clearItem[Data](key: ConfigurationKey[Data])(t: Id): Unit

  /** Remove all domain-level configuration for the key `key`. */
  def clearDomain[Data](key: ConfigurationKey[Data]): Unit

  /** Copy all configuration from a source item to a destination item. */
  def copyConfiguration(dst: Id, src: Id): Unit

  /** Validates and sets `itemJson` as the value for `key` on `item`. The sum of `parentJson` + `itemJson` is validated,
    * but only `itemJson` is set on `item`.
    *
    * If `itemJson` is `None` then `key` is removed from `item`'s config
    *
    * @return
    *   `parentJson` + `itemJson` decoded
    */
  def setJson[A](
    key: ConfigurationKey[A],
    item: Id,
    itemJson: Option[JsonNode],
    parentJson: JsonNode
  ): SetResult[A]

  /** Validate `itemJson` is a valid addend to `parentJson` for `key` */
  def validate[A](key: ConfigurationKey[A], itemJson: JsonNode, parentJson: JsonNode): SetResult[A]
end ConfigurationService

/** Configuration service companion object. */
object ConfigurationService:

  /** Either an error message, or the new value. */
  type SetResult[D] = Translatable.Any \/ D

  /** Config value along with detail about the hierarchy of config that formed `value`.
    *
    * The hierarchy is Overlord + Domain + Item for most items. For such a hierarchy:
    *   - ConfigDetail for an item carries the domain config in `parent`
    *   - ConfigDetail for a domain carries the overlord config in `parent`.
    *
    * Certain kinds of groupfinder have a different hierarchy.
    *
    * @param value
    *   `parent` + `overrides`
    * @param parent
    *   the config of parent of `value`
    * @param overrides
    *   partial type of D, how `value` changed `parent`, null if no overrides
    */
  case class ConfigDetail[A](
    value: A,
    valueJson: JsonNode,
    parent: A,
    parentJson: JsonNode,
    overrides: JsonNode
  )

  object ConfigDetail:
    def apply[A](
      key: ConfigurationKey[A],
      valueJson: JsonNode,
      parentJson: JsonNode,
      overrides: JsonNode
    ): ConfigDetail[A] =
      ConfigDetail(
        ConfigurationKey.decoded(key, valueJson),
        valueJson,
        ConfigurationKey.decoded(key, parentJson),
        parentJson,
        overrides
      )

    def noOverrides[A](parent: A, parentJson: JsonNode): ConfigDetail[A] = ConfigDetail(
      parent,
      parentJson,
      parent,
      parentJson,
      JsonNodeFactory.instance.objectNode(),
    )
  end ConfigDetail
end ConfigurationService
