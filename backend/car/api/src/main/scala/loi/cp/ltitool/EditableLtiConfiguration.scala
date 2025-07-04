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

package loi.cp.ltitool

/** A collection of values determining whether certain [[LtiToolConfiguration]] values are modifiable in the context of
  * a placed Lti Tool.
  */
case class EditableLtiConfiguration(
  ltiVersion: Boolean,
  url: Boolean,
  key: Boolean,
  secret: Boolean,
  clientId: Boolean,
  deploymentId: Boolean,
  keysetUrl: Boolean,
  loginUrl: Boolean,
  redirectionUrls: Boolean,
  deepLinkUrl: Boolean,
  launchStyle: Boolean,
  includeUsername: Boolean,
  includeRoles: Boolean,
  includeEmailAddress: Boolean,
  includeContextTitle: Boolean,
  includeExternalId: Boolean,
  isGraded: Boolean,
  customParameters: Boolean,
  editableCustomParameters: Seq[String]
)

object EditableLtiConfiguration:
  def nothingPermitted = new EditableLtiConfiguration(
    ltiVersion = false,
    url = false,
    key = false,
    secret = false,
    deploymentId = false,
    clientId = false,
    keysetUrl = false,
    loginUrl = false,
    redirectionUrls = false,
    deepLinkUrl = false,
    launchStyle = false,
    includeUsername = false,
    includeRoles = false,
    includeContextTitle = false,
    includeEmailAddress = false,
    includeExternalId = false,
    isGraded = false,
    customParameters = false,
    editableCustomParameters = Seq()
  )
end EditableLtiConfiguration
