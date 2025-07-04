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

package loi.authoring.asset.exception

import com.fasterxml.jackson.core.JsonProcessingException
import com.learningobjects.cpxp.jackson.ValidationException
import com.learningobjects.de.web.UncheckedMessageException
import loi.authoring.asset.factory.AssetTypeId
import loi.cp.i18n.AuthoringBundle

case class DeserializationException(
  typeId: AssetTypeId,
  id: Option[Long] = None,
  cause: JsonProcessingException
) extends UncheckedMessageException(
      AuthoringBundle.message("asset.deserializationFailure", typeId.entryName, id, cause.getOriginalMessage),
      cause
    )

case class DeserializationValidationException(
  typeId: AssetTypeId,
  id: Option[Long] = None,
  cause: ValidationException
) extends UncheckedMessageException(
      AuthoringBundle
        .message("asset.deserializationFailure", typeId.entryName, id, cause.violations.list.toList.mkString(", ")),
      cause
    )

case class NoSuchAssetType(typeId: String)
    extends UncheckedMessageException(AuthoringBundle.message("assetType.noSuchAssetType", typeId))

case class UnsupportedAssetType(typeId: AssetTypeId)
    extends UncheckedMessageException(AuthoringBundle.message("assetType.unsupportedAssetType", typeId))
