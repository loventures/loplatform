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

package loi.cp.user

import cats.syntax.option.*
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.UserAdminRight
import loi.cp.config.JsonSchema.*
import loi.cp.config.{ConfigurationKey, ConfigurationKeyBinding}

@JsonIgnoreProperties(ignoreUnknown = true)
case class UserPreferences(
  sendAlertEmails: Boolean = false,
  authoringPreferences: AuthoringPreferences = AuthoringPreferences(
    editModeDefault = false,
    autoPreview = false,
  ),
)

@JsonIgnoreProperties(ignoreUnknown = true)
case class AuthoringPreferences(
  editModeDefault: Boolean, // default to edit mode vs view mode
  autoPreview: Boolean,     // automatically open html preview
)

@ConfigurationKeyBinding(
  value = "userPreferences",
  read = new Secured(value = Array(classOf[UserAdminRight]), byOwner = true),
  write = new Secured(value = Array(classOf[UserAdminRight]), byOwner = true)
)
object UserPreferences extends ConfigurationKey[UserPreferences]:
  override final val schema = Schema(
    title = "Users".some,
    properties = List(
      BooleanField("sendAlertEmails", description = Some("Send email notifications for alerts")),
      ObjectField(
        "authoringPreferences",
        description = Some("Preferences for users in the authoring environment"),
        properties = List(
          BooleanField("editModeDefault"),
          BooleanField("autoPreview"),
        )
      ),
    )
  )

  override val init: UserPreferences = new UserPreferences

  /** Java access. */
  val instance: this.type = this
end UserPreferences
