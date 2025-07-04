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

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.{DeserializationFeature, JsonNode, ObjectMapper}
import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.util.ConfigUtils.applyDefaults
import com.learningobjects.cpxp.overlord.SmallOverlordService
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.util.cache.{BucketGenerationalCache, Entry}
import loi.cp.config.ConfigurationService.ConfigDetail
import loi.cp.i18n.Translatable
import scalaz.\/
import scalaz.syntax.either.*
import scaloi.syntax.disjunction.*

import java.lang as jl
import java.lang.reflect.Type
import scala.concurrent.duration.*
import scala.util.{Failure, Success, Try}

@Service
class ConfigurationServiceImpl(implicit
  domainDTO: () => DomainDTO,
  fs: FacadeService,
  mapper: ObjectMapper,
  ows: SmallOverlordService,
  cache: DomainConfigCache
) extends ConfigurationService:
  import ConfigurationService.*
  import ConfigurationServiceImpl.*

  override def getRawItemJson[Data](key: ConfigurationKey[Data])(t: Id): JsonNode =
    t.facade[ConfigFacade].getAttr(configName(key.getClass))

  override def getItemDetail[Data](key: ConfigurationKey[Data])(t: Id): ConfigDetail[Data] =
    val localConfig  = getRawItemJson(key)(t)
    val domainJson   = getDomainDetail(key).valueJson // overlord + domain
    val mergedConfig = applyDefaults(localConfig, domainJson)
    ConfigDetail(key, mergedConfig, domainJson, localConfig)

  override def getDomainDetail[Data](key: ConfigurationKey[Data]): ConfigDetail[Data] =
    domainDefaults(key)

  override def getSystem[Data](key: ConfigurationKey[Data]): Data = getSystemDetail(key).value

  private def getSystemDetail[A](key: ConfigurationKey[A]): ConfigDetail[A] = overlord match
    case None             => // no overlord :fearful:
      val parent     = key.init
      val parentJson = mapper.valueToTree[JsonNode](parent)
      ConfigDetail.noOverrides(parent, parentJson)
    case Some(overlordId) => domainDefaults(key, overlordId)

  override def patchItem[Data](key: ConfigurationKey[Data])(t: Id, d: JsonNode): SetResult[Data] =
    val itemCfgs = t.facade[ConfigFacade]
    val orig     = itemCfgs.getAttr(configName(key.getClass))
    setItem(key, t, Some(applyDefaults(d, orig)))

  override def setItem[A](key: ConfigurationKey[A], item: Id, itemJson: Option[JsonNode]): SetResult[A] =
    val parentJson = getDomainJson(key)
    setJson(key, item, itemJson, parentJson)

  override def overwriteItem[Data](key: ConfigurationKey[Data])(t: Id, d: Data): SetResult[Data] =
    setItem(key, t, Some(mapper.valueToTree(d)))

  override def setDomain[Data](key: ConfigurationKey[Data])(d: JsonNode): SetResult[Data] =
    val parentJson = getSystemDetail(key).valueJson
    setJson(key, domainDTO, Some(d), parentJson).rightTap(_ => invalidate(configName(key.getClass)))

  override def overwriteDomain[Data](key: ConfigurationKey[Data])(d: Data): SetResult[Data] =
    setDomain(key)(mapper.valueToTree(d))

  override def setJson[A](
    key: ConfigurationKey[A],
    item: Id,
    itemJson: Option[JsonNode],
    parentJson: JsonNode
  ): SetResult[A] =
    itemJson match
      case None           =>
        item.facade[ConfigFacade].removeAttr(configName(key.getClass))
        validate(key, JsonNodeFactory.instance.objectNode(), parentJson)
      case Some(itemJson) =>
        validate(key, itemJson, parentJson) map { validA =>
          item.facade[ConfigFacade].setAttr(configName(key.getClass), itemJson)
          validA
        }

  override def validate[A](
    key: ConfigurationKey[A],
    itemJson: JsonNode,
    parentJson: JsonNode
  ): SetResult[A] =

    val nextValueJson = applyDefaults(itemJson, parentJson)

    for
      deserialized <- tryDeserialize[A](nextValueJson, ConfigurationKey.jDataType(key.getClass))
      _            <- key.validate(deserialized)
    yield deserialized
  end validate

  override def clearItem[Data](key: ConfigurationKey[Data])(t: Id): Unit =
    val itemCfgs = t.facade[ConfigFacade]
    // itemCfgs.refresh()
    itemCfgs.removeAttr(configName(key.getClass))

  override def clearDomain[Data](key: ConfigurationKey[Data]): Unit =
    val domainCfgs = domainDTO.facade[ConfigFacade]
    // domainCfgs.refresh()
    domainCfgs.removeAttr(configName(key.getClass))

    invalidate(configName(key.getClass))

  override def copyConfiguration(dst: Id, src: Id): Unit =
    val config = src.facade[ConfigFacade].getConfig
    dst.facade[ConfigFacade].setConfig(config)

  private def invalidate(name: String) =
    cache.remove(DomainConfigKey(domainDTO.id, name))
    if isOverlord then cache.invalidate(name)

  private def overlord: Option[Long] = ows.findOverlordDomainId()
  private def isOverlord: Boolean    = overlord.contains(domainDTO.id)

  private def domainDefaults[Data](key: ConfigurationKey[Data], domain: jl.Long = domainDTO.id): ConfigDetail[Data] =
    def compute() =
      val parentJson: JsonNode = overlord match
        case None                                     => mapper.valueToTree(key.init) // no overlord :fearful:
        case Some(overlordId) if overlordId == domain => mapper.valueToTree(key.init) // overlord has no parent
        case Some(overlordId)                         => domainDefaults(key, overlordId).valueJson

      /* Aliases, though...! */
      val domainOverrides = domain.facade[ConfigFacade].getAttr(configName(key.getClass))
      val valueJson       = applyDefaults(domainOverrides, parentJson)
      val configDetail    = ConfigDetail(key, valueJson, parentJson, domainOverrides)
      new DomainConfigEntry(DomainConfigKey(domain, configName(key.getClass)), configDetail)
    end compute

    cache
      .getOrCompute((() => compute()), DomainConfigKey(domain, configName(key.getClass)))
      .asInstanceOf[ConfigDetail[Data]]
  end domainDefaults

  /** Intolerant mapper for strict checking of (imaginary) schemata. */
  private lazy val intolerant = mapper.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

  def tryDeserialize[Data](node: JsonNode, tpe: Type): Translatable.Any \/ Data =
    val jacksonType = mapper.getTypeFactory.constructType(tpe)
    Try(intolerant.readValue[Any](node.traverse(intolerant), jacksonType)) match
      case Failure(e)     => Translatable.Any(s"New value $node is impermissible as a $tpe (${e.getMessage})").left
      case Success(null)  => Translatable.Any("Null values for configuration keys are not allowed").left
      case Success(value) => value.asInstanceOf[Data].right // bleugh
end ConfigurationServiceImpl

object ConfigurationServiceImpl extends Translatable.RawStrings

final case class DomainConfigKey(domain: jl.Long, configName: String)

final class DomainConfigEntry(key: DomainConfigKey, cc: ConfigDetail[?])
    extends Entry[DomainConfigKey, ConfigDetail[?]](key, cc, Set(key.configName))

final class DomainConfigCache
    extends BucketGenerationalCache[DomainConfigKey, ConfigDetail[?], DomainConfigEntry](
      itemAware = false,
      replicated = true,
      timeout = 60.minutes
    )
