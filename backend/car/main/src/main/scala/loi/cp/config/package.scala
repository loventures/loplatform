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

package loi.cp

import com.learningobjects.cpxp.component.ComponentEnvironment
import com.learningobjects.de.authorization.Secured

package object config:
  private[config] val keyBase = classOf[ConfigurationKey[?]]

  private[config] def lookupKeyType(env: ComponentEnvironment, key: String) =
    Option(env.getRegistry.lookupClass(keyBase, key))

  /** Get the instance of `keyTpe`, if a module, otherwise create an instance using its zero-arg constructor.
    */
  private[config] def getInstance(keyTpe: Class[? <: ConfigurationKey[?]]) =
    if keyTpe.getName.endsWith("$") then keyTpe.getField("MODULE$").get(null).asInstanceOf[ConfigurationKey[?]]
    else keyTpe.getDeclaredConstructor().newInstance()

  /** Get the `name` value of the `ConfigurationKeyBinding` */
  private[config] def configName(keyTpe: ConfigurationKey.Any): String =
    ConfigurationKey.configBinding(keyTpe).value

  /** Get the `read` value of the `ConfigurationKeyBinding` */
  private[config] def configRead(keyTpe: ConfigurationKey.Any): Secured =
    ConfigurationKey.configBinding(keyTpe).read

  /** Get the `write` value of the `ConfigurationKeyBinding` */
  private[config] def configWrite(keyTpe: ConfigurationKey.Any): Secured =
    ConfigurationKey.configBinding(keyTpe).write
end config
