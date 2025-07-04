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

package loi.authoring.json

import com.learningobjects.cpxp.service.user.UserDTO

import java.util.{Date, UUID}

/** A representation of [[loi.authoring.asset.Asset]] suitable for JSON serialization, namely, the asset info is
  * unwrapped.
  *
  * [[loi.authoring.asset.Asset]] itself should never convert to JSON because doing so may unintentionally leak possible
  * behavior values on the asset implementation type.
  *
  * We never intend to deserialize JSON to an [[AssetJson]]
  *
  * @see
  *   AssetJsonConverter
  */
case class AssetJson(
  id: Long,
  name: UUID,
  typeId: String,
  created: Date,
  modified: Date,
  createdBy: Option[UserDTO],
  data: Any
)
