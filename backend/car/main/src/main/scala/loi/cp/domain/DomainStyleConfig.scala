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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.AdminRight
import loi.cp.config.JsonSchema.*
import loi.cp.config.{ConfigurationKey, ConfigurationKeyBinding}
import loi.cp.i18n.Translatable
import scalaz.\/
import scalaz.syntax.either.*

import java.util as ju
import scala.jdk.CollectionConverters.*

@JsonIgnoreProperties(ignoreUnknown = true)
private[domain] case class DomainStyleConfig(
  variables: ju.Map[String, String] = ju.Collections.emptyMap()
)

private[domain] object DomainStyleConfig:
  import Translatable.RawStrings.*

  val key: Key.type = Key

  @ConfigurationKeyBinding(
    value = "style",
    read = new Secured(allowAnonymous = true),
    write = new Secured(Array(classOf[AdminRight]))
  )
  object Key extends ConfigurationKey[DomainStyleConfig] with ConfigurationKey.Hidden:
    override final val schema = Schema(
      description = Some("Configuration for the domain-wide styles."),
      // no properties needed because this is hidden
    )

    override val init: DomainStyleConfig = DomainStyleConfig()

    override def validate(d: DomainStyleConfig): Translatable.Any \/ Unit =
      val missing =
        Set("color-primary", "color-secondary", "color-accent") diff d.variables.asScala.filterNot(_._2 == null).keySet
      if missing.nonEmpty then Translatable.Any(s"The keys [${missing.mkString(", ")}] are required.").left
      else ().right
  end Key
end DomainStyleConfig
