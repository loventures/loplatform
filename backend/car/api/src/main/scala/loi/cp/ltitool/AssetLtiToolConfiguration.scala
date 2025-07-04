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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/** A representation of configurations of an Lti Tool activity in the authoring environment
  *
  * @param toolId
  *   the database of the globally configured Lti Tool the content item created from this asset should pull from
  * @param name
  *   The name of the content item
  * @param toolConfiguration
  *   the configuration to apply to the content item created from this asset
  */
@JsonIgnoreProperties(ignoreUnknown = true)
case class AssetLtiToolConfiguration(
  toolId: String = "",
  name: String,
  toolConfiguration: LtiLaunchConfiguration
)
