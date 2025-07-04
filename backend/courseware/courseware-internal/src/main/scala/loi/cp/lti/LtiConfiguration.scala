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

package loi.cp.lti

import cats.syntax.option.*
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import loi.cp.config.JsonSchema.*
import loi.cp.config.{ConfigurationKey, ConfigurationKeyBinding}

/** Contains configuration for LTI Activities.
  */
@JsonIgnoreProperties(ignoreUnknown = true)
case class LtiConfiguration(
  syncEmptyLTIGrades: Boolean = false,
  scormPackaging: Boolean = false,
  siteRestrictions: Boolean = false,
)

@ConfigurationKeyBinding("ltiConfiguration")
object LtiConfiguration extends ConfigurationKey[LtiConfiguration]:
  override final val schema = Schema(
    title = "LTI".some,
    properties = List(
      BooleanField(
        name = "syncEmptyLTIGrades",
        description =
          "Send null grade on AGS Outcomes when the grade is removed (e.g. all attempts are deleted). Setting not applicable to launches that use LTI 1.x Outcomes".some
      ),
      BooleanField("scormPackaging", title = "SCORM Packaging".some, description = "Enable SCORM package export".some),
      BooleanField("siteRestrictions", description = "Enable site-based restrictions".some)
    )
  )

  override val init: LtiConfiguration = new LtiConfiguration
  val instance: this.type             = this
end LtiConfiguration
