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

import argonaut.*
import argonaut.Argonaut.*
import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.ErrorResponse
import com.learningobjects.cpxp.component.{ComponentEnvironment, ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.json.JacksonCodecs.*
import com.learningobjects.de.authorization.{Secured, SecuredService}
import de.tomcat.juli.LogMeta
import org.log4s.Logger
import scalaz.\/
import scalaz.syntax.either.*
import scalaz.syntax.functor.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*

import scala.collection.SortedMap
import scala.jdk.CollectionConverters.*

@Component
class ConfigurationRootApiImpl(val componentInstance: ComponentInstance)(implicit
  cs: ConfigurationService,
  env: ComponentEnvironment,
  securedService: SecuredService,
) extends ConfigurationRootApi
    with ComponentImplementation:
  import ConfigurationRootApi.*
  import ConfigurationRootApiImpl.*

  override def keys: SortedMap[String, JsonSchema] =
    SortedMap(schemata*)

  private def schemata =
    env.getRegistry
      .lookupAllClasses(keyBase)
      .asScala
      .toList
      .filterNot(isHidden)
      .filter(keyTpe => securedService.isPermitted(configWrite(keyTpe), None)) // todo: read-only configuration view?
      .map { keyTpe =>
        val key    = getInstance(keyTpe)
        val schema = key.schema
        val name   = configName(keyTpe)
        val valTpe = ConfigurationKey.jDataType(keyTpe)
        name -> JsonSchema.serialize(schema, valTpe.getTypeName.replaceAll(".*\\.", ""))
      }

  private def domainConfig(key: ConfigurationKey[?]) =
    val config = cs.getDomainDetail(key)
    ConfigOut.create(config)

  override def getDomain(key0: String): ErrorResponse \/ ConfigOut =
    for
      keyCls <- lookupKeyType(env, key0) \/> ErrorResponse.notFound
      _      <- ensurePermitted(_.read, None)(keyCls)
    yield
      val key = getInstance(keyCls)
      domainConfig(key)

  override def getItem(key0: String, item: Long): ErrorResponse \/ ConfigOut =
    for
      keyCls <- lookupKeyType(env, key0) \/> ErrorResponse.notFound
      _      <- ensurePermitted(_.read, Some(item))(keyCls)
    yield
      val key    = getInstance(keyCls)
      val config = cs.getItemDetail(key)(item)
      ConfigOut.create(config)

  override def setDomain(key0: String, value: Option[JsonNode]): ErrorResponse \/ Unit =
    for
      keyCls <- lookupKeyType(env, key0) \/> ErrorResponse.notFound
      _      <- ensurePermitted(_.write, None)(keyCls)
      key    <- getInstance(keyCls).right
      _       = logBlob(key0, "config", domainConfig(key), "Configuration before")
      _       = logBlob(key0, "value", value, "Configuration update")
      _      <- applyDomain(key, value)
    yield logBlob(key0, "config", domainConfig(key), "Configuration after")

  private def applyDomain(key: ConfigurationKey[?], valOpt: Option[JsonNode]): ErrorResponse \/ Unit =
    valOpt match
      case None        =>
        key.clearDomain().right
      case Some(value) =>
        key.setDomain(value).leftMap(ErrorResponse.badRequest).void

  private def logBlob[A: EncodeJson](key: String, name: String, value: A, msg: String): Unit =
    LogMeta.let("key" -> key.asJson, name -> value.asJson) {
      logger.info(msg)
    }

  override def getItemNonAdmin(key0: String, item: Long): ErrorResponse \/ JsonNode =
    getItem(key0, item).map(_.effective)

  override def setItem(key0: String, item: Long, value: Option[JsonNode]): ErrorResponse \/ Unit =
    for
      keyCls <- lookupKeyType(env, key0) \/> ErrorResponse.notFound
      _      <- ensurePermitted(_.write, Some(item))(keyCls)
      key    <- getInstance(keyCls).right
      _      <- cs.setItem(key, item, value).leftMap(ErrorResponse.badRequest)
    yield ()

  private def ensurePermitted[T](extr: ConfigurationKeyBinding => Secured, id: Option[Long])(
    tpe: ConfigurationKey.Any
  ): ErrorResponse \/ Unit =
    securedService.isPermitted(extr(ConfigurationKey.configBinding(tpe)), id) either (()) or ErrorResponse.forbidden
end ConfigurationRootApiImpl

object ConfigurationRootApiImpl:
  private val logger: Logger = org.log4s.getLogger

  private val isHidden: Class[? <: ConfigurationKey[?]] => Boolean =
    classOf[ConfigurationKey.Hidden].isAssignableFrom(_)

  import language.implicitConversions
  implicit def long2Id(id: Long): Id = () => id
