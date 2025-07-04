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
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.{ComponentEnvironment, ComponentImplementation, ComponentInstance}
import loi.cp.bootstrap.Bootstrap

@Component
class ConfigurationBootstrap(val componentInstance: ComponentInstance)(
  cs: ConfigurationService,
  env: ComponentEnvironment,
) extends ComponentImplementation:
  import ConfigurationBootstrap.*

  @Bootstrap("core.configurate")
  def configurate(cfgs: List[Config]): Unit = cfgs.foreach { case Config(keyStr, value) =>
    val keyTpe = lookupKeyType(env, keyStr).getOrElse {
      throw new IllegalArgumentException(s"unknown config: $keyStr")
    }
    val result = cs.setDomain(getInstance(keyTpe))(value)
    logger info result.toString
  }
end ConfigurationBootstrap

object ConfigurationBootstrap:

  private val logger = org.log4s.getLogger

  final case class Config(key: String, blob: JsonNode)
