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

package de.tomcat.config

import java.io.File

import com.typesafe.config.{Config, ConfigException}

/** Configuration for SSL trust and key stores.
  * @param trustStore
  * @param trustStorePassword
  * @param keyStore
  * @param keyStorePassword
  */
case class SSLConfig(
  trustStore: Option[File],
  trustStorePassword: Option[String],
  keyStore: Option[File],
  keyStorePassword: Option[String]
)
object SSLConfig:
  def load(config: Config): SSLConfig =
    SSLConfig(
      trustStore = optional(config.getString("truststore.file")).map(new File(_)),
      trustStorePassword = optional(config.getString("truststore.password")),
      keyStore = optional(config.getString("keystore.file")).map(new File(_)),
      keyStorePassword = optional(config.getString("keystore.password"))
    )

  private def optional[T](t: => T): Option[T] =
    try Some(t)
    catch case ex: ConfigException.Missing => None
end SSLConfig
