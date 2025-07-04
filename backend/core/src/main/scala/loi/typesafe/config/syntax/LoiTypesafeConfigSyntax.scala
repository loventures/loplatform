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

package loi.typesafe.config.syntax

import com.typesafe.config.Config

import scala.language.implicitConversions

trait LoiTypesafeConfigSyntax:
  implicit final def loiTypesafeConfigSyntaxConfig(config: Config): LoiTypesafeConfigConfigOps =
    new LoiTypesafeConfigConfigOps(config)

// https://github.com/lightbend/config#how-to-handle-defaults
final class LoiTypesafeConfigConfigOps(private val config: Config) extends AnyVal:
  def getOptionString(path: String): Option[String] = if config.hasPath(path) then Some(config.getString(path))
  else None
