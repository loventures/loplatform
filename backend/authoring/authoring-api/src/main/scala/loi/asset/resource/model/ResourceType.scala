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

package loi.asset.resource.model

import enumeratum.Enum
import enumeratum.EnumEntry.Uncapitalised

sealed abstract class ResourceType extends Uncapitalised {}

object ResourceType extends Enum[ResourceType]:

  val values = findValues

  case object ReadingInstructions extends ResourceType
  case object ReadingMaterial     extends ResourceType
  case object LtiLink             extends ResourceType
  case object AudioUpload         extends ResourceType
  case object AudioEmbed          extends ResourceType
  case object VideoUpload         extends ResourceType
  case object VideoEmbed          extends ResourceType
  case object ExternalLink        extends ResourceType
end ResourceType
