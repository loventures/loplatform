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

package loi.authoring.asset.web

import com.fasterxml.jackson.databind.JsonNode

/** Data transfer object that carries data about creating an asset. This DTO is dedicated to web clients.
  *
  * @param typeId
  *   the type id value of the asset to create
  * @param data
  *   the data of the asset to create
  */
private[web] case class CreateAssetWebDto(
  typeId: String,
  data: JsonNode
)
