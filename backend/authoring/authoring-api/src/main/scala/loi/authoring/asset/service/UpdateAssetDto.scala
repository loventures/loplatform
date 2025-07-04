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

package loi.authoring.asset.service

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.jackson.ValidationException
import loi.authoring.AssetType
import loi.authoring.asset.exception.{DeserializationException, DeserializationValidationException}

import scala.util.Try

/** Data used to update an asset.
  */
case class UpdateAssetDto[A](
  userId: Long,
  assetType: AssetType[A],
  data: A
) extends AssetDto

object UpdateAssetDto:

  /** Factory method for [[UpdateAssetDto]]. This is a variant of the primary constructor for use when the client knows
    * the asset implementation type at compile time, such that the factory for `A` can be summoned implicitly.
    *
    * @param userId
    *   the user updating the asset
    * @param data
    *   the new data to update the asset with
    * @tparam A
    *   the asset implementation type being updated
    * @return
    *   an update dto
    */
  def apply[A](userId: Long, data: A)(implicit assetType: AssetType[A]): UpdateAssetDto[A] =
    UpdateAssetDto(userId, assetType, data)

  def fromJson[A](
    userId: Long,
    node: JsonNode
  )(implicit assetType: AssetType[A]): Try[UpdateAssetDto[A]] =
    Try(JacksonUtils.getFinatraMapper.treeToValue(node, assetType.dataClass))
      .map(UpdateAssetDto(userId, assetType, _))
      .recover({
        case ex: ValidationException     => throw DeserializationValidationException(assetType.id, cause = ex)
        case ex: JsonProcessingException => throw DeserializationException(assetType.id, cause = ex)
      })
end UpdateAssetDto
