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

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.databind.util.Converter
import loi.authoring.asset.Asset

/** Converts an asset to its JSON representation. This is intended for serialization use only. Assets are not
  * deserialized (asset data is, but not asset).
  */
class AssetJsonConverter extends Converter[Asset[?], AssetJson]:

  override def convert(asset: Asset[?]): AssetJson =

    val info = asset.info

    AssetJson(
      id = info.id,
      name = info.name,
      typeId = info.typeId.entryName,
      created = info.created,
      createdBy = info.createdBy,
      modified = info.modified,
      data = asset.data
    )
  end convert

  override def getOutputType(typeFactory: TypeFactory): JavaType =
    typeFactory
      .constructType(classOf[AssetJson])

  override def getInputType(typeFactory: TypeFactory): JavaType =
    typeFactory
      .constructType(classOf[Asset[?]])
end AssetJsonConverter
