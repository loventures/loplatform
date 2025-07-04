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
import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.registry.Bound
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import loi.cp.i18n.Translatable
import scalaz.{Memo, \/, \/-}
import scaloi.syntax.classTag.*

import java.lang.reflect.{ParameterizedType, Type}
import scala.annotation.{nowarn, tailrec}
import scala.reflect.ClassTag

/** A key which witnesses and testifies that `Data` can be used to configure either the application or specific
  * entities. Bound by ConfigurationKeyBinding to allow automatic generation of configuration pages.
  */
@Bound(classOf[ConfigurationKeyBinding])
abstract class ConfigurationKey[Data]:

  /** An initial value to be used for previously-unconfigured entities. */
  val init: Data // I would name this "default" but we do use Java here

  /** Validate that `d` conforms to some manner of restriction. */
  def validate(@nowarn d: Data): Translatable.Any \/ Unit = \/-(())

  /** Get the schema for this. */
  def schema: JsonSchema.Schema
end ConfigurationKey

object ConfigurationKey:

  /** The type of any configuration key. */
  type Any = Class[? <: ConfigurationKey[?]]

  /** A marker interface asserting that a key should not be displayed on the configuration admin page. Presumably there
    * is another way for a user to view/modify it.
    */
  trait Hidden:
    this: ConfigurationKey[?] =>

  /** Get the `ConfigurationKeyBinding`, cachedly. */
  val configBinding: ConfigurationKey.Any => ConfigurationKeyBinding =
    Memo.immutableHashMapMemo[ConfigurationKey.Any, ConfigurationKeyBinding] { keyTpe =>
      Option(keyTpe.getAnnotation(classOf[ConfigurationKeyBinding]))
        .getOrElse(throw new IllegalArgumentException(s"ConfigurationKey ${keyTpe.getName} not bound"))
    }

  def configBindingA[A <: ConfigurationKey[?]: ClassTag]: ConfigurationKeyBinding = configBinding(classTagClass[A])

  private val keyBase = classOf[ConfigurationKey[?]]

  /** Get the actual type of the `Data` parameter on a `ConfigurationKey`, cachedly */
  val jDataType: Class[? <: ConfigurationKey[?]] => Type =
    Memo.immutableHashMapMemo[Class[? <: ConfigurationKey[?]], Type] { key =>
      @tailrec def loop(tpe: Class[?]): Type =
        if tpe.getSuperclass == keyBase then
          tpe.getGenericSuperclass
            .asInstanceOf[ParameterizedType]
            .getActualTypeArguments
            .head
        else loop(tpe.getSuperclass)
      loop(key)
    }

  def decoded[A](key: ConfigurationKey[A], json: JsonNode): A =
    val parser      = json.traverse(JacksonUtils.getMapper)
    val jlrType     = jDataType(key.getClass)
    val jacksonType = JacksonUtils.getMapper.constructType(jlrType)
    JacksonUtils.getMapper.readValue[A](parser, jacksonType)

  /** Put the methods from `ConfigurationService` onto a `ConfigurationKey`, as a convenience. */
  implicit final class ConfigurationKeyOps[D](val self: ConfigurationKey[D]) extends AnyVal:

    import ConfigurationService.*

    @inline def getItem(t: Id)(implicit cs: ConfigurationService): D =
      cs.getItem(self)(t)

    @inline def getDomain(implicit cs: ConfigurationService): D =
      cs.getDomain(self)

    @inline def getSystem(implicit cs: ConfigurationService): D =
      cs.getSystem(self)

    @inline def setDomain(d: JsonNode)(implicit cs: ConfigurationService): SetResult[D] =
      cs.setDomain(self)(d)

    @inline def clearDomain()(implicit cs: ConfigurationService): Unit =
      cs.clearDomain(self)
  end ConfigurationKeyOps
end ConfigurationKey
