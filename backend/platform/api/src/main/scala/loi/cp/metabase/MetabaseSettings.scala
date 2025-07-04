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

package loi.cp.metabase

import cats.syntax.option.*
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.AdminRight
import loi.cp.config.JsonSchema.*
import loi.cp.config.{ConfigurationKey, ConfigurationKeyBinding}

@JsonIgnoreProperties(ignoreUnknown = true)
case class MetabaseSettings(
  siteUrl: String,
  secretKey: String
)

private object MetabaseSettings:
  @ConfigurationKeyBinding(
    value = "metabase",
    read = new Secured(Array(classOf[AdminRight])),
    write = new Secured(Array(classOf[AdminRight]))
  )
  object Key extends ConfigurationKey[MetabaseSettings]:
    override val schema = Schema(
      title = "Metabase".some,
      properties = List(
        StringField("siteUrl"),
        StringField("secretKey")
      )
    )

    override val init: MetabaseSettings = MetabaseSettings(
      siteUrl = "https://storas.example.org",
      secretKey = null
    )
  end Key
end MetabaseSettings
